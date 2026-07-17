package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/contentores")
public class ContentorGarrafasController {

    private final ContentorGarrafasRepository repo;
    private final ArmazemRepository armazemRepo;
    private final AdegaRepository adegaRepo;
    private final CodigoService codigoService;

    public ContentorGarrafasController(ContentorGarrafasRepository repo, ArmazemRepository armazemRepo,
                                       AdegaRepository adegaRepo, CodigoService codigoService) {
        this.repo = repo;
        this.armazemRepo = armazemRepo;
        this.adegaRepo = adegaRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("contentores", repo.findAllByOrderByNomeAsc());
        return "fichas/contentores/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("contentor", new ContentorGarrafas());
        preencherOpcoes(model);
        return "fichas/contentores/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        ContentorGarrafas c = repo.findById(id).orElse(null);
        if (c == null) { ra.addFlashAttribute("erro", "Contentor nao encontrado."); return "redirect:/fichas/contentores"; }
        model.addAttribute("contentor", c);
        preencherOpcoes(model);
        return "fichas/contentores/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("contentor") ContentorGarrafas c, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "fichas/contentores/form";
        }
        if (c.getId() == null) {
            c.setCodigo(codigoService.proximoCodigo(ContentorGarrafas.PREFIXO));
        }
        repo.save(c);
        ra.addFlashAttribute("sucesso", "Contentor guardado: " + c.getCodigo());
        return "redirect:/fichas/contentores";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Contentor eliminado.");
        return "redirect:/fichas/contentores";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("tiposGarrafa", TipoGarrafa.values());
        model.addAttribute("armazens", armazemRepo.findAllByOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
    }
}
