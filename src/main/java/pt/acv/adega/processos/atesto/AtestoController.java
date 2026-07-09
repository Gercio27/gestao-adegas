package pt.acv.adega.processos.atesto;

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
@RequestMapping("/processos/atesto")
public class AtestoController {

    private final ProcessoAtestoRepository repo;
    private final AtestoService atestoService;
    private final RecipienteService recipienteService;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final MostoRepository mostoRepo;
    private final ProcessoMoagemRepository moagemRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public AtestoController(ProcessoAtestoRepository repo, AtestoService atestoService,
                            RecipienteService recipienteService, TalhaRepository talhaRepo,
                            DepositoRepository depositoRepo, MostoRepository mostoRepo,
                            ProcessoMoagemRepository moagemRepo, TrabalhadorRepository trabalhadorRepo,
                            CodigoService codigoService) {
        this.repo = repo;
        this.atestoService = atestoService;
        this.recipienteService = recipienteService;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
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
        return "processos/atesto/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        ProcessoAtesto a = new ProcessoAtesto();
        a.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("atesto", a);
        preencherOpcoes(model);
        return "processos/atesto/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoAtesto a = repo.findById(id).orElse(null);
        if (a == null || !podeAceder(a, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/atesto"; }
        model.addAttribute("atesto", a);
        return "processos/atesto/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoAtesto a = repo.findById(id).orElse(null);
        if (a == null || !podeAceder(a, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/atesto"; }
        if (!a.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/atesto/" + id; }
        model.addAttribute("atesto", a);
        preencherOpcoes(model);
        return "processos/atesto/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("atesto") ProcessoAtesto atesto, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        resolverRecipientes(atesto);
        // Adega do atesto = adega do recipiente de origem.
        if (atesto.getTalhaOrigem() != null) atesto.setAdega(atesto.getTalhaOrigem().getAdega());
        else if (atesto.getDepositoOrigem() != null) atesto.setAdega(atesto.getDepositoOrigem().getAdega());
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/atesto/form";
        }
        if (atesto.getId() == null) {
            atesto.setCodigo(codigoService.proximoCodigo(ProcessoAtesto.PREFIXO));
            atesto.setCriadoPor(auth.getName());
        } else {
            ProcessoAtesto existente = repo.findById(atesto.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/atesto";
            }
            atesto.setCriadoPor(existente.getCriadoPor());
            atesto.setEstado(existente.getEstado());
            atesto.setDataFecho(existente.getDataFecho());
        }
        repo.save(atesto);
        ra.addFlashAttribute("sucesso", "Atesto guardado: " + atesto.getCodigo());
        return "redirect:/processos/atesto/" + atesto.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoAtesto a = repo.findById(id).orElse(null);
        if (a == null || !podeAceder(a, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/atesto"; }
        try {
            atestoService.fechar(id);
            ra.addFlashAttribute("sucesso", "Atesto fechado. Volumes atualizados nos dois recipientes.");
        } catch (AtestoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/atesto/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/atesto/" + id; }
        try {
            atestoService.reabrir(id);
            ra.addFlashAttribute("sucesso", "Atesto reaberto. Volumes repostos.");
        } catch (AtestoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/atesto/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoAtesto a = repo.findById(id).orElse(null);
        if (a == null || !podeAceder(a, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/atesto"; }
        if (!a.isAberto()) { ra.addFlashAttribute("erro", "Reabra o processo antes de o eliminar (para repor os volumes)."); return "redirect:/processos/atesto/" + id; }
        repo.delete(a);
        ra.addFlashAttribute("sucesso", "Atesto eliminado.");
        return "redirect:/processos/atesto";
    }

    // ----- auxiliares -----

    private void resolverRecipientes(ProcessoAtesto a) {
        RecipienteService.Recipiente o = recipienteService.resolver(a.getOrigemRef());
        a.setTalhaOrigem(o.talha());
        a.setDepositoOrigem(o.deposito());
        RecipienteService.Recipiente d = recipienteService.resolver(a.getDestinoRef());
        a.setTalhaDestino(d.talha());
        a.setDepositoDestino(d.deposito());
    }

    private void preencherOpcoes(Model model) {
        // Vinhos que vieram da moagem (com mosto em fermentação) e as suas origens.
        Map<Long, PlaneamentoVinho> moagemPlano = new HashMap<>();
        for (ProcessoMoagem mo : moagemRepo.findAll()) {
            if (mo.getPlano() != null) moagemPlano.put(mo.getId(), mo.getPlano());
        }
        List<Mosto> fermentando = new ArrayList<>();
        fermentando.addAll(mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.EM_FERMENTACAO));
        fermentando.addAll(mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.ATESTADO));

        Map<Long, String> vinhoNome = new LinkedHashMap<>();
        Map<Long, LinkedHashMap<String, Origem>> origAgg = new LinkedHashMap<>();
        for (Mosto m : fermentando) {
            PlaneamentoVinho w = m.getOrigemMoagemId() != null ? moagemPlano.get(m.getOrigemMoagemId()) : null;
            if (w == null) continue;
            String ref, ident;
            Long adegaId;
            if (m.getTalha() != null && m.getTalha().getAdega() != null) {
                ref = "TALHA:" + m.getTalha().getId();
                ident = "Talha " + m.getTalha().getIdentificacao();
                adegaId = m.getTalha().getAdega().getId();
            } else if (m.getDeposito() != null && m.getDeposito().getAdega() != null) {
                ref = "DEPOSITO:" + m.getDeposito().getId();
                ident = "Depósito " + m.getDeposito().getIdentificacao();
                adegaId = m.getDeposito().getAdega().getId();
            } else {
                continue;
            }
            vinhoNome.putIfAbsent(w.getId(), w.getNomeVinho());
            Origem o = origAgg.computeIfAbsent(w.getId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(ref, k -> new Origem(ident, adegaId));
            o.litros = o.litros.add(m.getLitros() == null ? BigDecimal.ZERO : m.getLitros());
        }

        List<Map<String, Object>> vinhos = new ArrayList<>();
        for (Map.Entry<Long, String> e : vinhoNome.entrySet()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", e.getKey());
            v.put("nome", e.getValue());
            vinhos.add(v);
        }
        Map<Long, List<Map<String, Object>>> origensPorVinho = new LinkedHashMap<>();
        for (Map.Entry<Long, LinkedHashMap<String, Origem>> e : origAgg.entrySet()) {
            List<Map<String, Object>> lst = new ArrayList<>();
            for (Map.Entry<String, Origem> re : e.getValue().entrySet()) {
                Origem o = re.getValue();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ref", re.getKey());
                m.put("adegaId", o.adegaId);
                m.put("litros", o.litros.toPlainString());
                m.put("label", o.ident + " · " + o.litros.toPlainString() + " L");
                lst.add(m);
            }
            origensPorVinho.put(e.getKey(), lst);
        }

        model.addAttribute("vinhos", vinhos);
        model.addAttribute("origensPorVinho", origensPorVinho);
        model.addAttribute("destinosPorAdega", destinosPorAdega());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    /** Recipientes (talhas + depósitos) de cada adega, para o destino do atesto. */
    private Map<Long, List<Map<String, Object>>> destinosPorAdega() {
        Map<Long, List<Map<String, Object>>> out = new LinkedHashMap<>();
        talhaRepo.findAllByOrderByIdentificacaoAsc().forEach(t -> {
            if (t.getAdega() == null) return;
            out.computeIfAbsent(t.getAdega().getId(), k -> new ArrayList<>())
                    .add(recipiente("TALHA:" + t.getId(), "Talha " + t.getIdentificacao(),
                            t.getCapacidadeLitros(), t.getVolumeAtualLitros()));
        });
        depositoRepo.findAllByOrderByIdentificacaoAsc().forEach(d -> {
            if (d.getAdega() == null) return;
            out.computeIfAbsent(d.getAdega().getId(), k -> new ArrayList<>())
                    .add(recipiente("DEPOSITO:" + d.getId(), "Depósito " + d.getIdentificacao(),
                            d.getCapacidadeLitros(), d.getVolumeAtualLitros()));
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

    private static class Origem {
        final String ident;
        final Long adegaId;
        BigDecimal litros = BigDecimal.ZERO;
        Origem(String ident, Long adegaId) { this.ident = ident; this.adegaId = adegaId; }
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoAtesto a, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(a.getCriadoPor());
    }
}
