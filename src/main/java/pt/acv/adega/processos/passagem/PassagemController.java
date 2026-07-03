package pt.acv.adega.processos.passagem;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.time.LocalDateTime;
import java.util.StringJoiner;

@Controller
@RequestMapping("/processos/passagem-vinho")
public class PassagemController {

    private final ProcessoPassagemVinhoRepository repo;
    private final PassagemService passagemService;
    private final MostoRepository mostoRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public PassagemController(ProcessoPassagemVinhoRepository repo, PassagemService passagemService,
                              MostoRepository mostoRepo, TrabalhadorRepository trabalhadorRepo,
                              CodigoService codigoService) {
        this.repo = repo;
        this.passagemService = passagemService;
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
        return "processos/passagem/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoPassagemVinho p = new ProcessoPassagemVinho();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("passagem", p);
        preencherOpcoes(model);
        return "processos/passagem/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoPassagemVinho p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/passagem-vinho"; }
        model.addAttribute("passagem", p);
        return "processos/passagem/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoPassagemVinho p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/passagem-vinho"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/passagem-vinho/" + id; }
        model.addAttribute("passagem", p);
        preencherOpcoes(model);
        return "processos/passagem/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("passagem") ProcessoPassagemVinho passagem, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/passagem/form";
        }
        // Constroi csv de ids + descricao (codigos) a partir da selecao
        if (passagem.getMostoIds() != null && !passagem.getMostoIds().isEmpty()) {
            StringJoiner ids = new StringJoiner(",");
            StringJoiner cods = new StringJoiner(", ");
            for (Long mid : passagem.getMostoIds()) {
                Mosto m = mostoRepo.findById(mid).orElse(null);
                if (m != null) { ids.add(String.valueOf(mid)); cods.add(m.getCodigo() + " (" + m.getLocalizacao() + ")"); }
            }
            passagem.setMostosIdsCsv(ids.length() > 0 ? ids.toString() : null);
            passagem.setMostosDescricao(cods.length() > 0 ? cods.toString() : null);
        }

        if (passagem.getId() == null) {
            passagem.setCodigo(codigoService.proximoCodigo(ProcessoPassagemVinho.PREFIXO));
            passagem.setCriadoPor(auth.getName());
        } else {
            ProcessoPassagemVinho existente = repo.findById(passagem.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/passagem-vinho";
            }
            passagem.setCriadoPor(existente.getCriadoPor());
            passagem.setEstado(existente.getEstado());
            passagem.setDataFecho(existente.getDataFecho());
            if (passagem.getMostosIdsCsv() == null) {
                passagem.setMostosIdsCsv(existente.getMostosIdsCsv());
                passagem.setMostosDescricao(existente.getMostosDescricao());
            }
        }
        repo.save(passagem);
        ra.addFlashAttribute("sucesso", "Registo guardado: " + passagem.getCodigo());
        return "redirect:/processos/passagem-vinho/" + passagem.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoPassagemVinho p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/passagem-vinho"; }
        try {
            passagemService.fechar(id);
            ra.addFlashAttribute("sucesso", "Fechado. Os mostos passaram a vinho pronto a granel.");
        } catch (PassagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/passagem-vinho/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/passagem-vinho/" + id; }
        try {
            passagemService.reabrir(id);
            ra.addFlashAttribute("sucesso", "Reaberto. Os vinhos voltaram a mosto em fermentação.");
        } catch (PassagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/passagem-vinho/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoPassagemVinho p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/passagem-vinho"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra o processo antes de o eliminar."); return "redirect:/processos/passagem-vinho/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Registo eliminado.");
        return "redirect:/processos/passagem-vinho";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("mostosDisponiveis", mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.EM_FERMENTACAO));
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoPassagemVinho p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
