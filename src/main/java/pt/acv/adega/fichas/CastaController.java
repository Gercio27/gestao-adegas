package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/castas")
public class CastaController {

    private final CastaRepository repo;
    private final CodigoService codigoService;

    public CastaController(CastaRepository repo, CodigoService codigoService) {
        this.repo = repo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("castas", repo.findAllByOrderByNomeAsc());
        return "fichas/castas/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("casta", new Casta());
        model.addAttribute("cores", CorCasta.values());
        return "fichas/castas/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Casta casta = repo.findById(id).orElse(null);
        if (casta == null) {
            ra.addFlashAttribute("erro", "Casta nao encontrada.");
            return "redirect:/fichas/castas";
        }
        model.addAttribute("casta", casta);
        model.addAttribute("cores", CorCasta.values());
        return "fichas/castas/form";
    }

    /**
     * Cria ou altera uma casta (so' o administrador — ver SecurityConfig). Nao
     * deixa criar duas castas com o mesmo nome: a casta e' escolhida pelo nome
     * em quase todos os ecras, e nomes repetidos tornam a escolha ambigua.
     */
    @PostMapping
    public String guardar(@Valid @ModelAttribute("casta") Casta casta, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (casta.getNome() != null && !casta.getNome().isBlank()) {
            Casta comOMesmoNome = repo.findFirstByNomeIgnoreCase(casta.getNome().trim()).orElse(null);
            if (comOMesmoNome != null && !comOMesmoNome.getId().equals(casta.getId())) {
                result.rejectValue("nome", "duplicado",
                        "Já existe uma casta com este nome (" + comOMesmoNome.getCodigo() + ").");
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("cores", CorCasta.values());
            return "fichas/castas/form";
        }
        if (casta.getNome() != null) casta.setNome(casta.getNome().trim());
        if (casta.getId() == null) {
            casta.setCodigo(codigoService.proximoCodigo(Casta.PREFIXO));
        }
        repo.save(casta);
        ra.addFlashAttribute("sucesso", "Casta guardada: " + casta.getCodigo());
        return "redirect:/fichas/castas";
    }

    /**
     * Elimina a casta. Se ja' estiver a ser usada (parcelas, mostos, moagens...),
     * a base de dados recusa — devolve-se a mensagem em vez de rebentar o ecra.
     */
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repo.deleteById(id);
            ra.addFlashAttribute("sucesso", "Casta eliminada.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("erro", "Esta casta já está a ser usada (vinhas, mostos ou moagens) "
                    + "e por isso não pode ser eliminada.");
        }
        return "redirect:/fichas/castas";
    }
}
