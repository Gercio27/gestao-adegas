package pt.acv.adega.processos.comercial;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/processos/comercial")
public class ComercialController {

    private final ProcessoPassagemComercialRepository repo;
    private final ComercialService service;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public ComercialController(ProcessoPassagemComercialRepository repo, ComercialService service,
                               VinhoEngarrafadoRepository engarrafadoRepo, TrabalhadorRepository trabalhadorRepo,
                               CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.engarrafadoRepo = engarrafadoRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/comercial/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoPassagemComercial p = new ProcessoPassagemComercial();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("comercial", p);
        preencherOpcoes(model);
        return "processos/comercial/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoPassagemComercial p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/comercial"; }
        model.addAttribute("comercial", p);
        return "processos/comercial/detalhe";
    }

    /** Nota de entrega imprimivel (imprimir / guardar como PDF pelo browser). */
    @GetMapping("/{id}/nota")
    public String nota(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoPassagemComercial p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/comercial"; }
        if (p.getNumeroNota() == null) { ra.addFlashAttribute("erro", "A nota só é emitida ao fechar o processo."); return "redirect:/processos/comercial/" + id; }
        model.addAttribute("comercial", p);
        return "processos/comercial/nota";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoPassagemComercial p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/comercial"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/comercial/" + id; }
        model.addAttribute("comercial", p);
        preencherOpcoes(model);
        return "processos/comercial/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("comercial") ProcessoPassagemComercial com, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/comercial/form";
        }
        if (com.getId() == null) {
            com.setCodigo(codigoService.proximoCodigo(ProcessoPassagemComercial.PREFIXO));
            com.setCriadoPor(auth.getName());
        } else {
            ProcessoPassagemComercial existente = repo.findById(com.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/comercial";
            }
            com.setCriadoPor(existente.getCriadoPor());
            com.setEstado(existente.getEstado());
            com.setDataFecho(existente.getDataFecho());
            com.setNumeroNota(existente.getNumeroNota());
        }
        repo.save(com);
        ra.addFlashAttribute("sucesso", "Registo guardado: " + com.getCodigo());
        return "redirect:/processos/comercial/" + com.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoPassagemComercial p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/comercial"; }
        try {
            service.fechar(id);
            ra.addFlashAttribute("sucesso", "Entrega registada. Nota de entrega emitida.");
        } catch (ComercialException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/comercial/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/comercial/" + id; }
        try {
            service.reabrir(id);
            ra.addFlashAttribute("sucesso", "Reaberto. Garrafas repostas no stock disponível.");
        } catch (ComercialException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/comercial/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoPassagemComercial p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/comercial"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra o processo antes de o eliminar (para repor o stock)."); return "redirect:/processos/comercial/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Registo eliminado.");
        return "redirect:/processos/comercial";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("engarrafados", engarrafadoRepo.findByRotuladoTrueOrderByDataProducaoDesc());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoPassagemComercial p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
