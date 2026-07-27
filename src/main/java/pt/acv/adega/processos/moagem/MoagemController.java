package pt.acv.adega.processos.moagem;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.*;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcela;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcelaRepository;
import pt.acv.adega.planeamento.PlaneamentoVinho;
import pt.acv.adega.planeamento.PlaneamentoVinhoRepository;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Fase 3 — Moagem. Uma moagem é feita por adega + vinho e mói uma ou mais
 * vindimas (parcelas colhidas) desse vinho entregues nessa adega.
 */
@Controller
@RequestMapping("/processos/moagem")
public class MoagemController {

    private final ProcessoMoagemRepository repo;
    private final MoagemService moagemService;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final CastaRepository castaRepo;
    private final AdegaRepository adegaRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final LinhaPlaneamentoParcelaRepository linhaRepo;
    private final PlaneamentoVinhoRepository planeamentoRepo;
    private final EnchimentoRepository enchimentoRepo;
    private final EnchimentoVindimaRepository enchimentoVindimaRepo;
    private final MostoRepository mostoRepo;
    private final CodigoService codigoService;

    public MoagemController(ProcessoMoagemRepository repo, MoagemService moagemService,
                            TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                            CastaRepository castaRepo, AdegaRepository adegaRepo,
                            TrabalhadorRepository trabalhadorRepo, LinhaPlaneamentoParcelaRepository linhaRepo,
                            PlaneamentoVinhoRepository planeamentoRepo, EnchimentoRepository enchimentoRepo,
                            EnchimentoVindimaRepository enchimentoVindimaRepo,
                            MostoRepository mostoRepo, CodigoService codigoService) {
        this.repo = repo;
        this.moagemService = moagemService;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.castaRepo = castaRepo;
        this.adegaRepo = adegaRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.linhaRepo = linhaRepo;
        this.planeamentoRepo = planeamentoRepo;
        this.enchimentoRepo = enchimentoRepo;
        this.enchimentoVindimaRepo = enchimentoVindimaRepo;
        this.mostoRepo = mostoRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String folha(Model model) {
        // Vindimas disponíveis (para o seletor por adega + vinho, filtrado no cliente).
        // Cada uma leva já o saldo por moer: o que foi vindimado menos o que
        // outras moagens (mesmo abertas) já lhe tiraram.
        Map<Long, BigDecimal> usado = kgUsadoPorVindima();
        List<Map<String, Object>> vindimas = new ArrayList<>();
        for (PlaneamentoVinho p : planeamentoRepo.findAllByOrderByNomeVinhoAsc()) {
            for (LinhaPlaneamentoParcela l : p.getLinhas()) {
                if (l.getTotalVindimadoKg().signum() <= 0 || l.getAdegaEntrega() == null) continue;
                String parc = l.getParcela() != null
                        ? (l.getParcela().getNome() != null ? l.getParcela().getNome() : l.getParcela().getIdentificacao())
                        : "?";
                String casta = (l.getParcela() != null && l.getParcela().getCasta() != null) ? l.getParcela().getCasta().getNome() : "—";
                BigDecimal moido = usado.getOrDefault(l.getId(), BigDecimal.ZERO);
                BigDecimal disponivel = l.getTotalVindimadoKg().subtract(moido);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", l.getId());
                m.put("adegaId", l.getAdegaEntrega().getId());
                m.put("planoId", p.getId());
                m.put("label", parc + " (" + casta + ")");
                m.put("casta", casta);
                m.put("vindimado", l.getTotalVindimadoKg().toPlainString());
                m.put("moido", moido.toPlainString());
                m.put("disponivel", disponivel.toPlainString());
                vindimas.add(m);
            }
        }
        model.addAttribute("vindimasDisponiveis", vindimas);
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("planos", planeamentoRepo.findAllByOrderByNomeVinhoAsc());
        model.addAttribute("recipientes", recipienteOpcoes());
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());

        List<ProcessoMoagem> moagens = repo.findAllByOrderByDataCriacaoDesc();
        model.addAttribute("moagens", moagens);
        Map<Long, List<Mosto>> mostosPorMoagem = new HashMap<>();
        for (ProcessoMoagem mo : moagens) {
            if (!mo.isAberto()) mostosPorMoagem.put(mo.getId(), mostoRepo.findByOrigemMoagemId(mo.getId()));
        }
        model.addAttribute("mostosPorMoagem", mostosPorMoagem);

        // Vindimas de cada moagem aberta, com o saldo por moer — para o
        // formulário de acrescentar enchimentos repartir os Kg por vindima.
        Map<Long, List<Map<String, Object>>> vindimasPorMoagem = new HashMap<>();
        for (ProcessoMoagem mo : moagens) {
            if (!mo.isAberto()) continue;
            List<Map<String, Object>> linhas = new ArrayList<>();
            for (LinhaPlaneamentoParcela l : mo.getVindimas()) {
                BigDecimal moido = usado.getOrDefault(l.getId(), BigDecimal.ZERO);
                Map<String, Object> v = new LinkedHashMap<>();
                v.put("id", l.getId());
                v.put("label", l.getEtiqueta());
                v.put("casta", l.getParcela() != null && l.getParcela().getCasta() != null
                        ? l.getParcela().getCasta().getNome() : "—");
                v.put("disponivel", l.getTotalVindimadoKg().subtract(moido).toPlainString());
                linhas.add(v);
            }
            vindimasPorMoagem.put(mo.getId(), linhas);
        }
        model.addAttribute("vindimasPorMoagem", vindimasPorMoagem);
        return "processos/moagem/folha";
    }

