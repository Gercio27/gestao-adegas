package pt.acv.adega.produtos;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Mapa de mostos (existencias). Apenas consulta: as fichas de mosto sao geradas
 * pelos processos (Moagem, etc.), nunca criadas a mao.
 */
@Controller
@RequestMapping("/produtos/mostos")
public class MostoController {

    private final MostoRepository repo;

    public MostoController(MostoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("mostos", repo.findAllByOrderByDataProducaoDesc());
        return "produtos/mostos/lista";
    }

    /** Mapa de existencias de vinhos prontos a granel (Fase 4.5 / 5.1). */
    @GetMapping("/vinhos-granel")
    public String vinhosGranel(Model model) {
        var vinhos = repo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL);
        var totalLitros = vinhos.stream()
                .map(Mosto::getLitros)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        model.addAttribute("vinhos", vinhos);
        model.addAttribute("totalLitros", totalLitros);
        return "produtos/vinhos_granel/lista";
    }
}
