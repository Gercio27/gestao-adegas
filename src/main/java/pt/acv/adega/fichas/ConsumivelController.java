package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.movimentos.MovimentoStockService;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/consumiveis")
public class ConsumivelController {

    private final ConsumivelRepository repo;
    private final CodigoService codigoService;
    private final MovimentoStockService movimentos;

    public ConsumivelController(ConsumivelRepository repo, CodigoService codigoService,
                                MovimentoStockService movimentos) {
        this.repo = repo;
        this.codigoService = codigoService;
        this.movimentos = movimentos;
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
        // Quanto e' que o stock mexeu com esta gravacao (reposicao ou correcao).
        int antes = c.getId() == null ? 0
                : repo.findById(c.getId()).map(Consumivel::getStock).orElse(0);
        if (c.getId() == null) {
            c.setCodigo(codigoService.proximoCodigo(c.getTipo().getPrefixo()));
        }
        repo.save(c);
        int delta = c.getStock() - antes;
        if (delta != 0) {
            movimentos.consumivel(c, delta, "Ficha de consumíveis",
                    antes == 0 ? "Stock inicial registado na ficha"
                            : (delta > 0 ? "Reposição de stock na ficha" : "Correção de stock na ficha"));
        }
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
