package pt.acv.adega.produtos;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

/**
 * Gestao de lotes (Fase 4.6). Ao guardar, associa os vinhos a granel
 * selecionados ao lote (grava o codigo do lote em cada mosto).
 */
@Controller
@RequestMapping("/produtos/lotes")
public class LoteController {

    private final LoteRepository repo;
    private final MostoRepository mostoRepo;
    private final CodigoService codigoService;

    public LoteController(LoteRepository repo, MostoRepository mostoRepo, CodigoService codigoService) {
        this.repo = repo;
        this.mostoRepo = mostoRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lotes", repo.findAllByOrderByDataCriacaoDesc());
        return "produtos/lotes/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("lote", new Lote());
        model.addAttribute("mostos", mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL));
        return "produtos/lotes/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Lote l = repo.findById(id).orElse(null);
        if (l == null) { ra.addFlashAttribute("erro", "Lote não encontrado."); return "redirect:/produtos/lotes"; }
        model.addAttribute("lote", l);
        model.addAttribute("vinhos", mostoRepo.findByLoteCodigo(l.getCodigo()));
        return "produtos/lotes/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Lote l = repo.findById(id).orElse(null);
        if (l == null) { ra.addFlashAttribute("erro", "Lote não encontrado."); return "redirect:/produtos/lotes"; }
        // Pre-seleciona os mostos ja no lote
        l.setMostoIds(mostoRepo.findByLoteCodigo(l.getCodigo()).stream().map(m -> m.getId()).toList());
        model.addAttribute("lote", l);
        model.addAttribute("mostos", mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL));
        return "produtos/lotes/form";
    }

    @PostMapping
    @Transactional
    public String guardar(@Valid @ModelAttribute("lote") Lote lote, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("mostos", mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL));
            return "produtos/lotes/form";
        }
        if (lote.getId() == null) {
            lote.setCodigo(codigoService.proximoCodigo(Lote.PREFIXO));
        }
        repo.save(lote);
        // Reatribui: limpa os que estavam neste lote e marca os selecionados
        mostoRepo.findByLoteCodigo(lote.getCodigo()).forEach(m -> { m.setLoteCodigo(null); mostoRepo.save(m); });
        if (lote.getMostoIds() != null) {
            for (Long mid : lote.getMostoIds()) {
                mostoRepo.findById(mid).ifPresent(m -> { m.setLoteCodigo(lote.getCodigo()); mostoRepo.save(m); });
            }
        }
        ra.addFlashAttribute("sucesso", "Lote guardado: " + lote.getCodigo());
        return "redirect:/produtos/lotes/" + lote.getId();
    }

    @PostMapping("/{id}/eliminar")
    @Transactional
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        Lote l = repo.findById(id).orElse(null);
        if (l != null) {
            mostoRepo.findByLoteCodigo(l.getCodigo()).forEach(m -> { m.setLoteCodigo(null); mostoRepo.save(m); });
            repo.delete(l);
        }
        ra.addFlashAttribute("sucesso", "Lote eliminado.");
        return "redirect:/produtos/lotes";
    }
}
