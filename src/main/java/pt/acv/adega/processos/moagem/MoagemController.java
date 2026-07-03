package pt.acv.adega.processos.moagem;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.*;
import pt.acv.adega.processos.vindima.ProcessoVindimaRepository;
import pt.acv.adega.produtos.MostoRepository;

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
    private final ProcessoVindimaRepository vindimaRepo;
    private final MostoRepository mostoRepo;
    private final CodigoService codigoService;

    public MoagemController(ProcessoMoagemRepository repo, MoagemService moagemService,
                            TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                            CastaRepository castaRepo, AdegaRepository adegaRepo, VinhaRepository vinhaRepo,
                            TrabalhadorRepository trabalhadorRepo, ProcessoVindimaRepository vindimaRepo,
                            MostoRepository mostoRepo, CodigoService codigoService) {
        this.repo = repo;
        this.moagemService = moagemService;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.castaRepo = castaRepo;
        this.adegaRepo = adegaRepo;
        this.vinhaRepo = vinhaRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.vindimaRepo = vindimaRepo;
        this.mostoRepo = mostoRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/moagem/lista";
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
        List<RecipienteOpcao> recipientes = new ArrayList<>();
        talhaRepo.findAllByOrderByIdentificacaoAsc().forEach(t ->
                recipientes.add(new RecipienteOpcao("TALHA:" + t.getId(),
                        "Talha " + t.getIdentificacao() + capacidadeTxt(t.getCapacidadeLitros(), t.getVolumeAtualLitros()))));
        depositoRepo.findAllByOrderByIdentificacaoAsc().forEach(d ->
                recipientes.add(new RecipienteOpcao("DEPOSITO:" + d.getId(),
                        "Depósito " + d.getIdentificacao() + capacidadeTxt(d.getCapacidadeLitros(), d.getVolumeAtualLitros()))));
        model.addAttribute("recipientes", recipientes);
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("vinhas", vinhaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("vindimas", vindimaRepo.findAllByOrderByDataCriacaoDesc());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
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