    @PostMapping("/nova")
    @Transactional
    public String criar(@ModelAttribute MoagemForm form, Authentication auth, RedirectAttributes ra) {
        if (form.getAdega() == null || form.getPlano() == null) {
            ra.addFlashAttribute("erro", "Escolha a adega e o vinho.");
            return "redirect:/processos/moagem";
        }
        ProcessoMoagem m = new ProcessoMoagem();
        m.setCodigo(codigoService.proximoCodigo(ProcessoMoagem.PREFIXO));
        m.setCriadoPor(auth.getName());
        m.setAdega(form.getAdega());
        m.setPlano(form.getPlano());
        m.setResponsavel(form.getResponsavel());
        m.setDataHoraInicio(form.getDataInicio() != null ? form.getDataInicio().atStartOfDay() : LocalDateTime.now());
        if (form.getDataFim() != null) m.setDataHoraFim(form.getDataFim().atStartOfDay());
        if (form.getVindimaIds() != null) {
            for (Long lid : form.getVindimaIds()) {
                linhaRepo.findById(lid).ifPresent(m.getVindimas()::add);
            }
        }
        String erro = appendEnchimentos(m, form.getEnchimentos());
        if (erro != null) {
            // Nada foi gravado — a moagem ainda nem existe na base de dados.
            ra.addFlashAttribute("erro", erro);
            return "redirect:/processos/moagem";
        }
        repo.save(m);
        ra.addFlashAttribute("sucesso", "Moagem criada: " + m.getCodigo());
        return "redirect:/processos/moagem";
    }

    @PostMapping("/{id}/enchimentos")
    @Transactional
    public String adicionarEnchimentos(@PathVariable Long id, @ModelAttribute MoagemForm form,
                                       Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a esta moagem."); return "redirect:/processos/moagem"; }
        if (!m.isAberto()) { ra.addFlashAttribute("erro", "Moagem fechada — reabra antes de alterar."); return "redirect:/processos/moagem"; }
        if (form.getResponsavel() != null) m.setResponsavel(form.getResponsavel());
        String erro = appendEnchimentos(m, form.getEnchimentos());
        if (erro != null) { ra.addFlashAttribute("erro", erro); return "redirect:/processos/moagem"; }
        repo.save(m);
        ra.addFlashAttribute("sucesso", "Enchimentos guardados.");
        return "redirect:/processos/moagem";
    }

    @PostMapping("/enchimento/{id}/eliminar")
    @Transactional
    public String eliminarEnchimento(@PathVariable Long id, RedirectAttributes ra) {
        Enchimento e = enchimentoRepo.findById(id).orElse(null);
        if (e == null) { ra.addFlashAttribute("erro", "Enchimento não encontrado."); return "redirect:/processos/moagem"; }
        if (e.getMoagem() != null && !e.getMoagem().isAberto()) {
            ra.addFlashAttribute("erro", "Moagem fechada — reabra antes de remover.");
            return "redirect:/processos/moagem";
        }
        enchimentoRepo.delete(e);
        ra.addFlashAttribute("sucesso", "Enchimento removido.");
        return "redirect:/processos/moagem";
    }

