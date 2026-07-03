package pt.acv.adega.processos.remontagem;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.RecipienteService;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.processos.EstadoProcesso;

import java.time.LocalDateTime;
import java.util.StringJoiner;

@Controller
@RequestMapping("/processos/remontagem")
public class RemontagemController {

    private final ProcessoRemontagemRepository repo;
    private final RecipienteService recipienteService;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public RemontagemController(ProcessoRemontagemRepository repo, RecipienteService recipienteService,
                                TrabalhadorRepository trabalhadorRepo, CodigoService codigoService) {
        this.repo = repo;
        this.recipienteService = recipienteService;
        this.trabalhadorRepo = trabalhadorRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/remontagem/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoRemontagem r = new ProcessoRemontagem();
        r.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("remontagem", r);
        preencherOpcoes(model);
        return "processos/remontagem/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoRemontagem r = repo.findById(id).orElse(null);
        if (r == null || !podeAceder(r, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/remontagem"; }
        model.addAttribute("remontagem", r);
        return "processos/remontagem/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoRemontagem r = repo.findById(id).orElse(null);
        if (r == null || !podeAceder(r, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/remontagem"; }
        if (!r.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/remontagem/" + id; }
        model.addAttribute("remontagem", r);
        preencherOpcoes(model);
        return "processos/remontagem/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("remontagem") ProcessoRemontagem remontagem, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/remontagem/form";
        }
        // Constroi a descricao dos recipientes a partir da selecao
        if (remontagem.getRecipienteRefs() != null && !remontagem.getRecipienteRefs().isEmpty()) {
            StringJoiner sj = new StringJoiner(", ");
            for (String ref : remontagem.getRecipienteRefs()) {
                RecipienteService.Recipiente rec = recipienteService.resolver(ref);
                if (rec.talha() != null) sj.add("Talha " + rec.talha().getIdentificacao());
                else if (rec.deposito() != null) sj.add("Depósito " + rec.deposito().getIdentificacao());
            }
            if (sj.length() > 0) remontagem.setRecipientes(sj.toString());
        }

        if (remontagem.getId() == null) {
            remontagem.setCodigo(codigoService.proximoCodigo(ProcessoRemontagem.PREFIXO));
            remontagem.setCriadoPor(auth.getName());
        } else {
            ProcessoRemontagem existente = repo.findById(remontagem.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/remontagem";
            }
            remontagem.setCriadoPor(existente.getCriadoPor());
            remontagem.setEstado(existente.getEstado());
            remontagem.setDataFecho(existente.getDataFecho());
            // Mantem a descricao anterior se nada foi selecionado
            if ((remontagem.getRecipienteRefs() == null || remontagem.getRecipienteRefs().isEmpty())
                    && remontagem.getRecipientes() == null) {
                remontagem.setRecipientes(existente.getRecipientes());
            }
        }
        repo.save(remontagem);
        ra.addFlashAttribute("sucesso", "Remontagem guardada: " + remontagem.getCodigo());
        return "redirect:/processos/remontagem/" + remontagem.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoRemontagem r = repo.findById(id).orElse(null);
        if (r == null || !podeAceder(r, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/remontagem"; }
        r.setEstado(EstadoProcesso.FECHADO);
        if (r.getDataHoraFim() == null) r.setDataHoraFim(LocalDateTime.now());
        r.setDataFecho(LocalDateTime.now());
        repo.save(r);
        ra.addFlashAttribute("sucesso", "Remontagem fechada: " + r.getCodigo());
        return "redirect:/processos/remontagem/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoRemontagem r = repo.findById(id).orElse(null);
        if (r == null || !isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/remontagem/" + id; }
        r.setEstado(EstadoProcesso.ABERTO);
        r.setDataFecho(null);
        repo.save(r);
        ra.addFlashAttribute("sucesso", "Remontagem reaberta.");
        return "redirect:/processos/remontagem/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoRemontagem r = repo.findById(id).orElse(null);
        if (r == null || !podeAceder(r, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/remontagem"; }
        repo.delete(r);
        ra.addFlashAttribute("sucesso", "Remontagem eliminada.");
        return "redirect:/processos/remontagem";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("recipientes", recipienteService.opcoes());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoRemontagem r, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(r.getCriadoPor());
    }
}
