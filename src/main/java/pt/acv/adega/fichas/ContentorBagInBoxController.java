package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

/**
 * Ficha dos contentores / paletes de bag-in-box. Espelha a ficha dos
 * contentores de garrafas, mas conta unidades de bag-in-box em vez de garrafas.
 */
@Controller
@RequestMapping("/fichas/bag-in-box")
public class ContentorBagInBoxController {

    private final ContentorBagInBoxRepository repo;
    private final ArmazemRepository armazemRepo;
    private final AdegaRepository adegaRepo;
    private final CodigoService codigoService;

    public ContentorBagInBoxController(ContentorBagInBoxRepository repo, ArmazemRepository armazemRepo,
                                       AdegaRepository adegaRepo, CodigoService codigoService) {
        this.repo = repo;
        this.armazemRepo = armazemRepo;
        this.adegaRepo = adegaRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("contentores", repo.findAllByOrderByNomeAsc());
        return "fichas/baginbox/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("contentor", new ContentorBagInBox());
        preencherOpcoes(model);
        return "fichas/baginbox/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        ContentorBagInBox c = repo.findById(id).orElse(null);
        if (c == null) { ra.addFlashAttribute("erro", "Contentor bag-in-box nao encontrado."); return "redirect:/fichas/bag-in-box"; }
        model.addAttribute("contentor", c);
        preencherOpcoes(model);
        return "fichas/baginbox/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("contentor") ContentorBagInBox c, BindingResult result,
                          @RequestParam(value = "localTipo", required = false) String localTipo,
                          Model model, RedirectAttributes ra) {
        // Fica num armazém OU numa adega — limpa o lado não escolhido.
        if ("ADEGA".equals(localTipo)) c.setArmazem(null); else c.setAdega(null);
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "fichas/baginbox/form";
        }
        if (c.getId() == null) {
            c.setCodigo(codigoService.proximoCodigo(ContentorBagInBox.PREFIXO));
        }
        if (c.getUnidadesAtuais() < 0) c.setUnidadesAtuais(0);
        if (c.getCapacidadeUnidades() < 0) c.setCapacidadeUnidades(0);
        // Contentor vazio não fica com vinho pendurado.
        if (c.getUnidadesAtuais() == 0) { c.setVinhoNome(null); c.setVinhoEmbaladoId(null); c.setRotulado(false); }
        repo.save(c);
        ra.addFlashAttribute("sucesso", "Contentor bag-in-box guardado: " + c.getCodigo());
        return "redirect:/fichas/bag-in-box";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        ContentorBagInBox c = repo.findById(id).orElse(null);
        if (c == null) { ra.addFlashAttribute("erro", "Contentor bag-in-box nao encontrado."); return "redirect:/fichas/bag-in-box"; }
        if (c.getUnidadesAtuais() > 0) {
            ra.addFlashAttribute("erro", "Não pode eliminar: ainda tem " + c.getUnidadesAtuais() + " unidade(s) lá dentro.");
            return "redirect:/fichas/bag-in-box";
        }
        repo.delete(c);
        ra.addFlashAttribute("sucesso", "Contentor bag-in-box eliminado.");
        return "redirect:/fichas/bag-in-box";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("tiposBag", TipoBagInBox.values());
        model.addAttribute("armazens", armazemRepo.findAllByOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
    }
}
