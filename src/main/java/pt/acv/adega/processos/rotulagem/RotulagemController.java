package pt.acv.adega.processos.rotulagem;

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
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/processos/rotulagem")
public class RotulagemController {

    private final ProcessoRotulagemRepository repo;
    private final RotulagemService service;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final ConsumivelRepository consumivelRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public RotulagemController(ProcessoRotulagemRepository repo, RotulagemService service,
                              VinhoEngarrafadoRepository engarrafadoRepo, ConsumivelRepository consumivelRepo,
                              TrabalhadorRepository trabalhadorRepo, CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.engarrafadoRepo = engarrafadoRepo;
        this.consumivelRepo = consumivelRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/rotulagem/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoRotulagem p = new ProcessoRotulagem();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("rotulagem", p);
        preencherOpcoes(model);
        return "processos/rotulagem/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoRotulagem p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/rotulagem"; }
        model.addAttribute("rotulagem", p);
        return "processos/rotulagem/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoRotulagem p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/rotulagem"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/rotulagem/" + id; }
        model.addAttribute("rotulagem", p);
        preencherOpcoes(model);
        return "processos/rotulagem/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("rotulagem") ProcessoRotulagem rot, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/rotulagem/form";
        }
        if (rot.getId() == null) {
            rot.setCodigo(codigoService.proximoCodigo(ProcessoRotulagem.PREFIXO));
            rot.setCriadoPor(auth.getName());
        } else {
            ProcessoRotulagem existente = repo.findById(rot.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/rotulagem";
            }
            rot.setCriadoPor(existente.getCriadoPor());
            rot.setEstado(existente.getEstado());
            rot.setDataFecho(existente.getDataFecho());
        }
        repo.save(rot);
        ra.addFlashAttribute("sucesso", "Rotulagem guardada: " + rot.getCodigo());
        return "redirect:/processos/rotulagem/" + rot.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoRotulagem p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/rotulagem"; }
        try {
            service.fechar(id);
            ra.addFlashAttribute("sucesso", "Rotulagem fechada. Baixa de rótulos/cápsulas/caixas; vinho marcado como rotulado.");
        } catch (RotulagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/rotulagem/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/rotulagem/" + id; }
        try {
            service.reabrir(id);
            ra.addFlashAttribute("sucesso", "Rotulagem reaberta. Stocks repostos; vinho já não está rotulado.");
        } catch (RotulagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/rotulagem/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoRotulagem p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/rotulagem"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra o processo antes de o eliminar (para repor stocks)."); return "redirect:/processos/rotulagem/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Rotulagem eliminada.");
        return "redirect:/processos/rotulagem";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("engarrafados", engarrafadoRepo.findByRotuladoFalseOrderByDataProducaoDesc());
        model.addAttribute("rotulos", consumivelRepo.findByTipoOrderByDescricaoAsc(TipoConsumivel.ROTULO));
        model.addAttribute("capsulas", consumivelRepo.findByTipoOrderByDescricaoAsc(TipoConsumivel.CAPSULA));
        model.addAttribute("caixas", consumivelRepo.findByTipoOrderByDescricaoAsc(TipoConsumivel.CAIXA));
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoRotulagem p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
