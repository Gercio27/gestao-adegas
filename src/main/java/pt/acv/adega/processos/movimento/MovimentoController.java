package pt.acv.adega.processos.movimento;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.CastaRepository;
import pt.acv.adega.fichas.RecipienteService;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.MostoRepository;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/processos/movimento-mosto")
public class MovimentoController {

    private final ProcessoMovimentoMostoRepository repo;
    private final MovimentoService service;
    private final RecipienteService recipienteService;
    private final CastaRepository castaRepo;
    private final MostoRepository mostoRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public MovimentoController(ProcessoMovimentoMostoRepository repo, MovimentoService service,
                              RecipienteService recipienteService, CastaRepository castaRepo,
                              MostoRepository mostoRepo, TrabalhadorRepository trabalhadorRepo,
                              CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.recipienteService = recipienteService;
        this.castaRepo = castaRepo;
        this.mostoRepo = mostoRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/movimento/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        ProcessoMovimentoMosto p = new ProcessoMovimentoMosto();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("mov", p);
        preencherOpcoes(model);
        return "processos/movimento/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoMovimentoMosto p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-mosto"; }
        model.addAttribute("mov", p);
        return "processos/movimento/detalhe";
    }

    @GetMapping("/{id}/da")
    public String da(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoMovimentoMosto p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-mosto"; }
        if (p.getNumeroDA() == null) { ra.addFlashAttribute("erro", "O DA só é emitido ao fechar o processo."); return "redirect:/processos/movimento-mosto/" + id; }
        model.addAttribute("mov", p);
        return "processos/movimento/da";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoMovimentoMosto p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-mosto"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/movimento-mosto/" + id; }
        model.addAttribute("mov", p);
        preencherOpcoes(model);
        return "processos/movimento/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("mov") ProcessoMovimentoMosto mov, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (mov.getTipo() == TipoMovimento.ENTRADA) {
            RecipienteService.Recipiente r = recipienteService.resolver(mov.getDestinoRef());
            mov.setTalhaDestino(r.talha());
            mov.setDepositoDestino(r.deposito());
            mov.setMostoOrigem(null);
        } else {
            mov.setTalhaDestino(null);
            mov.setDepositoDestino(null);
            mov.setCasta(null);
        }
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/movimento/form";
        }
        if (mov.getId() == null) {
            mov.setCodigo(codigoService.proximoCodigo(ProcessoMovimentoMosto.PREFIXO));
            mov.setCriadoPor(auth.getName());
        } else {
            ProcessoMovimentoMosto existente = repo.findById(mov.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/movimento-mosto";
            }
            mov.setCriadoPor(existente.getCriadoPor());
            mov.setEstado(existente.getEstado());
            mov.setDataFecho(existente.getDataFecho());
            mov.setNumeroDA(existente.getNumeroDA());
        }
        repo.save(mov);
        ra.addFlashAttribute("sucesso", "Movimento guardado: " + mov.getCodigo());
        return "redirect:/processos/movimento-mosto/" + mov.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMovimentoMosto p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-mosto"; }
        try {
            service.fechar(id);
            ra.addFlashAttribute("sucesso", "Movimento fechado. DA emitido.");
        } catch (MovimentoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/movimento-mosto/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/movimento-mosto/" + id; }
        try {
            service.reabrir(id);
            ra.addFlashAttribute("sucesso", "Movimento reaberto. Efeitos revertidos.");
        } catch (MovimentoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/movimento-mosto/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMovimentoMosto p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/movimento-mosto"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra antes de eliminar (para reverter os efeitos)."); return "redirect:/processos/movimento-mosto/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Movimento eliminado.");
        return "redirect:/processos/movimento-mosto";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("tipos", TipoMovimento.values());
        model.addAttribute("recipientes", recipienteService.opcoes());
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("mostos", mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.EM_FERMENTACAO));
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoMovimentoMosto p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
