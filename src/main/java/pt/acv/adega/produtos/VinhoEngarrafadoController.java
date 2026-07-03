package pt.acv.adega.produtos;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Mapa de vinhos engarrafados (produtos acabados). Apenas consulta: gerado
 * pelo processo de Engarrafamento.
 */
@Controller
@RequestMapping("/produtos/engarrafados")
public class VinhoEngarrafadoController {

    private final VinhoEngarrafadoRepository repo;

    public VinhoEngarrafadoController(VinhoEngarrafadoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String listar(Model model) {
        var lista = repo.findAllByOrderByDataProducaoDesc();
        int totalGarrafas = lista.stream().mapToInt(VinhoEngarrafado::getNumeroGarrafas).sum();
        model.addAttribute("engarrafados", lista);
        model.addAttribute("totalGarrafas", totalGarrafas);
        return "produtos/engarrafados/lista";
    }

    /** Etiqueta imprimivel para o contentor do vinho engarrafado. */
    @GetMapping("/{id}/etiqueta")
    public String etiqueta(@PathVariable Long id, Model model, RedirectAttributes ra) {
        VinhoEngarrafado v = repo.findById(id).orElse(null);
        if (v == null) { ra.addFlashAttribute("erro", "Vinho engarrafado nao encontrado."); return "redirect:/produtos/engarrafados"; }
        model.addAttribute("v", v);
        return "produtos/engarrafados/etiqueta";
    }
}
