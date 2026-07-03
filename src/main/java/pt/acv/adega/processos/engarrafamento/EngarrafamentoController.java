package pt.acv.adega.processos.engarrafamento;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.ConsumivelRepository;
import pt.acv.adega.fichas.TipoConsumivel;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.MostoRepository;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/processos/engarrafamento")
public class EngarrafamentoController {

    private final ProcessoEngarrafamentoRepository repo;
    private final EngarrafamentoService service;
    private final MostoRepository mostoRepo;
    private final ConsumivelRepository consumivelRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final CodigoService codigoService;

    public EngarrafamentoController(ProcessoEngarrafamentoRepository repo, EngarrafamentoService service,
                                    MostoRepository mostoRepo, ConsumivelRepository consumivelRepo,
                                    TrabalhadorRepository trabalhadorRepo, VinhoEngarrafadoRepository engarrafadoRepo,
                                    CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.mostoRepo = mostoRepo;
        this.consumivelRepo = consumivelRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/engarrafamento/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        ProcessoEngarrafamento p = new ProcessoEngarrafamento();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("engarrafamento", p);
        preencherOpcoes(model);
        return "processos/engarrafamento/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoEngarrafamento p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/engarrafamento"; }
        model.addAttribute("engarrafamento", p);
        model.addAttribute("engarrafados", engarrafadoRepo.findByOrigemEngarrafamentoId(p.getId()));
        return "processos/engarrafamento/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoEngarrafamento p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/engarrafamento"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/engarrafamento/" + id; }
        model.addAttribute("engarrafamento", p);
        preencherOpcoes(model);
        return "processos/engarrafamento/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("engarrafamento") ProcessoEngarrafamento eng, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/engarrafamento/form";
        }
        if (eng.getId() == null) {
            eng.setCodigo(codigoService.proximoCodigo(ProcessoEngarrafamento.PREFIXO));
            eng.setCriadoPor(auth.getName());
        } else {
            ProcessoEngarrafamento existente = repo.findById(eng.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/engarrafamento";
            }
            eng.setCriadoPor(existente.getCriadoPor());
            eng.setEstado(existente.getEstado());
            eng.setDataFecho(existente.getDataFecho());
        }
        repo.save(eng);
        ra.addFlashAttribute("sucesso", "Engarrafamento guardado: " + eng.getCodigo());
        return "redirect:/processos/engarrafamento/" + eng.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoEngarrafamento p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/engarrafamento"; }
        try {
            service.fechar(id);
            ra.addFlashAttribute("sucesso", "Engarrafamento fechado. Baixa de vinho, garrafas e rolhas; vinho engarrafado criado.");
        } catch (EngarrafamentoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/engarrafamento/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/engarrafamento/" + id; }
        try {
            service.reabrir(id);
            ra.addFlashAttribute("sucesso", "Engarrafamento reaberto. Vinho, garrafas e rolhas repostos; engarrafado anulado.");
        } catch (EngarrafamentoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/engarrafamento/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoEngarrafamento p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/engarrafamento"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra o processo antes de o eliminar (para repor stocks/vinho)."); return "redirect:/processos/engarrafamento/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Engarrafamento eliminado.");
        return "redirect:/processos/engarrafamento";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("vinhosGranel", mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL));
        model.addAttribute("garrafas", consumivelRepo.findByTipoOrderByDescricaoAsc(TipoConsumivel.GARRAFA));
        model.addAttribute("rolhas", consumivelRepo.findByTipoOrderByDescricaoAsc(TipoConsumivel.ROLHA));
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoEngarrafamento p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
