package pt.acv.adega.processos.vindima;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.*;
import pt.acv.adega.processos.EstadoProcesso;

import java.time.LocalDateTime;

/**
 * Processo de Vindima (Fase 2). Abre-se, preenche-se e fecha-se.
 * Acesso: o utilizador que abriu ve os seus; o administrador ve todos.
 */
@Controller
@RequestMapping("/processos/vindima")
public class VindimaController {

    private final ProcessoVindimaRepository repo;
    private final VinhaRepository vinhaRepo;
    private final CastaRepository castaRepo;
    private final AdegaRepository adegaRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public VindimaController(ProcessoVindimaRepository repo, VinhaRepository vinhaRepo,
                             CastaRepository castaRepo, AdegaRepository adegaRepo,
                             TrabalhadorRepository trabalhadorRepo, CodigoService codigoService) {
        this.repo = repo;
        this.vinhaRepo = vinhaRepo;
        this.castaRepo = castaRepo;
        this.adegaRepo = adegaRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/vindima/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoVindima p = new ProcessoVindima();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("vindima", p);
        preencherOpcoes(model);
        return "processos/vindima/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoVindima p = repo.findById(id).orElse(null);
        if (p == null) { ra.addFlashAttribute("erro", "Vindima não encontrada."); return "redirect:/processos/vindima"; }
        if (!podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/vindima"; }
        model.addAttribute("vindima", p);
        return "processos/vindima/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoVindima p = repo.findById(id).orElse(null);
        if (p == null) { ra.addFlashAttribute("erro", "Vindima não encontrada."); return "redirect:/processos/vindima"; }
        if (!podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/vindima"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/vindima/" + id; }
        model.addAttribute("vindima", p);
        preencherOpcoes(model);
        return "processos/vindima/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("vindima") ProcessoVindima vindima, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/vindima/form";
        }
        if (vindima.getId() == null) {
            vindima.setCodigo(codigoService.proximoCodigo(ProcessoVindima.PREFIXO));
            vindima.setCriadoPor(auth.getName());
        } else {
            // Preserva autor/estado do registo existente e valida acesso.
            ProcessoVindima existente = repo.findById(vindima.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/vindima";
            }
            vindima.setCriadoPor(existente.getCriadoPor());
            vindima.setEstado(existente.getEstado());
            vindima.setDataFecho(existente.getDataFecho());
        }
        repo.save(vindima);
        ra.addFlashAttribute("sucesso", "Vindima guardada: " + vindima.getCodigo());
        return "redirect:/processos/vindima/" + vindima.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoVindima p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/vindima"; }
        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
        ra.addFlashAttribute("sucesso", "Vindima fechada: " + p.getCodigo());
        return "redirect:/processos/vindima/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoVindima p = repo.findById(id).orElse(null);
        if (p == null || !isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/vindima"; }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
        ra.addFlashAttribute("sucesso", "Vindima reaberta: " + p.getCodigo());
        return "redirect:/processos/vindima/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoVindima p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/vindima"; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Vindima eliminada.");
        return "redirect:/processos/vindima";
    }

    // ----- auxiliares -----

    private void preencherOpcoes(Model model) {
        model.addAttribute("vinhas", vinhaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("origens", OrigemUva.values());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoVindima p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
