package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
@RequestMapping("/fichas/vinhas")
public class VinhaController {

    private final VinhaRepository repo;
    private final CastaRepository castaRepo;
    private final CodigoService codigoService;

    public VinhaController(VinhaRepository repo, CastaRepository castaRepo, CodigoService codigoService) {
        this.repo = repo;
        this.castaRepo = castaRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("vinhas", repo.findAllByOrderByNomeAsc());
        return "fichas/vinhas/lista";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Vinha v = repo.findById(id).orElse(null);
        if (v == null) {
            ra.addFlashAttribute("erro", "Vinha nao encontrada.");
            return "redirect:/fichas/vinhas";
        }
        model.addAttribute("vinha", v);
        return "fichas/vinhas/detalhe";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        Vinha v = new Vinha();
        v.getParcelas().add(new Parcela()); // uma linha inicial
        model.addAttribute("vinha", v);
        preencherOpcoes(model);
        return "fichas/vinhas/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Vinha v = repo.findById(id).orElse(null);
        if (v == null) {
            ra.addFlashAttribute("erro", "Vinha nao encontrada.");
            return "redirect:/fichas/vinhas";
        }
        model.addAttribute("vinha", v);
        preencherOpcoes(model);
        return "fichas/vinhas/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("vinha") Vinha vinha, BindingResult result,
                          Model model, RedirectAttributes ra) {
        limparParcelasVazias(vinha);
        resolverCastas(vinha);
        for (Parcela p : vinha.getParcelas()) {
            p.setVinha(vinha);
        }
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "fichas/vinhas/form";
        }
        if (vinha.getId() == null) {
            vinha.setCodigo(codigoService.proximoCodigo(Vinha.PREFIXO));
        }
        repo.save(vinha);
        ra.addFlashAttribute("sucesso", "Vinha guardada: " + vinha.getCodigo());
        return "redirect:/fichas/vinhas";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Vinha eliminada.");
        return "redirect:/fichas/vinhas";
    }

    /** Remove linhas de parcela totalmente vazias vindas do formulario. */
    private void limparParcelasVazias(Vinha vinha) {
        Iterator<Parcela> it = vinha.getParcelas().iterator();
        while (it.hasNext()) {
            Parcela p = it.next();
            boolean semId = p.getIdentificacao() == null || p.getIdentificacao().isBlank();
            boolean semCasta = p.getCasta() == null || p.getCasta().getId() == null;
            boolean semArea = p.getAreaHa() == null;
            boolean semAno = p.getAnoPlantacao() == null;
            if (semId && semCasta && semArea && semAno) {
                it.remove();
            }
        }
    }

    /** Substitui as castas "leves" (so com id) pelas entidades geridas. */
    private void resolverCastas(Vinha vinha) {
        for (Parcela p : vinha.getParcelas()) {
            if (p.getCasta() != null && p.getCasta().getId() != null) {
                castaRepo.findById(p.getCasta().getId()).ifPresent(p::setCasta);
            } else {
                p.setCasta(null);
            }
        }
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("propriedades", Propriedade.values());
    }
}
