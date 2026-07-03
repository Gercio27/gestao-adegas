package pt.acv.adega.planeamento;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.AdegaRepository;
import pt.acv.adega.fichas.CastaRepository;
import pt.acv.adega.fichas.VinhaRepository;
import pt.acv.adega.processos.maturacao.ProcessoAnaliseMaturacaoRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Planeamento dos vinhos (Fase 1.2). O mapa junta a cada linha de planeamento a
 * análise à maturação mais recente da respetiva vinha/casta (dados automáticos,
 * não editáveis aqui).
 */
@Controller
@RequestMapping("/planeamento")
public class PlaneamentoController {

    private final PlaneamentoVinhoRepository repo;
    private final ProcessoAnaliseMaturacaoRepository maturacaoRepo;
    private final VinhaRepository vinhaRepo;
    private final CastaRepository castaRepo;
    private final AdegaRepository adegaRepo;
    private final CodigoService codigoService;

    public PlaneamentoController(PlaneamentoVinhoRepository repo, ProcessoAnaliseMaturacaoRepository maturacaoRepo,
                                 VinhaRepository vinhaRepo, CastaRepository castaRepo, AdegaRepository adegaRepo,
                                 CodigoService codigoService) {
        this.repo = repo;
        this.maturacaoRepo = maturacaoRepo;
        this.vinhaRepo = vinhaRepo;
        this.castaRepo = castaRepo;
        this.adegaRepo = adegaRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String mapa(Model model) {
        List<LinhaPlaneamento> linhas = new ArrayList<>();
        BigDecimal totalKg = BigDecimal.ZERO;
        for (PlaneamentoVinho p : repo.findAllByOrderByVinhaNomeAscCastaNomeAsc()) {
            var analise = (p.getVinha() != null && p.getCasta() != null)
                    ? maturacaoRepo.findFirstByVinhaIdAndCastaIdOrderByDataCriacaoDesc(p.getVinha().getId(), p.getCasta().getId()).orElse(null)
                    : null;
            linhas.add(new LinhaPlaneamento(p, analise));
            if (p.getQuantidadePrevistaKg() != null) totalKg = totalKg.add(p.getQuantidadePrevistaKg());
        }
        model.addAttribute("linhas", linhas);
        model.addAttribute("totalKg", totalKg);
        return "planeamento/mapa";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("plano", new PlaneamentoVinho());
        preencherOpcoes(model);
        return "planeamento/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        PlaneamentoVinho p = repo.findById(id).orElse(null);
        if (p == null) { ra.addFlashAttribute("erro", "Linha não encontrada."); return "redirect:/planeamento"; }
        model.addAttribute("plano", p);
        preencherOpcoes(model);
        return "planeamento/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("plano") PlaneamentoVinho plano, BindingResult result, Model model,
                          RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "planeamento/form";
        }
        if (plano.getId() == null) {
            plano.setCodigo(codigoService.proximoCodigo(PlaneamentoVinho.PREFIXO));
        }
        repo.save(plano);
        ra.addFlashAttribute("sucesso", "Planeamento guardado.");
        return "redirect:/planeamento";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Linha de planeamento eliminada.");
        return "redirect:/planeamento";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("vinhas", vinhaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
    }
}
