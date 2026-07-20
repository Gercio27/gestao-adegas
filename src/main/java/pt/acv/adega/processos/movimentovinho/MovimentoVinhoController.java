package pt.acv.adega.processos.movimentovinho;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.*;
import pt.acv.adega.planeamento.PlaneamentoVinho;
import pt.acv.adega.processos.moagem.ProcessoMoagem;
import pt.acv.adega.processos.moagem.ProcessoMoagemRepository;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/processos/movimento-vinho")
public class MovimentoVinhoController {

    private final ProcessoMovimentoVinhoRepository repo;
    private final MovimentoVinhoService service;
    private final RecipienteService recipienteService;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final AdegaRepository adegaRepo;
    private final MostoRepository mostoRepo;
    private final ProcessoMoagemRepository moagemRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CastaRepository castaRepo;
    private final CodigoService codigoService;

    public MovimentoVinhoController(ProcessoMovimentoVinhoRepository repo, MovimentoVinhoService service,
                                    RecipienteService recipienteService, TalhaRepository talhaRepo,
                                    DepositoRepository depositoRepo, AdegaRepository adegaRepo, MostoRepository mostoRepo,
                                    ProcessoMoagemRepository moagemRepo, TrabalhadorRepository trabalhadorRepo,
                                    CastaRepository castaRepo, CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.recipienteService = recipienteService;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.adegaRepo = adegaRepo;
        this.mostoRepo = mostoRepo;
        this.moagemRepo = moagemRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.castaRepo = castaRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/movimentovinho/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        ProcessoMovimentoVinho p = new ProcessoMovimentoVinho();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("mov", p);
        preencherOpcoes(model);
        return "processos/movimentovinho/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoMovimentoVinho p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-vinho"; }
        model.addAttribute("mov", p);
        return "processos/movimentovinho/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoMovimentoVinho p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-vinho"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/movimento-vinho/" + id; }
        model.addAttribute("mov", p);
        preencherOpcoes(model);
        return "processos/movimentovinho/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("mov") ProcessoMovimentoVinho mov, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (mov.getTipo() == TipoMovimentoVinho.ENTRADA) {
            RecipienteService.Recipiente r = recipienteService.resolver(mov.getDestinoRef());
            mov.setTalhaDestino(r.talha());
            mov.setDepositoDestino(r.deposito());
            mov.setMostoOrigem(null);
        } else if (mov.getTipo() == TipoMovimentoVinho.SAIDA) {
            mov.setTalhaDestino(null);
            mov.setDepositoDestino(null);
            mov.setCasta(null);
        } else { // TRANSFEGA
            RecipienteService.Recipiente r = recipienteService.resolver(mov.getDestinoRef());
            mov.setTalhaDestino(r.talha());
            mov.setDepositoDestino(r.deposito());
            mov.setCasta(null);
        }
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/movimentovinho/form";
        }
        if (mov.getId() == null) {
            mov.setCodigo(codigoService.proximoCodigo(ProcessoMovimentoVinho.PREFIXO));
            mov.setCriadoPor(auth.getName());
        } else {
            ProcessoMovimentoVinho existente = repo.findById(mov.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/movimento-vinho";
            }
            mov.setCriadoPor(existente.getCriadoPor());
            mov.setEstado(existente.getEstado());
            mov.setDataFecho(existente.getDataFecho());
            mov.setNumeroDA(existente.getNumeroDA());
            mov.setMostoDestinoId(existente.getMostoDestinoId());
            mov.setDestinoCriado(existente.isDestinoCriado());
        }
        repo.save(mov);
        ra.addFlashAttribute("sucesso", "Movimento guardado: " + mov.getCodigo());
        return "redirect:/processos/movimento-vinho/" + mov.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMovimentoVinho p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-vinho"; }
        try {
            service.fechar(id);
            ra.addFlashAttribute("sucesso", "Movimento fechado.");
        } catch (MovimentoVinhoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/movimento-vinho/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/movimento-vinho/" + id; }
        try {
            service.reabrir(id);
            ra.addFlashAttribute("sucesso", "Movimento reaberto. Efeitos revertidos.");
        } catch (MovimentoVinhoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/movimento-vinho/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMovimentoVinho p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-vinho"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra antes de eliminar (para reverter os efeitos)."); return "redirect:/processos/movimento-vinho/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Movimento eliminado.");
        return "redirect:/processos/movimento-vinho";
    }

    // ----- auxiliares -----

    private void preencherOpcoes(Model model) {
        model.addAttribute("tipos", TipoMovimentoVinho.values());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());

        Map<Long, PlaneamentoVinho> moagemPlano = new HashMap<>();
        for (ProcessoMoagem mo : moagemRepo.findAll()) {
            if (mo.getPlano() != null) moagemPlano.put(mo.getId(), mo.getPlano());
        }

        // Vinhos a granel agrupados pelo NOME do vinho (denormalizado ou do plano).
        Set<String> nomes = new LinkedHashSet<>();
        Map<String, List<Map<String, Object>>> mostosPorVinho = new LinkedHashMap<>();
        for (Mosto m : mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL)) {
            Long adegaId = null; String local = "—";
            if (m.getTalha() != null && m.getTalha().getAdega() != null) { adegaId = m.getTalha().getAdega().getId(); local = "Talha " + m.getTalha().getIdentificacao(); }
            else if (m.getDeposito() != null && m.getDeposito().getAdega() != null) { adegaId = m.getDeposito().getAdega().getId(); local = "Depósito " + m.getDeposito().getIdentificacao(); }
            String nome = nomeVinho(m, moagemPlano);
            if (adegaId == null || nome == null) continue;
            nomes.add(nome);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("adegaId", adegaId);
            row.put("label", m.getCodigo() + " · " + local + " · " + (m.getLitros() == null ? "0" : m.getLitros().toPlainString()) + " L");
            mostosPorVinho.computeIfAbsent(nome, k -> new ArrayList<>()).add(row);
        }
        model.addAttribute("vinhos", new ArrayList<>(nomes));
        model.addAttribute("mostosPorVinho", mostosPorVinho);
        model.addAttribute("recipientesPorAdega", recipientesPorAdega());

        // Nomes de vinho já conhecidos no sistema (todos os processos), para o
        // campo de ENTRADA os puxar em vez de serem escritos do zero.
        TreeSet<String> conhecidos = new TreeSet<>(nomes);
        for (Mosto m : mostoRepo.findAll()) {
            if (m.getVinhoNome() != null && !m.getVinhoNome().isBlank()) conhecidos.add(m.getVinhoNome());
        }
        for (PlaneamentoVinho w : moagemPlano.values()) {
            if (w.getNomeVinho() != null && !w.getNomeVinho().isBlank()) conhecidos.add(w.getNomeVinho());
        }
        model.addAttribute("nomesConhecidos", new ArrayList<>(conhecidos));
    }

    /** Nome do vinho: denormalizado no mosto ou, em falta, do planeamento de origem. */
    private String nomeVinho(Mosto m, Map<Long, PlaneamentoVinho> moagemPlano) {
        if (m.getVinhoNome() != null && !m.getVinhoNome().isBlank()) return m.getVinhoNome();
        PlaneamentoVinho w = m.getOrigemMoagemId() != null ? moagemPlano.get(m.getOrigemMoagemId()) : null;
        return w != null ? w.getNomeVinho() : null;
    }

    private Map<Long, List<Map<String, Object>>> recipientesPorAdega() {
        Map<Long, List<Map<String, Object>>> out = new LinkedHashMap<>();
        talhaRepo.findAllByOrderByIdentificacaoAsc().forEach(t -> {
            if (t.getAdega() == null) return;
            out.computeIfAbsent(t.getAdega().getId(), k -> new ArrayList<>())
                    .add(recipiente("TALHA:" + t.getId(), "Talha " + t.getIdentificacao(), t.getCapacidadeLitros(), t.getVolumeAtualLitros()));
        });
        depositoRepo.findAllByOrderByIdentificacaoAsc().forEach(d -> {
            if (d.getAdega() == null) return;
            out.computeIfAbsent(d.getAdega().getId(), k -> new ArrayList<>())
                    .add(recipiente("DEPOSITO:" + d.getId(), "Depósito " + d.getIdentificacao(), d.getCapacidadeLitros(), d.getVolumeAtualLitros()));
        });
        return out;
    }

    private Map<String, Object> recipiente(String ref, String ident, BigDecimal cap, BigDecimal vol) {
        BigDecimal vv = vol == null ? BigDecimal.ZERO : vol;
        boolean cheia = cap != null && vv.compareTo(cap) >= 0;
        String capTxt = cap == null ? " (sem cap.)" : " (" + vv.toPlainString() + "/" + cap.toPlainString() + " L)";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ref", ref);
        m.put("label", ident + capTxt);
        m.put("cheia", cheia);
        return m;
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoMovimentoVinho p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
