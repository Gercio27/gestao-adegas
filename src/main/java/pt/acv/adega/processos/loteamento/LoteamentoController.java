package pt.acv.adega.processos.loteamento;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.*;
import pt.acv.adega.planeamento.PlaneamentoVinho;
import pt.acv.adega.planeamento.PlaneamentoVinhoRepository;
import pt.acv.adega.processos.moagem.ProcessoMoagem;
import pt.acv.adega.processos.moagem.ProcessoMoagemRepository;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/processos/loteamento")
public class LoteamentoController {

    private final LoteamentoRepository loteRepo;
    private final LoteLinhaRepository linhaRepo;
    private final LoteConstrucaoRepository construcaoRepo;
    private final LoteamentoService service;
    private final MostoRepository mostoRepo;
    private final AdegaRepository adegaRepo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final ProcessoMoagemRepository moagemRepo;
    private final PlaneamentoVinhoRepository planeamentoRepo;
    private final CodigoService codigoService;

    public LoteamentoController(LoteamentoRepository loteRepo, LoteLinhaRepository linhaRepo,
                                LoteConstrucaoRepository construcaoRepo, LoteamentoService service,
                                MostoRepository mostoRepo, AdegaRepository adegaRepo, TalhaRepository talhaRepo,
                                DepositoRepository depositoRepo, ProcessoMoagemRepository moagemRepo,
                                PlaneamentoVinhoRepository planeamentoRepo,
                                CodigoService codigoService) {
        this.loteRepo = loteRepo;
        this.linhaRepo = linhaRepo;
        this.construcaoRepo = construcaoRepo;
        this.service = service;
        this.mostoRepo = mostoRepo;
        this.adegaRepo = adegaRepo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.moagemRepo = moagemRepo;
        this.planeamentoRepo = planeamentoRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lotes", loteRepo.findAllByOrderByDataCriacaoDesc());
        return "processos/loteamento/lista";
    }

    // ----- 6.1 Planeamento -----

    @GetMapping("/planear")
    public String planear(Model model) {
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("granelPorAdega", granelPorAdega());
        return "processos/loteamento/planeamento";
    }

    @PostMapping("/planear")
    @Transactional
    public String guardarPlano(@RequestParam String nome, @RequestParam(required = false) Long adegaId,
                               @RequestParam(required = false) String linhas,
                               Authentication auth, RedirectAttributes ra) {
        if (nome == null || nome.isBlank()) { ra.addFlashAttribute("erro", "Dê um nome ao vinho do lote."); return "redirect:/processos/loteamento/planear"; }
        String emUso = nomeEmUso(nome.trim());
        if (emUso != null) {
            ra.addFlashAttribute("erro", emUso);
            return "redirect:/processos/loteamento/planear";
        }
        Loteamento lote = new Loteamento();
        lote.setCodigo(codigoService.proximoCodigo(Loteamento.PREFIXO));
        lote.setNome(nome.trim());
        if (adegaId != null) lote.setAdega(adegaRepo.findById(adegaId).orElse(null));
        lote.setDataPlaneamento(LocalDate.now());
        lote.setCriadoPor(auth.getName());
        loteRepo.save(lote);

        guardarLinhas(lote.getId(), linhas);
        ra.addFlashAttribute("sucesso", "Lote planeado: " + lote.getCodigo() + " · " + lote.getNome());
        return "redirect:/processos/loteamento/" + lote.getId();
    }

