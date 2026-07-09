package pt.acv.adega.processos.moagem;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
@RequestMapping("/processos/moagem")
public class MoagemController {

    private final ProcessoMoagemRepository repo;
    private final MoagemService moagemService;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final CastaRepository castaRepo;
    private final AdegaRepository adegaRepo;
    private final VinhaRepository vinhaRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final LinhaPlaneamentoParcelaRepository linhaRepo;
    private final PlaneamentoVinhoRepository planeamentoRepo;
    private final EnchimentoRepository enchimentoRepo;
    private final MostoRepository mostoRepo;
    private final CodigoService codigoService;

    public MoagemController(ProcessoMoagemRepository repo, MoagemService moagemService,
                            TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                            CastaRepository castaRepo, AdegaRepository adegaRepo, VinhaRepository vinhaRepo,
                            TrabalhadorRepository trabalhadorRepo, LinhaPlaneamentoParcelaRepository linhaRepo,
                            PlaneamentoVinhoRepository planeamentoRepo, EnchimentoRepository enchimentoRepo,
                            MostoRepository mostoRepo, CodigoService codigoService) {
        this.repo = repo;
        this.moagemService = moagemService;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.castaRepo = castaRepo;
        this.adegaRepo = adegaRepo;
        this.vinhaRepo = vinhaRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.linhaRepo = linhaRepo;
        this.planeamentoRepo = planeamentoRepo;
        this.enchimentoRepo = enchimentoRepo;
        this.mostoRepo = mostoRepo;
        this.codigoService = codigoService;
    }

    /** Fase 3 — Folha da moagem sobre as parcelas já vindimadas. */
    @GetMapping
    public String folha(Model model) {
        List<MoagemVinhoView> vinhos = new ArrayList<>();
        for (PlaneamentoVinho p : planeamentoRepo.findAllByOrderByNomeVinhoAsc()) {
            List<MoagemLinhaView> linhas = new ArrayList<>();
            for (LinhaPlaneamentoParcela l : p.getLinhas()) {
                if (l.getTotalVindimadoKg().compareTo(BigDecimal.ZERO) <= 0) continue; // só vindimadas
                ProcessoMoagem m = repo.findFirstByOrigemVindimaId(l.getId()).orElse(null);
                List<Mosto> mostos = (m != null && !m.isAberto()) ? mostoRepo.findByOrigemMoagemId(m.getId()) : List.of();
                linhas.add(new MoagemLinhaView(l, m, mostos));
            }
            if (!linhas.isEmpty()) vinhos.add(new MoagemVinhoView(p, linhas));
        }
        model.addAttribute("vinhos", vinhos);
        model.addAttribute("recipientes", recipienteOpcoes());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        return "processos/moagem/folha";
    }

    /** Cria/atualiza a moagem de uma linha vindimada e ACRESCENTA enchimentos. */
    @PostMapping("/linha/{lineId}")
    @Transactional
    public String guardarLinha(@PathVariable Long lineId, @ModelAttribute MoagemLinhaForm form,
                               Authentication auth, RedirectAttributes ra) {
        LinhaPlaneamentoParcela linha = linhaRepo.findById(lineId).orElse(null);
        if (linha == null) { ra.addFlashAttribute("erro", "Linha vindimada não encontrada."); return "redirect:/processos/moagem"; }

        ProcessoMoagem m = repo.findFirstByOrigemVindimaId(lineId).orElse(null);
        if (m == null) {
            m = new ProcessoMoagem();
            m.setCodigo(codigoService.proximoCodigo(ProcessoMoagem.PREFIXO));
            m.setCriadoPor(auth.getName());
            m.setOrigemVindima(linha);
            if (linha.getParcela() != null) m.setVinha(linha.getParcela().getVinha());
        } else if (!m.isAberto()) {
            ra.addFlashAttribute("erro", "Moagem fechada — reabra antes de alterar.");
            return "redirect:/processos/moagem";
        }

        m.setResponsavel(form.getResponsavel());
        m.setAdega(form.getAdega());
        if (form.getDataInicio() != null) m.setDataHoraInicio(form.getDataInicio().atStartOfDay());
        else if (m.getDataHoraInicio() == null) m.setDataHoraInicio(LocalDateTime.now());
        if (form.getDataFim() != null) m.setDataHoraFim(form.getDataFim().atStartOfDay());

        Casta castaDaVindima = linha.getParcela() != null ? linha.getParcela().getCasta() : null;
        int novos = 0;
        for (Enchimento e : form.getEnchimentos()) {
            if (e == null) continue;
            boolean semRecipiente = e.getRecipienteRef() == null || e.getRecipienteRef().isBlank();
            if (semRecipiente && e.getQuantidadeMoidaKg() == null && e.getLitros() == null) continue;
            e.setId(null);
            resolverRecipiente(e);
            e.setCasta(castaDaVindima); // casta vem sempre da vindima
            e.setMoagem(m);
            m.getEnchimentos().add(e);
            novos++;
        }
        repo.save(m);
        ra.addFlashAttribute("sucesso", novos > 0
                ? "Moagem guardada (+" + novos + " enchimento" + (novos == 1 ? "" : "s") + ")."
                : "Dados da moagem atualizados.");
        return "redirect:/processos/moagem";
    }

    /** Remove um enchimento de uma moagem ainda aberta (correções). */
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

