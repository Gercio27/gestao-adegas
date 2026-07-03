package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/adegas")
public class AdegaController {

    private final AdegaRepository repo;
    private final CodigoService codigoService;

    public AdegaController(AdegaRepository repo, CodigoService codigoService) {
        this.repo = repo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("adegas", repo.findAllByOrderByNomeAsc());
        return "fichas/adegas/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("adega", new Adega());
        return "fichas/adegas/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Adega a = repo.findById(id).orElse(null);
        if (a == null) {
            ra.addFlashAttribute("erro", "Adega nao encontrada.");
            return "redirect:/fichas/adegas";
        }
        model.addAttribute("adega", a);
        return "fichas/adegas/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("adega") Adega a, BindingResult result,
                          RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "fichas/adegas/form";
        }
        if (a.getId() == null) {
            a.setCodigo(codigoService.proximoCodigo(Adega.PREFIXO));
        }
        repo.save(a);
        ra.addFlashAttribute("sucesso", "Adega guardada: " + a.getCodigo());
        return "redirect:/fichas/adegas";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Adega eliminada.");
        return "redirect:/fichas/adegas";
    }
}