    /**
     * Correcoes ao planeado (6.2). Substitui as linhas do plano — entre o
     * planeamento e a construcao pode ter havido entradas/saidas/transfegas e os
     * depositos ja nao terem as quantidades de quando foi planeado.
     */
    @PostMapping("/{id}/linhas")
    @Transactional
    public String corrigirPlano(@PathVariable Long id, @RequestParam(required = false) String linhas,
                                RedirectAttributes ra) {
        Loteamento lote = loteRepo.findById(id).orElse(null);
        if (lote == null) { ra.addFlashAttribute("erro", "Lote não encontrado."); return "redirect:/processos/loteamento"; }
        if (lote.isConcluido()) { ra.addFlashAttribute("erro", "O lote já está concluído."); return "redirect:/processos/loteamento/" + id; }
        linhaRepo.deleteByLoteamentoId(id);
        guardarLinhas(id, linhas);
        ra.addFlashAttribute("sucesso", "Plano corrigido.");
        return "redirect:/processos/loteamento/" + id;
    }

    // ----- 6.2 Construção -----

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Loteamento lote = loteRepo.findById(id).orElse(null);
        if (lote == null) { ra.addFlashAttribute("erro", "Lote não encontrado."); return "redirect:/processos/loteamento"; }
        model.addAttribute("lote", lote);
        model.addAttribute("construcoes", construcaoRepo.findByLoteamentoIdOrderByNumeroAsc(id));

        // Plano com o disponivel de hoje: entre o planeamento e a construcao pode
        // ter havido movimentos e o deposito ja nao ter o que foi planeado.
        List<Map<String, Object>> linhasView = new ArrayList<>();
        BigDecimal totalPlaneado = BigDecimal.ZERO;
        for (LoteLinha ln : linhaRepo.findByLoteamentoId(id)) {
            Mosto m = ln.getMostoOrigemId() != null ? mostoRepo.findById(ln.getMostoOrigemId()).orElse(null) : null;
            BigDecimal disp = m != null && m.getLitros() != null ? m.getLitros() : BigDecimal.ZERO;
            BigDecimal plan = ln.getLitrosPlaneados() != null ? ln.getLitrosPlaneados() : BigDecimal.ZERO;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("mostoOrigemId", ln.getMostoOrigemId());
            row.put("origemDescricao", ln.getOrigemDescricao());
            row.put("litrosPlaneados", plan);
            row.put("disponivel", disp);
            row.put("insuficiente", disp.compareTo(plan) < 0);
            linhasView.add(row);
            totalPlaneado = totalPlaneado.add(plan);
        }
        model.addAttribute("linhas", linhasView);
        model.addAttribute("totalPlaneado", totalPlaneado);

        BigDecimal totalConstruido = BigDecimal.ZERO;
        for (LoteConstrucao c : construcaoRepo.findByLoteamentoIdOrderByNumeroAsc(id)) {
            if (c.getLitros() != null) totalConstruido = totalConstruido.add(c.getLitros());
        }
        model.addAttribute("totalConstruido", totalConstruido);