    /** Fecha a moagem a partir da folha (gera mostos e soma volumes). */
    @PostMapping("/linha-moagem/{id}/fechar")
    public String fecharFolha(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/moagem"; }
        try {
            moagemService.fechar(id);
            ra.addFlashAttribute("sucesso", "Moagem fechada. Fichas de mosto geradas.");
        } catch (MoagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/moagem";
    }

    /** Reabre a moagem a partir da folha (admin): anula mostos e repõe volumes. */
    @PostMapping("/linha-moagem/{id}/reabrir")
    public String reabrirFolha(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/moagem"; }
        try {
            moagemService.reabrir(id);
            ra.addFlashAttribute("sucesso", "Moagem reaberta. Mostos anulados e volumes repostos.");
        } catch (MoagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/moagem";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoMoagem m = new ProcessoMoagem();
        m.setDataHoraInicio(LocalDateTime.now());
        m.getEnchimentos().add(new Enchimento());
        model.addAttribute("moagem", m);
        preencherOpcoes(model);
        return "processos/moagem/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/moagem"; }
        model.addAttribute("moagem", m);
        model.addAttribute("mostos", mostoRepo.findByOrigemMoagemId(m.getId()));
        return "processos/moagem/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/moagem"; }
        if (!m.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/moagem/" + id; }
        model.addAttribute("moagem", m);
        preencherOpcoes(model);
        return "processos/moagem/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("moagem") ProcessoMoagem moagem, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        limparEnchimentosVazios(moagem);
        resolverEnchimentos(moagem);
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/moagem/form";
        }
        if (moagem.getId() == null) {
            moagem.setCodigo(codigoService.proximoCodigo(ProcessoMoagem.PREFIXO));
            moagem.setCriadoPor(auth.getName());
        } else {
            ProcessoMoagem existente = repo.findById(moagem.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/moagem";
            }
            moagem.setCriadoPor(existente.getCriadoPor());
            moagem.setEstado(existente.getEstado());
            moagem.setDataFecho(existente.getDataFecho());
        }
        repo.save(moagem);
        ra.addFlashAttribute("sucesso", "Moagem guardada: " + moagem.getCodigo());
        return "redirect:/processos/moagem/" + moagem.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/moagem"; }
        try {
            moagemService.fechar(id);
            ra.addFlashAttribute("sucesso", "Moagem fechada. Fichas de mosto geradas automaticamente.");
        } catch (MoagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/moagem/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/moagem/" + id; }
        try {
            moagemService.reabrir(id);
            ra.addFlashAttribute("sucesso", "Moagem reaberta. Mostos gerados foram anulados e os volumes repostos.");
        } catch (MoagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/moagem/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/moagem"; }
        if (!m.isAberto()) { ra.addFlashAttribute("erro", "Reabra o processo antes de o eliminar (para repor os mostos/volumes)."); return "redirect:/processos/moagem/" + id; }
        repo.delete(m);
        ra.addFlashAttribute("sucesso", "Moagem eliminada.");
        return "redirect:/processos/moagem";
    }

    // ----- auxiliares -----

    private void limparEnchimentosVazios(ProcessoMoagem m) {
        Iterator<Enchimento> it = m.getEnchimentos().iterator();
        while (it.hasNext()) {
            Enchimento e = it.next();
            boolean semRecipiente = e.getRecipienteRef() == null || e.getRecipienteRef().isBlank();
            boolean semLitros = e.getLitros() == null;
            boolean semCasta = e.getCasta() == null || e.getCasta().getId() == null;
            if (semRecipiente && semLitros && semCasta) it.remove();
        }
    }

    /** Resolve recipienteRef ("TALHA:id"/"DEPOSITO:id") e castas para entidades geridas. */
    private void resolverEnchimentos(ProcessoMoagem m) {
        for (Enchimento e : m.getEnchimentos()) {
            e.setMoagem(m);
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
            if (e.getCasta() != null && e.getCasta().getId() != null) {
                castaRepo.findById(e.getCasta().getId()).ifPresent(e::setCasta);
            } else {
                e.setCasta(null);
            }
        }
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("recipientes", recipienteOpcoes());
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("vinhas", vinhaRepo.findAllByOrderByNomeAsc());
        // Origem da uva: linhas de planeamento ja vindimadas (com Kg colhidos).
        List<LinhaPlaneamentoParcela> vindimadas = new ArrayList<>();
        for (LinhaPlaneamentoParcela l : linhaRepo.findAll()) {
            if (l.getTotalVindimadoKg().compareTo(BigDecimal.ZERO) > 0) vindimadas.add(l);
        }
        vindimadas.sort(java.util.Comparator.comparing(LinhaPlaneamentoParcela::getEtiqueta, String.CASE_INSENSITIVE_ORDER));
        model.addAttribute("vindimadas", vindimadas);
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    /** Opções de recipiente (talhas + depósitos), com marca de "cheio". */
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

    /** Resolve o recipienteRef ("TALHA:id"/"DEPOSITO:id") para talha/deposito. */
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

    private String capacidadeTxt(java.math.BigDecimal cap, java.math.BigDecimal vol) {
        if (cap == null) return " (sem capacidade definida)";
        java.math.BigDecimal v = vol == null ? java.math.BigDecimal.ZERO : vol;
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