    /** Inicia uma nova moagem para a uva que ainda faltou moer (sobra) desta. */
    @PostMapping("/{id}/nova-sobra")
    @Transactional
    public String novaSobra(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem orig = repo.findById(id).orElse(null);
        if (orig == null || !podeAceder(orig, auth)) { ra.addFlashAttribute("erro", "Sem acesso a esta moagem."); return "redirect:/processos/moagem"; }
        BigDecimal sobra = orig.getSobraPorMoerKg();
        if (sobra.signum() <= 0) { ra.addFlashAttribute("erro", "Esta moagem não tem sobra por moer."); return "redirect:/processos/moagem"; }

        ProcessoMoagem nova = new ProcessoMoagem();
        nova.setCodigo(codigoService.proximoCodigo(ProcessoMoagem.PREFIXO));
        nova.setCriadoPor(auth.getName());
        nova.setAdega(orig.getAdega());
        nova.setPlano(orig.getPlano());
        nova.setResponsavel(orig.getResponsavel());
        nova.getVindimas().addAll(orig.getVindimas());
        nova.setObjetivoKgManual(sobra); // só falta moer esta quantidade
        nova.setDataHoraInicio(LocalDateTime.now());
        repo.save(nova);
        ra.addFlashAttribute("sucesso", "Nova moagem criada para a sobra (" + sobra.toPlainString() + " kg): " + nova.getCodigo());
        return "redirect:/processos/moagem";
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a esta moagem."); return "redirect:/processos/moagem"; }
        try {
            moagemService.fechar(id);
            ra.addFlashAttribute("sucesso", "Moagem fechada. Fichas de mosto geradas.");
        } catch (MoagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/moagem";
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/moagem"; }
        try {
            moagemService.reabrir(id);
            ra.addFlashAttribute("sucesso", "Moagem reaberta. Mostos anulados e volumes repostos.");
        } catch (MoagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/moagem";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a esta moagem."); return "redirect:/processos/moagem"; }
        if (!m.isAberto()) { ra.addFlashAttribute("erro", "Reabra a moagem antes de a eliminar (para repor os mostos/volumes)."); return "redirect:/processos/moagem"; }
        repo.delete(m);
        ra.addFlashAttribute("sucesso", "Moagem eliminada.");
        return "redirect:/processos/moagem";
    }

    // ----- auxiliares -----

    /**
     * Acrescenta os enchimentos vindos do formulário. Devolve uma mensagem de
     * erro se algum deles quiser moer mais Kg do que a vindima ainda tem — não
     * grava nada nesse caso (o método é chamado dentro de @Transactional).
     */
    private String appendEnchimentos(ProcessoMoagem m, List<Enchimento> lista) {
        if (lista == null) return null;
        // Saldo por vindima já comprometido noutras moagens.
        Map<Long, BigDecimal> usado = kgUsadoPorVindima();
        List<Enchimento> aceites = new ArrayList<>();
        for (Enchimento e : lista) {
            if (e == null) continue;
            resolverOrigens(e);
            boolean semRecipiente = e.getRecipienteRef() == null || e.getRecipienteRef().isBlank();
            boolean semNada = e.getQuantidadeMoidaKg() == null && e.getLitros() == null
                    && e.getTotalOrigensKg().signum() == 0;
            if (semRecipiente && semNada) continue;
            e.setId(null);
            resolverRecipiente(e);
            resolverCastas(e);
            // Com vindimas indicadas, os Kg moídos são a soma delas.
            if (!e.getOrigens().isEmpty()) e.setQuantidadeMoidaKg(e.getTotalOrigensKg());

            String erro = validarSaldos(e, usado);
            if (erro != null) return erro;   // ainda não se tocou na moagem
            aceites.add(e);
        }
        // Só depois de tudo validado é que se mexe na moagem.
        for (Enchimento e : aceites) {
            e.setMoagem(m);
            m.getEnchimentos().add(e);
        }
        return null;
    }

    /** Confirma que cada vindima ainda tem Kg suficientes e vai descontando. */
    private String validarSaldos(Enchimento e, Map<Long, BigDecimal> usado) {
        for (EnchimentoVindima o : e.getOrigens()) {
            if (o.getLinha() == null || o.getQuantidadeKg() == null || o.getQuantidadeKg().signum() <= 0) continue;
            Long lid = o.getLinha().getId();
            BigDecimal total = o.getLinha().getTotalVindimadoKg();
            BigDecimal jaUsado = usado.getOrDefault(lid, BigDecimal.ZERO);
            BigDecimal disponivel = total.subtract(jaUsado);
            if (o.getQuantidadeKg().compareTo(disponivel) > 0) {
                return String.format("%s só tem %s kg por moer — não pode moer %s kg.",
                        o.getLinha().getEtiqueta(), disponivel.toPlainString(), o.getQuantidadeKg().toPlainString());
            }
            usado.put(lid, jaUsado.add(o.getQuantidadeKg()));
        }
        return null;
    }

