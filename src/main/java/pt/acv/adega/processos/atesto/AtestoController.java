package pt.acv.adega.processos.atesto;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.AdegaRepository;
import pt.acv.adega.fichas.RecipienteService;
import pt.acv.adega.fichas.TrabalhadorRepository;
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
    private final AdegaRepository adegaRepo;
    private final MostoRepository mostoRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public AtestoController(ProcessoAtestoRepository repo, AtestoService atestoService,
                            RecipienteService recipienteService, AdegaRepository adegaRepo,
                            MostoRepository mostoRepo, TrabalhadorRepository trabalhadorRepo,
                            CodigoService codigoService) {
        this.repo = repo;
        this.atestoService = atestoService;
        this.recipienteService = recipienteService;
        this.adegaRepo = adegaRepo;
        this.mostoRepo = mostoRepo;
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
        model.addAttribute("recipientes", recipienteService.opcoes());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("recipientesPorAdega", recipientesPorAdega());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    /** Mapa adega -> recipientes dessa adega com mosto em fermentação (ref + info). */
    private Map<Long, List<Map<String, Object>>> recipientesPorAdega() {
        List<Mosto> fermentando = new ArrayList<>();
        fermentando.addAll(mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.EM_FERMENTACAO));
        fermentando.addAll(mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.ATESTADO));

        Map<Long, LinkedHashMap<String, RecAgg>> agg = new LinkedHashMap<>();
        for (Mosto m : fermentando) {
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
            RecAgg a = agg.computeIfAbsent(adegaId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(ref, k -> new RecAgg(ident));
            a.litros = a.litros.add(m.getLitros() == null ? BigDecimal.ZERO : m.getLitros());
            if (m.getCasta() != null) a.castas.add(m.getCasta().getNome());
        }

        Map<Long, List<Map<String, Object>>> out = new LinkedHashMap<>();
        for (Map.Entry<Long, LinkedHashMap<String, RecAgg>> e : agg.entrySet()) {
            List<Map<String, Object>> linhas = new ArrayList<>();
            for (Map.Entry<String, RecAgg> re : e.getValue().entrySet()) {
                RecAgg a = re.getValue();
                String castasTxt = a.castas.isEmpty() ? "" : " (" + String.join(", ", a.castas) + ")";
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ref", re.getKey());
                m.put("ident", a.ident);
                m.put("litros", a.litros.toPlainString());
                m.put("castas", String.join(", ", a.castas));
                m.put("label", a.ident + " · " + a.litros.toPlainString() + " L" + castasTxt);
                linhas.add(m);
            }
            out.put(e.getKey(), linhas);
        }
        return out;
    }

    private static class RecAgg {
        final String ident;
        BigDecimal litros = BigDecimal.ZERO;
        final LinkedHashSet<String> castas = new LinkedHashSet<>();
        RecAgg(String ident) { this.ident = ident; }
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoAtesto a, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(a.getCriadoPor());
    }
}
