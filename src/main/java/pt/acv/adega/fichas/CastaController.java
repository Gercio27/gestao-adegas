package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/castas")
public class CastaController {

    private final CastaRepository repo;
    private final CodigoService codigoService;

    public CastaController(CastaRepository repo, CodigoService codigoService) {
        this.repo = repo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("castas", repo.findAllByOrderByNomeAsc());
        return "fichas/castas/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("casta", new Casta());
        model.addAttribute("cores", CorCasta.values());
        return "fichas/castas/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Casta casta = repo.findById(id).orElse(null);
        if (casta == null) {
            ra.addFlashAttribute("erro", "Casta nao encontrada.");
            return "redirect:/fichas/castas";
        }
        model.addAttribute("casta", casta);
        model.addAttribute("cores", CorCasta.values());
        return "fichas/castas/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("casta") Casta casta, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cores", CorCasta.values());
            return "fichas/castas/form";
        }
        if (casta.getId() == null) {
            casta.setCodigo(codigoService.proximoCodigo(Casta.PREFIXO));
        }
        repo.save(casta);
        ra.addFlashAttribute("sucesso", "Casta guardada: " + casta.getCodigo());
        return "redirect:/fichas/castas";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Casta eliminada.");
        return "redirect:/fichas/castas";
    }
}
