package pt.acv.adega.tratamentos;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

import java.time.LocalDate;

/**
 * Tratamentos enológicos — transversal a qualquer fase. Escolhe-se a adega, o
 * vinho (mosto ou granel) e vê-se onde está; regista-se a data e o tratamento
 * aplicado. Repetível, para rastrear o histórico do vinho ao longo do tempo.
 */
@Controller
@RequestMapping("/tratamentos")
public class TratamentoEnologicoController {

    private final TratamentoEnologicoRepository repo;
    private final LocalizacaoVinhoService localizacaoService;
    private final CodigoService codigoService;

    public TratamentoEnologicoController(TratamentoEnologicoRepository repo,
                                         LocalizacaoVinhoService localizacaoService, CodigoService codigoService) {
        this.repo = repo;
        this.localizacaoService = localizacaoService;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tratamentos", repo.findAllByOrderByVinhoNomeAscDataTratamentoAsc());
        return "tratamentos/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        TratamentoEnologico t = new TratamentoEnologico();
        t.setDataTratamento(LocalDate.now());
        model.addAttribute("tratamento", t);
        preencherOpcoes(model);
        return "tratamentos/form";
    }

    @PostMapping
    public String guardar(@ModelAttribute("tratamento") TratamentoEnologico tratamento,
                          Authentication auth, RedirectAttributes ra) {
        if (tratamento.getId() == null) {
            tratamento.setCodigo(codigoService.proximoCodigo(TratamentoEnologico.PREFIXO));
            tratamento.setCriadoPor(auth.getName());
        } else {
            TratamentoEnologico existente = repo.findById(tratamento.getId()).orElse(null);
            if (existente != null) tratamento.setCriadoPor(existente.getCriadoPor());
        }
        repo.save(tratamento);
        ra.addFlashAttribute("sucesso", "Tratamento registado: " + tratamento.getCodigo());
        return "redirect:/tratamentos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Tratamento eliminado.");
        return "redirect:/tratamentos";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("categorias", CategoriaVinho.values());
        model.addAttribute("dadosPorCategoria", localizacaoService.dadosPorCategoria());
    }
}
