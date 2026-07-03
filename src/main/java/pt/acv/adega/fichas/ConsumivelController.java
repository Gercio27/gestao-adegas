package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/consumiveis")
public class ConsumivelController {

    private final ConsumivelRepository repo;
    private final CodigoService codigoService;

    public ConsumivelController(ConsumivelRepository repo, CodigoService codigoService) {
        this.repo = repo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("consumiveis", repo.findAllByOrderByTipoAscDescricaoAsc());
        return "fichas/consumiveis/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("consumivel", new Consumivel());
        preencherOpcoes(model);
        return "fichas/consumiveis/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Consumivel c = repo.findById(id).orElse(null);
        if (c == null) { ra.addFlashAttribute("erro", "Consumível não encontrado."); return "redirect:/fichas/consumiveis"; }
        model.addAttribute("consumivel", c);
        preencherOpcoes(model);
        return "fichas/consumiveis/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("consumivel") Consumivel c, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "fichas/consumiveis/form";
        }
        if (c.getId() == null) {
            c.setCodigo(codigoService.proximoCodigo(c.getTipo().getPrefixo()));
        }
        repo.save(c);
        ra.addFlashAttribute("sucesso", "Consumível guardado: " + c.getCodigo());
        return "redirect:/fichas/consumiveis";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Consumível eliminado.");
        return "redirect:/fichas/consumiveis";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("tipos", TipoConsumivel.values());
        model.addAttribute("propriedades", Propriedade.values());
    }
}
