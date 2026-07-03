package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/trabalhadores")
public class TrabalhadorController {

    private final TrabalhadorRepository repo;
    private final CodigoService codigoService;

    public TrabalhadorController(TrabalhadorRepository repo, CodigoService codigoService) {
        this.repo = repo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("trabalhadores", repo.findAllByOrderByNomeAsc());
        return "fichas/trabalhadores/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("trabalhador", new Trabalhador());
        return "fichas/trabalhadores/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Trabalhador t = repo.findById(id).orElse(null);
        if (t == null) {
            ra.addFlashAttribute("erro", "Trabalhador nao encontrado.");
            return "redirect:/fichas/trabalhadores";
        }
        model.addAttribute("trabalhador", t);
        return "fichas/trabalhadores/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("trabalhador") Trabalhador t, BindingResult result,
                          RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "fichas/trabalhadores/form";
        }
        if (t.getId() == null) {
            t.setCodigo(codigoService.proximoCodigo(Trabalhador.PREFIXO));
        }
        repo.save(t);
        ra.addFlashAttribute("sucesso", "Trabalhador guardado: " + t.getCodigo());
        return "redirect:/fichas/trabalhadores";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Trabalhador eliminado.");
        return "redirect:/fichas/trabalhadores";
    }
}