    /** Resolve os ids de vindima do formulário e deita fora as linhas sem Kg. */
    private void resolverOrigens(Enchimento e) {
        List<EnchimentoVindima> validas = new ArrayList<>();
        if (e.getOrigens() != null) {
            for (EnchimentoVindima o : e.getOrigens()) {
                if (o == null || o.getQuantidadeKg() == null || o.getQuantidadeKg().signum() <= 0) continue;
                Long lid = o.getLinhaId();
                if (lid == null) continue;
                LinhaPlaneamentoParcela linha = linhaRepo.findById(lid).orElse(null);
                if (linha == null) continue;
                o.setId(null);
                o.setLinha(linha);
                o.setEnchimento(e);
                validas.add(o);
            }
        }
        e.setOrigens(validas);
    }

    /** Kg já atribuídos a moagens, por vindima (inclui as moagens ainda abertas). */
    private Map<Long, BigDecimal> kgUsadoPorVindima() {
        Map<Long, BigDecimal> out = new HashMap<>();
        for (Object[] linha : enchimentoVindimaRepo.totaisPorVindima()) {
            if (linha[0] == null) continue;
            out.put((Long) linha[0], linha[1] == null ? BigDecimal.ZERO : (BigDecimal) linha[1]);
        }
        return out;
    }

    private List<RecipienteOpcao> recipienteOpcoes() {
        List<RecipienteOpcao> recipientes = new ArrayList<>();
        talhaRepo.findAllByOrderByIdentificacaoAsc().forEach(t ->
                recipientes.add(new RecipienteOpcao("TALHA:" + t.getId(),
                        "Talha " + t.getIdentificacao() + capacidadeTxt(t.getCapacidadeLitros(), t.getVolumeAtualLitros()),
                        cheia(t.getCapacidadeLitros(), t.getVolumeAtualLitros()))));
        depositoRepo.findAllByOrderByIdentificacaoAsc().forEach(d ->
                recipientes.add(new RecipienteOpcao("DEPOSITO:" + d.getId(),
                        "Depósito " + d.getIdentificacao() + capacidadeTxt(d.getCapacidadeLitros(), d.getVolumeAtualLitros()),
                        cheia(d.getCapacidadeLitros(), d.getVolumeAtualLitros()))));
        return recipientes;
    }

    private boolean cheia(BigDecimal capacidade, BigDecimal volume) {
        if (capacidade == null) return false;
        BigDecimal v = volume == null ? BigDecimal.ZERO : volume;
        return v.compareTo(capacidade) >= 0;
    }

    /**
     * Define as castas do enchimento. Com vindimas indicadas, a casta vem da
     * parcela de cada vindima — não é escolhida à mão. Só quando não há
     * vindimas (ex.: moagens antigas) é que se usa o multi-select.
     */
    private void resolverCastas(Enchimento e) {
        List<Casta> castas = new ArrayList<>();
        if (!e.getOrigens().isEmpty()) {
            for (EnchimentoVindima o : e.getOrigens()) {
                Casta c = o.getCasta();
                if (c != null && castas.stream().noneMatch(x -> x.getId().equals(c.getId()))) castas.add(c);
            }
            e.setCastas(castas);
            e.setCasta(castas.isEmpty() ? null : castas.get(0));
            return;
        }
        List<Long> ids = e.getCastaIds();
        // Compatibilidade: se vier a casta única (binding antigo), usa o id dela.
        if ((ids == null || ids.isEmpty()) && e.getCasta() != null && e.getCasta().getId() != null) {
            ids = List.of(e.getCasta().getId());
        }
        if (ids != null) {
            for (Long cid : ids) {
                if (cid != null) castaRepo.findById(cid).ifPresent(castas::add);
            }
        }
        e.setCastas(castas);
        e.setCasta(castas.isEmpty() ? null : castas.get(0));
    }

    private void resolverRecipiente(Enchimento e) {
        e.setTalha(null);
        e.setDeposito(null);
        String ref = e.getRecipienteRef();
        if (ref != null && ref.contains(":")) {
            String[] partes = ref.split(":", 2);
            Long rid = parseLong(partes[1]);
            if (rid != null) {
                if ("TALHA".equals(partes[0])) talhaRepo.findById(rid).ifPresent(e::setTalha);
                else if ("DEPOSITO".equals(partes[0])) depositoRepo.findById(rid).ifPresent(e::setDeposito);
            }
        }
    }

    private String capacidadeTxt(BigDecimal cap, BigDecimal vol) {
        if (cap == null) return " (sem capacidade definida)";
        BigDecimal v = vol == null ? BigDecimal.ZERO : vol;
        return " (" + v.toPlainString() + "/" + cap.toPlainString() + " L)";
    }

    private Long parseLong(String s) {
        try { return Long.valueOf(s.trim()); } catch (Exception e) { return null; }
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoMoagem m, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(m.getCriadoPor());
    }
}
