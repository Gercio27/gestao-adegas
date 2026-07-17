package pt.acv.adega.processos.saidacontentor;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.ContentorGarrafas;
import pt.acv.adega.fichas.ContentorGarrafasRepository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Saidas de garrafas de um contentor por motivo (certificacao, prova, reserva
 * da adega, promocao, outras). Da baixa das garrafas no contentor; a eliminacao
 * repoe-as. Distinto da entrega ao comercial (Fase 10).
 */
@Controller
@RequestMapping("/processos/saida-contentor")
public class SaidaContentorController {

    private final SaidaContentorRepository repo;
    private final ContentorGarrafasRepository contentorRepo;
    private final CodigoService codigoService;

    public SaidaContentorController(SaidaContentorRepository repo, ContentorGarrafasRepository contentorRepo,
                                    CodigoService codigoService) {
        this.repo = repo;
        this.contentorRepo = contentorRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("saidas", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/saidacontentor/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        SaidaContentor s = new SaidaContentor();
        s.setDataSaida(LocalDateTime.now());
        model.addAttribute("saida", s);
        preencherOpcoes(model);
        return "processos/saidacontentor/form";
    }

    @PostMapping
    @Transactional
    public String guardar(@Valid @ModelAttribute("saida") SaidaContentor saida, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/saidacontentor/form";
        }
        if (saida.getContentorId() == null) {
            ra.addFlashAttribute("erro", "Escolha o contentor.");
            return "redirect:/processos/saida-contentor/nova";
        }
        ContentorGarrafas c = contentorRepo.findById(saida.getContentorId()).orElse(null);
        if (c == null) {
            ra.addFlashAttribute("erro", "Contentor não encontrado.");
            return "redirect:/processos/saida-contentor/nova";
        }
        if (saida.getQuantidade() <= 0) {
            ra.addFlashAttribute("erro", "Indique a quantidade de garrafas (> 0).");
            return "redirect:/processos/saida-contentor/nova";
        }
        if (saida.getQuantidade() > c.getGarrafasAtuais()) {
            ra.addFlashAttribute("erro", String.format("%s só tem %d garrafas — não pode sair %d.",
                    c.getNome(), c.getGarrafasAtuais(), saida.getQuantidade()));
            return "redirect:/processos/saida-contentor/nova";
        }
        // Baixa no contentor
        c.setGarrafasAtuais(c.getGarrafasAtuais() - saida.getQuantidade());
        contentorRepo.save(c);

        saida.setContentorNome(c.getNome());
        saida.setVinhoNome(c.getVinhoNome());
        saida.setCodigo(codigoService.proximoCodigo(SaidaContentor.PREFIXO));
        saida.setCriadoPor(auth.getName());
        if (saida.getDataSaida() == null) saida.setDataSaida(LocalDateTime.now());
        repo.save(saida);
        ra.addFlashAttribute("sucesso", "Saída registada: " + saida.getCodigo());
        return "redirect:/processos/saida-contentor";
    }

    @PostMapping("/{id}/eliminar")
    @Transactional
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        SaidaContentor s = repo.findById(id).orElse(null);
        if (s == null || !(isAdmin(auth) || auth.getName().equals(s.getCriadoPor()))) {
            ra.addFlashAttribute("erro", "Sem acesso a este registo.");
            return "redirect:/processos/saida-contentor";
        }
        // Repor as garrafas no contentor
        if (s.getContentorId() != null) {
            ContentorGarrafas c = contentorRepo.findById(s.getContentorId()).orElse(null);
            if (c != null) { c.setGarrafasAtuais(c.getGarrafasAtuais() + s.getQuantidade()); contentorRepo.save(c); }
        }
        repo.delete(s);
        ra.addFlashAttribute("sucesso", "Saída anulada. Garrafas repostas no contentor.");
        return "redirect:/processos/saida-contentor";
    }

    private void preencherOpcoes(Model model) {
        List<Map<String, Object>> contentores = new ArrayList<>();
        for (ContentorGarrafas c : contentorRepo.findAllByOrderByNomeAsc()) {
            if (c.getGarrafasAtuais() <= 0) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("label", c.getCodigo() + " · " + c.getNome() + " · "
                    + (c.getVinhoNome() != null ? c.getVinhoNome() : "—") + " · "
                    + c.getLocalizacao() + " · " + c.getGarrafasAtuais() + " garrafas");
            contentores.add(row);
        }
        model.addAttribute("contentores", contentores);
        model.addAttribute("motivos", MotivoSaidaContentor.values());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }
}
