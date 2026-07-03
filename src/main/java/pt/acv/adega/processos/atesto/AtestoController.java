package pt.acv.adega.processos.atesto;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.RecipienteService;
import pt.acv.adega.fichas.TrabalhadorRepository;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/processos/atesto")
public class AtestoController {

    private final ProcessoAtestoRepository repo;
    private final AtestoService atestoService;
    private final RecipienteService recipienteService;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public AtestoController(ProcessoAtestoRepository repo, AtestoService atestoService,
                            RecipienteService recipienteService, TrabalhadorRepository trabalhadorRepo,
                            CodigoService codigoService) {
        this.repo = repo;
        this.atestoService = atestoService;
        this.recipienteService = recipienteService;
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
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoAtesto a, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(a.getCriadoPor());
    }
}
