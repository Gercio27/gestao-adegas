package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/armazens")
public class ArmazemController {

    private final ArmazemRepository repo;
    private final CodigoService codigoService;

    public ArmazemController(ArmazemRepository repo, CodigoService codigoService) {
        this.repo = repo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("armazens", repo.findAllByOrderByNomeAsc());
        return "fichas/armazens/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("armazem", new Armazem());
        return "fichas/armazens/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Armazem a = repo.findById(id).orElse(null);
        if (a == null) { ra.addFlashAttribute("erro", "Armazem nao encontrado."); return "redirect:/fichas/armazens"; }
        model.addAttribute("armazem", a);
        return "fichas/armazens/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("armazem") Armazem a, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "fichas/armazens/form";
        }
        if (a.getId() == null) {
            a.setCodigo(codigoService.proximoCodigo(Armazem.PREFIXO));
        }
        repo.save(a);
        ra.addFlashAttribute("sucesso", "Armazem guardado: " + a.getCodigo());
        return "redirect:/fichas/armazens";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Armazem eliminado.");
        return "redirect:/fichas/armazens";
    }
}
