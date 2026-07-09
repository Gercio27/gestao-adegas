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
    private final MostoRepository mostoRepo;
    private final CodigoService codigoService;

    public MoagemController(ProcessoMoagemRepository repo, MoagemService moagemService,
                            TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                            CastaRepository castaRepo, AdegaRepository adegaRepo,
                            TrabalhadorRepository trabalhadorRepo, LinhaPlaneamentoParcelaRepository linhaRepo,
                            PlaneamentoVinhoRepository planeamentoRepo, EnchimentoRepository enchimentoRepo,
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
        this.mostoRepo = mostoRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String folha(Model model) {
        // Vindimas disponíveis (para o seletor por adega + vinho, filtrado no cliente).
        List<Map<String, Object>> vindimas = new ArrayList<>();
        for (PlaneamentoVinho p : planeamentoRepo.findAllByOrderByNomeVinhoAsc()) {
            for (LinhaPlaneamentoParcela l : p.getLinhas()) {
                if (l.getTotalVindimadoKg().signum() <= 0 || l.getAdegaEntrega() == null) continue;
                String parc = l.getParcela() != null
                        ? (l.getParcela().getNome() != null ? l.getParcela().getNome() : l.getParcela().getIdentificacao())
                        : "?";
                String casta = (l.getParcela() != null && l.getParcela().getCasta() != null) ? l.getParcela().getCasta().getNome() : "—";
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", l.getId());
                m.put("adegaId", l.getAdegaEntrega().getId());
                m.put("planoId", p.getId());
                m.put("label", parc + " (" + casta + ")");
                m.put("vindimado", l.getTotalVindimadoKg().toPlainString());
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
        appendEnchimentos(m, form.getEnchimentos());
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
        appendEnchimentos(m, form.getEnchimentos());
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

    private void appendEnchimentos(ProcessoMoagem m, List<Enchimento> lista) {
        if (lista == null) return;
        for (Enchimento e : lista) {
            if (e == null) continue;
            boolean semRecipiente = e.getRecipienteRef() == null || e.getRecipienteRef().isBlank();
            if (semRecipiente && e.getQuantidadeMoidaKg() == null && e.getLitros() == null) continue;
            e.setId(null);
            resolverRecipiente(e);
            if (e.getCasta() != null && e.getCasta().getId() != null) {
                castaRepo.findById(e.getCasta().getId()).ifPresent(e::setCasta);
            } else {
                e.setCasta(null);
            }
            e.setMoagem(m);
            m.getEnchimentos().add(e);
        }
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
