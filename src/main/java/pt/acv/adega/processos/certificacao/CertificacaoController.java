package pt.acv.adega.processos.certificacao;

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
import pt.acv.adega.produtos.MostoRepository;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/processos/certificacao")
public class CertificacaoController {

    private final ProcessoCertificacaoRepository repo;
    private final CertificacaoService service;
    private final MostoRepository mostoRepo;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public CertificacaoController(ProcessoCertificacaoRepository repo, CertificacaoService service,
                                  MostoRepository mostoRepo, VinhoEngarrafadoRepository engarrafadoRepo,
                                  TrabalhadorRepository trabalhadorRepo, CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.mostoRepo = mostoRepo;
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
        return "processos/certificacao/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoCertificacao p = new ProcessoCertificacao();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("cert", p);
        preencherOpcoes(model);
        return "processos/certificacao/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoCertificacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/certificacao"; }
        model.addAttribute("cert", p);
        return "processos/certificacao/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoCertificacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/certificacao"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/certificacao/" + id; }
        model.addAttribute("cert", p);
        preencherOpcoes(model);
        return "processos/certificacao/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("cert") ProcessoCertificacao cert, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        // Mantem apenas o alvo escolhido
        if (cert.getAlvo() == AlvoCertificacao.GRANEL) cert.setEngarrafado(null);
        else cert.setVinhoGranel(null);

        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/certificacao/form";
        }
        if (cert.getId() == null) {
            cert.setCodigo(codigoService.proximoCodigo(ProcessoCertificacao.PREFIXO));
            cert.setCriadoPor(auth.getName());
        } else {
            ProcessoCertificacao existente = repo.findById(cert.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/certificacao";
            }
            cert.setCriadoPor(existente.getCriadoPor());
            cert.setEstado(existente.getEstado());
            cert.setDataFecho(existente.getDataFecho());
        }
        repo.save(cert);
        ra.addFlashAttribute("sucesso", "Certificação guardada: " + cert.getCodigo());
        return "redirect:/processos/certificacao/" + cert.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoCertificacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/certificacao"; }
        try {
            service.fechar(id);
            ra.addFlashAttribute("sucesso", "Certificação fechada. Resultado registado no produto.");
        } catch (CertificacaoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/certificacao/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/certificacao/" + id; }
        try {
            service.reabrir(id);
            ra.addFlashAttribute("sucesso", "Certificação reaberta. Marcação anulada no produto.");
        } catch (CertificacaoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/certificacao/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoCertificacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/certificacao"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra antes de eliminar (para anular a marcação)."); return "redirect:/processos/certificacao/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Certificação eliminada.");
        return "redirect:/processos/certificacao";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("vinhosGranel", mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL));
        model.addAttribute("engarrafados", engarrafadoRepo.findAllByOrderByDataProducaoDesc());
        model.addAttribute("alvos", AlvoCertificacao.values());
        model.addAttribute("resultados", ResultadoCertificacao.values());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoCertificacao p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
