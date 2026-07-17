package pt.acv.adega.produtos;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.CastaRepository;
import pt.acv.adega.fichas.ContentorGarrafas;
import pt.acv.adega.fichas.ContentorGarrafasRepository;

import java.time.LocalDateTime;

/**
 * Mapa de vinhos engarrafados (produtos acabados). No fluxo normal sao gerados
 * pelo Engarrafamento. Para uma adega "a meio", o administrador pode registar o
 * saldo inicial de vinho ja engarrafado e coloca-lo num contentor.
 */
@Controller
@RequestMapping("/produtos/engarrafados")
public class VinhoEngarrafadoController {

    private final VinhoEngarrafadoRepository repo;
    private final CastaRepository castaRepo;
    private final ContentorGarrafasRepository contentorRepo;
    private final CodigoService codigoService;

    public VinhoEngarrafadoController(VinhoEngarrafadoRepository repo, CastaRepository castaRepo,
                                      ContentorGarrafasRepository contentorRepo, CodigoService codigoService) {
        this.repo = repo;
        this.castaRepo = castaRepo;
        this.contentorRepo = contentorRepo;
        this.codigoService = codigoService;
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

    // ----- Saldo inicial (adega a meio) — só administrador (protegido no SecurityConfig) -----

    @GetMapping("/saldo-inicial")
    public String novoSaldo(Model model) {
        VinhoEngarrafado v = new VinhoEngarrafado();
        v.setDataProducao(LocalDateTime.now());
        model.addAttribute("engarrafado", v);
        preencherOpcoes(model);
        return "produtos/engarrafados/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        VinhoEngarrafado v = repo.findById(id).orElse(null);
        if (v == null) { ra.addFlashAttribute("erro", "Vinho engarrafado nao encontrado."); return "redirect:/produtos/engarrafados"; }
        model.addAttribute("engarrafado", v);
        preencherOpcoes(model);
        return "produtos/engarrafados/form";
    }

    @PostMapping("/saldo-inicial")
    public String guardarSaldo(@Valid @ModelAttribute("engarrafado") VinhoEngarrafado v, BindingResult result,
                               @RequestParam(value = "contentorId", required = false) Long contentorId,
                               Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "produtos/engarrafados/form";
        }
        boolean novo = v.getId() == null;
        if (novo) {
            v.setCodigo(codigoService.proximoCodigo(VinhoEngarrafado.PREFIXO));
            if (v.getOrigemDescricao() == null || v.getOrigemDescricao().isBlank()) {
                v.setOrigemDescricao("Saldo inicial (adega a meio)");
            }
        }
        repo.save(v);

        // Coloca as garrafas no contentor escolhido (opcional).
        if (contentorId != null) {
            ContentorGarrafas c = contentorRepo.findById(contentorId).orElse(null);
            if (c != null) {
                c.setVinhoEngarrafadoId(v.getId());
                c.setVinhoNome(v.getNome());
                c.setGarrafasAtuais(v.getNumeroGarrafas());
                c.setRotulado(v.isRotulado());
                contentorRepo.save(c);
            }
        }
        ra.addFlashAttribute("sucesso", "Saldo de vinho engarrafado guardado: " + v.getCodigo());
        return "redirect:/produtos/engarrafados";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Vinho engarrafado eliminado.");
        return "redirect:/produtos/engarrafados";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("contentores", contentorRepo.findAllByOrderByNomeAsc());
    }
}
