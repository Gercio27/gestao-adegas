package pt.acv.adega.planeamento;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.Parcela;
import pt.acv.adega.fichas.ParcelaRepository;
import pt.acv.adega.processos.maturacao.ProcessoAnaliseMaturacao;
import pt.acv.adega.processos.maturacao.ProcessoAnaliseMaturacaoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Planeamento dos vinhos (Fase 1.2). Cada vinho tem nome, tipo, datas e uma
 * lista de parcelas com o Kg de uva a aplicar. A producao prevista vive na
 * parcela; o saldo de cada parcela = producao - total ja aplicado em todos os
 * vinhos. Um vinho novo so pode usar parcelas com saldo disponivel.
 */
@Controller
@RequestMapping("/planeamento")
public class PlaneamentoController {

    private final PlaneamentoVinhoRepository repo;
    private final ProcessoAnaliseMaturacaoRepository maturacaoRepo;
    private final ParcelaRepository parcelaRepo;
    private final CodigoService codigoService;

    public PlaneamentoController(PlaneamentoVinhoRepository repo, ProcessoAnaliseMaturacaoRepository maturacaoRepo,
                                 ParcelaRepository parcelaRepo, CodigoService codigoService) {
        this.repo = repo;
        this.maturacaoRepo = maturacaoRepo;
        this.parcelaRepo = parcelaRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String mapa(Model model) {
        Map<Long, BigDecimal> aplicado = aplicadoPorParcela(null);
        List<VinhoMapa> vinhos = new ArrayList<>();
        BigDecimal totalKg = BigDecimal.ZERO;
        BigDecimal totalLitros = BigDecimal.ZERO;

        for (PlaneamentoVinho p : repo.findAllByOrderByNomeVinhoAsc()) {
            List<LinhaMapa> linhas = new ArrayList<>();
            for (LinhaPlaneamentoParcela l : p.getLinhas()) {
                Parcela pc = l.getParcela();
                BigDecimal saldo = null;
                ProcessoAnaliseMaturacao analise = null;
                if (pc != null) {
                    BigDecimal prod = pc.getProducaoPrevistaKg();
                    BigDecimal apl = aplicado.getOrDefault(pc.getId(), BigDecimal.ZERO);
                    if (prod != null) saldo = prod.subtract(apl);
                    if (pc.getVinha() != null && pc.getCasta() != null) {
                        analise = maturacaoRepo.findFirstByVinhaIdAndCastaIdOrderByDataCriacaoDesc(
                                pc.getVinha().getId(), pc.getCasta().getId()).orElse(null);
                    }
                }
                linhas.add(new LinhaMapa(l, saldo, analise));
                if (l.getKgAplicar() != null) totalKg = totalKg.add(l.getKgAplicar());
            }
            totalLitros = totalLitros.add(p.getTotalLitrosPrevistos());
            vinhos.add(new VinhoMapa(p, linhas));
        }

        model.addAttribute("vinhos", vinhos);
        model.addAttribute("totalKg", totalKg);
        model.addAttribute("totalLitros", totalLitros);
        return "planeamento/mapa";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        PlaneamentoVinho p = new PlaneamentoVinho();
        p.setDataPlaneamento(LocalDate.now());
        model.addAttribute("plano", p);
        preencherOpcoes(model, null);
        return "planeamento/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        PlaneamentoVinho p = repo.findById(id).orElse(null);
        if (p == null) { ra.addFlashAttribute("erro", "Vinho não encontrado."); return "redirect:/planeamento"; }
        // Preenche a producao (transiente) de cada linha a partir da respetiva parcela.
        for (LinhaPlaneamentoParcela l : p.getLinhas()) {
            if (l.getParcela() != null) l.setProducaoPrevistaKg(l.getParcela().getProducaoPrevistaKg());
        }
        model.addAttribute("plano", p);
        preencherOpcoes(model, id);
        return "planeamento/form";
    }

    @PostMapping
    @Transactional
    public String guardar(@Valid @ModelAttribute("plano") PlaneamentoVinho plano, BindingResult result, Model model,
                          RedirectAttributes ra) {
        // Descarta linhas submetidas sem parcela.
        plano.getLinhas().removeIf(l -> l.getParcela() == null);

        if (result.hasErrors()) {
            preencherOpcoes(model, plano.getId());
            return "planeamento/form";
        }
        if (plano.getId() == null) {
            plano.setCodigo(codigoService.proximoCodigo(PlaneamentoVinho.PREFIXO));
        }
        for (LinhaPlaneamentoParcela l : plano.getLinhas()) {
            l.setPlaneamento(plano);
            // Escreve a producao prevista introduzida de volta na parcela (partilhada).
            if (l.getParcela() != null && l.getProducaoPrevistaKg() != null) {
                Parcela pc = l.getParcela();
                pc.setProducaoPrevistaKg(l.getProducaoPrevistaKg());
                parcelaRepo.save(pc);
            }
        }
        repo.save(plano);
        ra.addFlashAttribute("sucesso", "Planeamento guardado: " + plano.getNomeVinho());
        return "redirect:/planeamento";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Vinho eliminado do planeamento.");
        return "redirect:/planeamento";
    }

    // ----- auxiliares -----

    private void preencherOpcoes(Model model, Long excluirPlaneamentoId) {
        model.addAttribute("tipos", TipoVinho.values());
        model.addAttribute("parcelaOpcoes", parcelaOpcoes(excluirPlaneamentoId));
    }

    /** Soma de Kg a aplicar por parcela em todos os vinhos (opcionalmente excluindo um). */
    private Map<Long, BigDecimal> aplicadoPorParcela(Long excluirPlaneamentoId) {
        Map<Long, BigDecimal> mapa = new HashMap<>();
        for (PlaneamentoVinho pv : repo.findAll()) {
            if (excluirPlaneamentoId != null && excluirPlaneamentoId.equals(pv.getId())) continue;
            for (LinhaPlaneamentoParcela l : pv.getLinhas()) {
                if (l.getParcela() == null || l.getKgAplicar() == null) continue;
                mapa.merge(l.getParcela().getId(), l.getKgAplicar(), BigDecimal::add);
            }
        }
        return mapa;
    }

    /** Dados de todas as parcelas para o formulario (o JS usa-os para o saldo). */
    private List<Map<String, Object>> parcelaOpcoes(Long excluirPlaneamentoId) {
        Map<Long, BigDecimal> aplicado = aplicadoPorParcela(excluirPlaneamentoId);
        List<Parcela> parcelas = new ArrayList<>(parcelaRepo.findAll());
        parcelas.sort(Comparator
                .comparing((Parcela p) -> p.getVinha() != null ? p.getVinha().getNome() : "", String.CASE_INSENSITIVE_ORDER)
                .thenComparing(p -> nomeParcela(p), String.CASE_INSENSITIVE_ORDER));

        List<Map<String, Object>> ops = new ArrayList<>();
        for (Parcela p : parcelas) {
            BigDecimal prod = p.getProducaoPrevistaKg();
            BigDecimal apl = aplicado.getOrDefault(p.getId(), BigDecimal.ZERO);
            boolean disponivel = (prod == null) || prod.subtract(apl).compareTo(BigDecimal.ZERO) > 0;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("vinha", p.getVinha() != null ? p.getVinha().getNome() : "—");
            m.put("nome", nomeParcela(p));
            m.put("casta", p.getCasta() != null ? p.getCasta().getNome() : "—");
            m.put("area", p.getAreaHa());
            m.put("producao", prod);
            m.put("aplicado", apl);
            m.put("disponivel", disponivel);
            ops.add(m);
        }
        return ops;
    }

    private String nomeParcela(Parcela p) {
        if (p.getNome() != null && !p.getNome().isBlank()) return p.getNome();
        if (p.getIdentificacao() != null && !p.getIdentificacao().isBlank()) return p.getIdentificacao();
        return "Parcela " + p.getId();
    }
}
