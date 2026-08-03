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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

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
    public String listar(@RequestParam(required = false) Integer ano,
                         @RequestParam(required = false) String local,
                         @RequestParam(required = false) String vinho,
                         Model model) {
        List<Mosto> todos = repo.findAllByOrderByDataProducaoDesc();
        List<Mosto> lista = filtrar(todos, ano, local, vinho);
        model.addAttribute("mostos", lista);
        preencherFiltros(model, todos, lista, ano, local, vinho);
        return "produtos/mostos/lista";
    }

    /** Mapa de existencias de vinhos prontos a granel (Fase 4.5 / 5.1). */
    @GetMapping("/vinhos-granel")
    public String vinhosGranel(@RequestParam(required = false) Integer ano,
                               @RequestParam(required = false) String local,
                               @RequestParam(required = false) String vinho,
                               Model model) {
        List<Mosto> todos = repo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL);
        List<Mosto> lista = filtrar(todos, ano, local, vinho);
        var totalLitros = lista.stream()
                .map(Mosto::getLitros)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("vinhos", lista);
        model.addAttribute("totalLitros", totalLitros);
        preencherFiltros(model, todos, lista, ano, local, vinho);
        return "produtos/vinhos_granel/lista";
    }

    /** Ano de produção, adega/armazém onde está e nome do vinho. */
    private List<Mosto> filtrar(List<Mosto> todos, Integer ano, String local, String vinho) {
        List<Mosto> out = new ArrayList<>();
        for (Mosto m : todos) {
            if (ano != null && (m.getAno() == null || !ano.equals(m.getAno()))) continue;
            if (local != null && !local.isBlank() && !local.equals(m.getLocalNome())) continue;
            if (vinho != null && !vinho.isBlank() && !vinho.equals(m.getVinhoNome())) continue;
            out.add(m);
        }
        return out;
    }

    /**
     * As opções dos seletores saem sempre da lista completa (não da filtrada),
     * senão ao escolher um ano deixavam de aparecer os outros.
     */
    private void preencherFiltros(Model model, List<Mosto> todos, List<Mosto> lista,
                                  Integer ano, String local, String vinho) {
        TreeSet<Integer> anos = new TreeSet<>(Comparator.reverseOrder());
        TreeSet<String> locais = new TreeSet<>();
        TreeSet<String> vinhos = new TreeSet<>();
        for (Mosto m : todos) {
            if (m.getAno() != null) anos.add(m.getAno());
            if (m.getLocalNome() != null && !"—".equals(m.getLocalNome())) locais.add(m.getLocalNome());
            if (m.getVinhoNome() != null && !m.getVinhoNome().isBlank()) vinhos.add(m.getVinhoNome());
        }
        model.addAttribute("anos", anos);
        model.addAttribute("locais", locais);
        model.addAttribute("nomesVinhos", vinhos);
        model.addAttribute("fAno", ano);
        model.addAttribute("fLocal", local);
        model.addAttribute("fVinho", vinho);
        model.addAttribute("totalFiltrado", lista.size());
        model.addAttribute("totalGeral", todos.size());
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
