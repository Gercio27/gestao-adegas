package pt.acv.adega.processos.movimento;

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
@RequestMapping("/processos/movimento-mosto")
public class MovimentoController {

    private final ProcessoMovimentoMostoRepository repo;
    private final MovimentoService service;
    private final RecipienteService recipienteService;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final AdegaRepository adegaRepo;
    private final MostoRepository mostoRepo;
    private final ProcessoMoagemRepository moagemRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public MovimentoController(ProcessoMovimentoMostoRepository repo, MovimentoService service,
                              RecipienteService recipienteService, TalhaRepository talhaRepo,
                              DepositoRepository depositoRepo, AdegaRepository adegaRepo, MostoRepository mostoRepo,
                              ProcessoMoagemRepository moagemRepo, TrabalhadorRepository trabalhadorRepo,
                              CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.recipienteService = recipienteService;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.adegaRepo = adegaRepo;
        this.mostoRepo = mostoRepo;
        this.moagemRepo = moagemRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/movimento/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        ProcessoMovimentoMosto p = new ProcessoMovimentoMosto();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("mov", p);
        preencherOpcoes(model);
        return "processos/movimento/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoMovimentoMosto p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-mosto"; }
        model.addAttribute("mov", p);
        return "processos/movimento/detalhe";
    }

    @GetMapping("/{id}/da")
    public String da(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoMovimentoMosto p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-mosto"; }
        if (p.getNumeroDA() == null) { ra.addFlashAttribute("erro", "O DA só é emitido ao fechar o processo."); return "redirect:/processos/movimento-mosto/" + id; }
        model.addAttribute("mov", p);
        return "processos/movimento/da";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoMovimentoMosto p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-mosto"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/movimento-mosto/" + id; }
        model.addAttribute("mov", p);
        preencherOpcoes(model);
        return "processos/movimento/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("mov") ProcessoMovimentoMosto mov, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (mov.getTipo() == TipoMovimento.ENTRADA) {
            RecipienteService.Recipiente r = recipienteService.resolver(mov.getDestinoRef());
            mov.setTalhaDestino(r.talha());
            mov.setDepositoDestino(r.deposito());
            mov.setMostoOrigem(null);
        } else {
            mov.setTalhaDestino(null);
            mov.setDepositoDestino(null);
        }
        mov.setCasta(null); // a casta vem do mosto/vinho — não é pedida aqui
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/movimento/form";
        }
        if (mov.getId() == null) {
            mov.setCodigo(codigoService.proximoCodigo(ProcessoMovimentoMosto.PREFIXO));
            mov.setCriadoPor(auth.getName());
        } else {
            ProcessoMovimentoMosto existente = repo.findById(mov.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/movimento-mosto";
            }
            mov.setCriadoPor(existente.getCriadoPor());
            mov.setEstado(existente.getEstado());
            mov.setDataFecho(existente.getDataFecho());
            mov.setNumeroDA(existente.getNumeroDA());
        }
        repo.save(mov);
        ra.addFlashAttribute("sucesso", "Movimento guardado: " + mov.getCodigo());
        return "redirect:/processos/movimento-mosto/" + mov.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMovimentoMosto p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-mosto"; }
        try {
            service.fechar(id);
            ra.addFlashAttribute("sucesso", "Movimento fechado. DA emitido.");
        } catch (MovimentoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/movimento-mosto/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/movimento-mosto/" + id; }
        try {
            service.reabrir(id);
            ra.addFlashAttribute("sucesso", "Movimento reaberto. Efeitos revertidos.");
        } catch (MovimentoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/movimento-mosto/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMovimentoMosto p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-mosto"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra antes de eliminar (para reverter os efeitos)."); return "redirect:/processos/movimento-mosto/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Movimento eliminado.");
        return "redirect:/processos/movimento-mosto";
    }

    // ----- auxiliares -----

    private void preencherOpcoes(Model model) {
        model.addAttribute("tipos", TipoMovimento.values());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());

        // Vinhos (da moagem) + mostos em fermentação por vinho, e recipientes por adega.
        Map<Long, PlaneamentoVinho> moagemPlano = new HashMap<>();
        for (ProcessoMoagem mo : moagemRepo.findAll()) {
            if (mo.getPlano() != null) moagemPlano.put(mo.getId(), mo.getPlano());
        }
        List<Mosto> fermentando = new ArrayList<>();
        fermentando.addAll(mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.EM_FERMENTACAO));
        fermentando.addAll(mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.ATESTADO));

        Map<Long, String> vinhoNome = new LinkedHashMap<>();
        Map<Long, List<Map<String, Object>>> mostosPorVinho = new LinkedHashMap<>();
        for (Mosto m : fermentando) {
            PlaneamentoVinho w = m.getOrigemMoagemId() != null ? moagemPlano.get(m.getOrigemMoagemId()) : null;
            Long adegaId = null;
            String local = "—";
            if (m.getTalha() != null && m.getTalha().getAdega() != null) {
                adegaId = m.getTalha().getAdega().getId();
                local = "Talha " + m.getTalha().getIdentificacao();
            } else if (m.getDeposito() != null && m.getDeposito().getAdega() != null) {
                adegaId = m.getDeposito().getAdega().getId();
                local = "Depósito " + m.getDeposito().getIdentificacao();
            }
            if (w == null || adegaId == null) continue;
            vinhoNome.putIfAbsent(w.getId(), w.getNomeVinho());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("adegaId", adegaId);
            row.put("label", m.getCodigo() + " · " + local + " · " + (m.getLitros() == null ? "0" : m.getLitros().toPlainString()) + " L");
            mostosPorVinho.computeIfAbsent(w.getId(), k -> new ArrayList<>()).add(row);
        }

        List<Map<String, Object>> vinhos = new ArrayList<>();
        for (Map.Entry<Long, String> e : vinhoNome.entrySet()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", e.getKey());
            v.put("nome", e.getValue());
            vinhos.add(v);
        }
        model.addAttribute("vinhos", vinhos);
        model.addAttribute("mostosPorVinho", mostosPorVinho);
        model.addAttribute("recipientesPorAdega", recipientesPorAdega());
    }

    /** Recipientes (talhas + depósitos) por adega, para o destino da ENTRADA. */
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
        BigDecimal v = vol == null ? BigDecimal.ZERO : vol;
        boolean cheia = cap != null && v.compareTo(cap) >= 0;
        String capTxt = cap == null ? " (sem cap.)" : " (" + v.toPlainString() + "/" + cap.toPlainString() + " L)";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ref", ref);
        m.put("label", ident + capTxt);
        m.put("cheia", cheia);
        return m;
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoMovimentoMosto p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