        Long adegaId = lote.getAdega() != null ? lote.getAdega().getId() : null;
        model.addAttribute("origens", granelDaAdega(adegaId, lote.getCodigo()));
        model.addAttribute("recipientes", recipientesDaAdega(adegaId));
        return "processos/loteamento/construcao";
    }

    @PostMapping("/{id}/construir")
    public String construir(@PathVariable Long id, @RequestParam Long mostoOrigemId,
                            @RequestParam String destinoRef, @RequestParam BigDecimal litros,
                            RedirectAttributes ra) {
        try {
            service.executarConstrucao(id, mostoOrigemId, destinoRef, litros);
            ra.addFlashAttribute("sucesso", "Construção registada.");
        } catch (LoteamentoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/loteamento/" + id;
    }

    @PostMapping("/construcao/{cid}/reverter")
    public String reverter(@PathVariable Long cid, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reverter."); return "redirect:/processos/loteamento"; }
        LoteConstrucao c = construcaoRepo.findById(cid).orElse(null);
        Long loteId = c != null ? c.getLoteamentoId() : null;
        try {
            service.reverterConstrucao(cid);
            ra.addFlashAttribute("sucesso", "Construção revertida.");
        } catch (LoteamentoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/loteamento/" + (loteId != null ? loteId : "");
    }

    @PostMapping("/{id}/concluir")
    public String concluir(@PathVariable Long id, RedirectAttributes ra) {
        Loteamento lote = loteRepo.findById(id).orElse(null);
        if (lote == null) { ra.addFlashAttribute("erro", "Lote não encontrado."); return "redirect:/processos/loteamento"; }
        if (construcaoRepo.countByLoteamentoId(id) == 0) {
            ra.addFlashAttribute("erro", "Faça pelo menos uma construção antes de concluir.");
            return "redirect:/processos/loteamento/" + id;
        }
        lote.setConcluido(true);
        loteRepo.save(lote);
        ra.addFlashAttribute("sucesso", "Lote concluído.");
        return "redirect:/processos/loteamento/" + id;
    }

    @PostMapping("/{id}/eliminar")
    @Transactional
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Loteamento lote = loteRepo.findById(id).orElse(null);
        if (lote == null) { ra.addFlashAttribute("erro", "Lote não encontrado."); return "redirect:/processos/loteamento"; }
        if (construcaoRepo.countByLoteamentoId(id) > 0) {
            ra.addFlashAttribute("erro", "Reverta as construções antes de eliminar o lote.");
            return "redirect:/processos/loteamento/" + id;
        }
        linhaRepo.deleteByLoteamentoId(id);
        loteRepo.delete(lote);
        ra.addFlashAttribute("sucesso", "Lote eliminado.");
        return "redirect:/processos/loteamento";
    }

    // ----- auxiliares -----

    /** Grava as linhas planeadas a partir do formato "mostoId:litros,mostoId:litros". */
    private void guardarLinhas(Long loteId, String linhas) {
        if (linhas == null || linhas.isBlank()) return;
        for (String par : linhas.split(",")) {
            String[] kv = par.split(":");
            if (kv.length != 2) continue;
            try {
                Long mid = Long.valueOf(kv[0].trim());
                BigDecimal lit = new BigDecimal(kv[1].trim());
                if (lit.signum() <= 0) continue;
                Mosto m = mostoRepo.findById(mid).orElse(null);
                if (m == null) continue;
                LoteLinha ln = new LoteLinha();
                ln.setLoteamentoId(loteId);
                ln.setMostoOrigemId(mid);
                ln.setOrigemDescricao(m.getCodigo() + " · " + m.getLocalizacao() + " · " + (m.getVinhoNome() != null ? m.getVinhoNome() : "vinho"));
                ln.setLitrosPlaneados(lit);
                linhaRepo.save(ln);
            } catch (Exception ignored) { }
        }
    }

    /**
     * Um vinho a granel nunca pode ter o mesmo nome de outro, loteado ou nao —
     * na certificacao escolhe-se pelo nome do vinho. Devolve a mensagem de erro
     * ou null se o nome estiver livre.
     */
    private String nomeEmUso(String nome) {
        if (loteRepo.existsByNomeIgnoreCase(nome)) {
            return "Já existe um lote com o nome \"" + nome + "\". Os vinhos não podem ter nomes repetidos.";
        }
        for (PlaneamentoVinho w : planeamentoRepo.findAllByOrderByNomeVinhoAsc()) {
            if (w.getNomeVinho() != null && w.getNomeVinho().equalsIgnoreCase(nome)) {
                return "Já existe um vinho planeado com o nome \"" + nome + "\". O vinho do lote tem de ter um nome diferente.";
            }
        }
        for (Mosto m : mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL)) {
            if (m.getVinhoNome() != null && m.getVinhoNome().equalsIgnoreCase(nome)) {
                return "Já existe vinho a granel com o nome \"" + nome + "\". O vinho do lote tem de ter um nome diferente.";
            }
        }
        return null;
    }

    /** Vinhos a granel por adega (todos os vinhos com todos os depositos). */
    private Map<Long, List<Map<String, Object>>> granelPorAdega() {
        Map<Long, PlaneamentoVinho> moagemPlano = planoPorMoagem();
        Map<Long, List<Map<String, Object>>> out = new LinkedHashMap<>();
        for (Mosto m : mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL)) {
            Long adegaId = adegaDe(m);
            if (adegaId == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("vinho", nomeVinho(m, moagemPlano));
            row.put("label", m.getCodigo() + " · " + m.getLocalizacao() + " · "
                    + (nomeVinho(m, moagemPlano) != null ? nomeVinho(m, moagemPlano) : "vinho") + " · "
                    + (m.getLitros() == null ? "0" : m.getLitros().toPlainString()) + " L");
            out.computeIfAbsent(adegaId, k -> new ArrayList<>()).add(row);
        }
        return out;
    }

    /** Origens possiveis: o vinho a granel da adega, menos o proprio vinho do lote. */
    private List<Map<String, Object>> granelDaAdega(Long adegaId, String excluirLoteCodigo) {
        Map<Long, PlaneamentoVinho> moagemPlano = planoPorMoagem();
        List<Map<String, Object>> out = new ArrayList<>();
        if (adegaId == null) return out;
        for (Mosto m : mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL)) {
            if (!adegaId.equals(adegaDe(m))) continue;
            if (excluirLoteCodigo != null && excluirLoteCodigo.equals(m.getLoteCodigo())) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("label", m.getCodigo() + " · " + m.getLocalizacao() + " · "
                    + (nomeVinho(m, moagemPlano) != null ? nomeVinho(m, moagemPlano) : "vinho") + " · "
                    + (m.getLitros() == null ? "0" : m.getLitros().toPlainString()) + " L");
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> recipientesDaAdega(Long adegaId) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (adegaId == null) return out;
        talhaRepo.findAllByOrderByIdentificacaoAsc().forEach(t -> {
            if (t.getAdega() == null || !adegaId.equals(t.getAdega().getId())) return;
            out.add(recipiente("TALHA:" + t.getId(), "Talha " + t.getIdentificacao(), t.getCapacidadeLitros(), t.getVolumeAtualLitros()));
        });
        depositoRepo.findAllByOrderByIdentificacaoAsc().forEach(d -> {
            if (d.getAdega() == null || !adegaId.equals(d.getAdega().getId())) return;
            out.add(recipiente("DEPOSITO:" + d.getId(), "Depósito " + d.getIdentificacao(), d.getCapacidadeLitros(), d.getVolumeAtualLitros()));
        });
        return out;
    }

    private Map<String, Object> recipiente(String ref, String ident, BigDecimal cap, BigDecimal vol) {
        BigDecimal vv = vol == null ? BigDecimal.ZERO : vol;
        String capTxt = cap == null ? " (sem cap.)" : " (" + vv.toPlainString() + "/" + cap.toPlainString() + " L)";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ref", ref);
        m.put("label", ident + capTxt);
        return m;
    }

    private Map<Long, PlaneamentoVinho> planoPorMoagem() {
        Map<Long, PlaneamentoVinho> map = new HashMap<>();
        for (ProcessoMoagem mo : moagemRepo.findAll()) {
            if (mo.getPlano() != null) map.put(mo.getId(), mo.getPlano());
        }
        return map;
    }

    private Long adegaDe(Mosto m) {
        if (m.getTalha() != null && m.getTalha().getAdega() != null) return m.getTalha().getAdega().getId();
        if (m.getDeposito() != null && m.getDeposito().getAdega() != null) return m.getDeposito().getAdega().getId();
        return null;
    }

    private String nomeVinho(Mosto m, Map<Long, PlaneamentoVinho> moagemPlano) {
        if (m.getVinhoNome() != null && !m.getVinhoNome().isBlank()) return m.getVinhoNome();
        PlaneamentoVinho w = m.getOrigemMoagemId() != null ? moagemPlano.get(m.getOrigemMoagemId()) : null;
        return w != null ? w.getNomeVinho() : null;
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }
}
