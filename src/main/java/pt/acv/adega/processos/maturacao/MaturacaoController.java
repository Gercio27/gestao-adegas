package pt.acv.adega.processos.maturacao;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.CastaRepository;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.fichas.VinhaRepository;
import pt.acv.adega.processos.EstadoProcesso;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/processos/maturacao")
public class MaturacaoController {

    private final ProcessoAnaliseMaturacaoRepository repo;
    private final VinhaRepository vinhaRepo;
    private final CastaRepository castaRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public MaturacaoController(ProcessoAnaliseMaturacaoRepository repo, VinhaRepository vinhaRepo,
                               CastaRepository castaRepo, TrabalhadorRepository trabalhadorRepo,
                               CodigoService codigoService) {
        this.repo = repo;
        this.vinhaRepo = vinhaRepo;
        this.castaRepo = castaRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/maturacao/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoAnaliseMaturacao p = new ProcessoAnaliseMaturacao();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("analise", p);
        preencherOpcoes(model);
        return "processos/maturacao/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoAnaliseMaturacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/maturacao"; }
        model.addAttribute("analise", p);
        return "processos/maturacao/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoAnaliseMaturacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/maturacao"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/maturacao/" + id; }
        model.addAttribute("analise", p);
        preencherOpcoes(model);
        return "processos/maturacao/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("analise") ProcessoAnaliseMaturacao analise, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/maturacao/form";
        }
        if (analise.getId() == null) {
            analise.setCodigo(codigoService.proximoCodigo(ProcessoAnaliseMaturacao.PREFIXO));
            analise.setCriadoPor(auth.getName());
        } else {
            ProcessoAnaliseMaturacao ex = repo.findById(analise.getId()).orElse(null);
            if (ex == null || !podeAceder(ex, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/maturacao";
            }
            analise.setCriadoPor(ex.getCriadoPor());
            analise.setEstado(ex.getEstado());
            analise.setDataFecho(ex.getDataFecho());
        }
        repo.save(analise);
        ra.addFlashAttribute("sucesso", "Boletim de análise guardado: " + analise.getCodigo());
        return "redirect:/processos/maturacao/" + analise.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoAnaliseMaturacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/maturacao"; }
        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
        ra.addFlashAttribute("sucesso", "Boletim fechado: " + p.getCodigo());
        return "redirect:/processos/maturacao/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoAnaliseMaturacao p = repo.findById(id).orElse(null);
        if (p == null || !isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/maturacao/" + id; }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
        ra.addFlashAttribute("sucesso", "Boletim reaberto.");
        return "redirect:/processos/maturacao/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoAnaliseMaturacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/maturacao"; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Boletim eliminado.");
        return "redirect:/processos/maturacao";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("vinhas", vinhaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoAnaliseMaturacao p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
