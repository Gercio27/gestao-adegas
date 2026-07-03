package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/depositos")
public class DepositoController {

    private final DepositoRepository repo;
    private final AdegaRepository adegaRepo;
    private final CodigoService codigoService;
    private final pt.acv.adega.produtos.MostoRepository mostoRepo;

    public DepositoController(DepositoRepository repo, AdegaRepository adegaRepo, CodigoService codigoService,
                              pt.acv.adega.produtos.MostoRepository mostoRepo) {
        this.repo = repo;
        this.adegaRepo = adegaRepo;
        this.codigoService = codigoService;
        this.mostoRepo = mostoRepo;
    }

    @GetMapping("/{id}/etiqueta")
    public String etiqueta(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Deposito d = repo.findById(id).orElse(null);
        if (d == null) { ra.addFlashAttribute("erro", "Deposito nao encontrado."); return "redirect:/fichas/depositos"; }
        model.addAttribute("tipo", "Depósito");
        model.addAttribute("codigo", d.getCodigo());
        model.addAttribute("identificacao", d.getIdentificacao());
        model.addAttribute("adega", d.getAdega() != null ? d.getAdega().getNome() : null);
        model.addAttribute("capacidade", d.getCapacidadeLitros());
        model.addAttribute("volume", d.getVolumeAtualLitros());
        model.addAttribute("propriedade", d.getPropriedade().getDescricao());
        model.addAttribute("conteudos", mostoRepo.findByDepositoId(id));
        return "fichas/etiqueta";
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("depositos", repo.findAllByOrderByIdentificacaoAsc());
        return "fichas/depositos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("deposito", new Deposito());
        preencherOpcoes(model);
        return "fichas/depositos/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Deposito d = repo.findById(id).orElse(null);
        if (d == null) {
            ra.addFlashAttribute("erro", "Deposito nao encontrado.");
            return "redirect:/fichas/depositos";
        }
        model.addAttribute("deposito", d);
        preencherOpcoes(model);
        return "fichas/depositos/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("deposito") Deposito d, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "fichas/depositos/form";
        }
        if (d.getId() == null) {
            d.setCodigo(codigoService.proximoCodigo(Deposito.PREFIXO));
        }
        repo.save(d);
        ra.addFlashAttribute("sucesso", "Deposito guardado: " + d.getCodigo());
        return "redirect:/fichas/depositos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Deposito eliminado.");
        return "redirect:/fichas/depositos";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("propriedades", Propriedade.values());
    }
}
