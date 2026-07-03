package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/talhas")
public class TalhaController {

    private final TalhaRepository repo;
    private final AdegaRepository adegaRepo;
    private final CodigoService codigoService;
    private final pt.acv.adega.produtos.MostoRepository mostoRepo;

    public TalhaController(TalhaRepository repo, AdegaRepository adegaRepo, CodigoService codigoService,
                           pt.acv.adega.produtos.MostoRepository mostoRepo) {
        this.repo = repo;
        this.adegaRepo = adegaRepo;
        this.codigoService = codigoService;
        this.mostoRepo = mostoRepo;
    }

    @GetMapping("/{id}/etiqueta")
    public String etiqueta(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Talha t = repo.findById(id).orElse(null);
        if (t == null) { ra.addFlashAttribute("erro", "Talha nao encontrada."); return "redirect:/fichas/talhas"; }
        model.addAttribute("tipo", "Talha");
        model.addAttribute("codigo", t.getCodigo());
        model.addAttribute("identificacao", t.getIdentificacao());
        model.addAttribute("adega", t.getAdega() != null ? t.getAdega().getNome() : null);
        model.addAttribute("capacidade", t.getCapacidadeLitros());
        model.addAttribute("volume", t.getVolumeAtualLitros());
        model.addAttribute("propriedade", t.getPropriedade().getDescricao());
        model.addAttribute("conteudos", mostoRepo.findByTalhaId(id));
        return "fichas/etiqueta";
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("talhas", repo.findAllByOrderByIdentificacaoAsc());
        return "fichas/talhas/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("talha", new Talha());
        preencherOpcoes(model);
        return "fichas/talhas/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Talha t = repo.findById(id).orElse(null);
        if (t == null) {
            ra.addFlashAttribute("erro", "Talha nao encontrada.");
            return "redirect:/fichas/talhas";
        }
        model.addAttribute("talha", t);
        preencherOpcoes(model);
        return "fichas/talhas/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("talha") Talha t, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "fichas/talhas/form";
        }
        if (t.getId() == null) {
            t.setCodigo(codigoService.proximoCodigo(Talha.PREFIXO));
        }
        repo.save(t);
        ra.addFlashAttribute("sucesso", "Talha guardada: " + t.getCodigo());
        return "redirect:/fichas/talhas";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Talha eliminada.");
        return "redirect:/fichas/talhas";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("propriedades", Propriedade.values());
    }
}
