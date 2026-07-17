package pt.acv.adega.produtos;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.CastaRepository;
import pt.acv.adega.fichas.DepositoRepository;
import pt.acv.adega.fichas.TalhaRepository;

import java.time.LocalDateTime;

/**
 * Mapa de mostos (existencias). No fluxo normal as fichas de mosto sao geradas
 * pelos processos (Moagem, etc.). Para uma adega "a meio" (que ja tem mosto/vinho
 * a granel nos armazens antes de comecar a usar a aplicacao), o administrador
 * pode registar diretamente o saldo inicial.
 */
@Controller
@RequestMapping("/produtos/mostos")
public class MostoController {

    private final MostoRepository repo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final CastaRepository castaRepo;
    private final CodigoService codigoService;

    public MostoController(MostoRepository repo, TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                           CastaRepository castaRepo, CodigoService codigoService) {
        this.repo = repo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.castaRepo = castaRepo;
        this.codigoService = codigoService;
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

    // ----- Saldo inicial (adega a meio) — só administrador -----

    @GetMapping("/saldo-inicial")
    public String novoSaldo(Model model) {
        Mosto m = new Mosto();
        m.setDataProducao(LocalDateTime.now());
        model.addAttribute("mosto", m);
        preencherOpcoes(model);
        return "produtos/mostos/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Mosto m = repo.findById(id).orElse(null);
        if (m == null) { ra.addFlashAttribute("erro", "Mosto nao encontrado."); return "redirect:/produtos/mostos"; }
        model.addAttribute("mosto", m);
        preencherOpcoes(model);
        return "produtos/mostos/form";
    }

    @PostMapping("/saldo-inicial")
    public String guardarSaldo(@Valid @ModelAttribute("mosto") Mosto m, BindingResult result,
                               Model model, RedirectAttributes ra) {
        // Um mosto esta numa talha OU num deposito, nunca em ambos.
        if (m.getTalha() != null) m.setDeposito(null);
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "produtos/mostos/form";
        }
        if (m.getId() == null) {
            m.setCodigo(codigoService.proximoCodigo(Mosto.PREFIXO));
            if (m.getOrigemDescricao() == null || m.getOrigemDescricao().isBlank()) {
                m.setOrigemDescricao("Saldo inicial (adega a meio)");
            }
        }
        repo.save(m);
        ra.addFlashAttribute("sucesso", "Saldo de mosto guardado: " + m.getCodigo());
        return "redirect:/produtos/mostos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Mosto eliminado.");
        return "redirect:/produtos/mostos";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("talhas", talhaRepo.findAllByOrderByIdentificacaoAsc());
        model.addAttribute("depositos", depositoRepo.findAllByOrderByIdentificacaoAsc());
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("estados", EstadoMosto.values());
    }
}
