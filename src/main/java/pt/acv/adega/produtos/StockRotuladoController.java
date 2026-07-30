package pt.acv.adega.produtos;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vinho ja rotulado e encaixotado, por vinho e por local — o que esta pronto a
 * ser entregue ao comercial. Serve para rastrear onde esta cada vinho depois
 * de sair dos contentores.
 */
@Controller
@RequestMapping("/produtos/rotulados")
public class StockRotuladoController {

    private final StockRotuladoRepository repo;

    public StockRotuladoController(StockRotuladoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String listar(Model model) {
        List<StockRotulado> stock = repo.findByGarrafasGreaterThanOrderByVinhoNomeAsc(0);
        model.addAttribute("stock", stock);

        // Totais por vinho, para se ver o que existe ao todo.
        Map<String, Integer> totais = new LinkedHashMap<>();
        int garrafas = 0;
        for (StockRotulado s : stock) {
            String nome = s.getVinhoNome() != null ? s.getVinhoNome() : "(vinho sem nome)";
            totais.merge(nome, s.getGarrafas(), Integer::sum);
            garrafas += s.getGarrafas();
        }
        model.addAttribute("totaisPorVinho", totais);
        model.addAttribute("totalGarrafas", garrafas);
        return "produtos/rotulados";
    }
}
