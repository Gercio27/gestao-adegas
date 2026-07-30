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
import pt.acv.adega.fichas.ContentorService;
import pt.acv.adega.fichas.TipoEmbalagem;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Saidas de um contentor (de garrafas ou de bag-in-box) por motivo:
 * certificacao, prova, reserva da adega, promocao, outras. Da baixa das
 * unidades no contentor; a eliminacao repoe-as. Distinto da entrega ao
 * comercial (Fase 10).
 */
@Controller
@RequestMapping("/processos/saida-contentor")
public class SaidaContentorController {

    private final SaidaContentorRepository repo;
    private final ContentorService contentorService;
    private final CodigoService codigoService;

    public SaidaContentorController(SaidaContentorRepository repo, ContentorService contentorService,
                                    CodigoService codigoService) {
        this.repo = repo;
        this.contentorService = contentorService;
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
        TipoEmbalagem tipo = saida.getTipoEmbalagem() != null ? saida.getTipoEmbalagem() : TipoEmbalagem.GARRAFA;
        ContentorService.Opcao c = contentorService.procurar(tipo, saida.getContentorId());
        if (c == null) {
            ra.addFlashAttribute("erro", "Contentor não encontrado.");
            return "redirect:/processos/saida-contentor/nova";
        }
        if (saida.getQuantidade() <= 0) {
            ra.addFlashAttribute("erro", "Indique a quantidade de " + tipo.getUnidade() + " (> 0).");
            return "redirect:/processos/saida-contentor/nova";
        }
        if (saida.getQuantidade() > c.stock()) {
            ra.addFlashAttribute("erro", String.format("%s só tem %d %s — não pode sair %d.",
                    c.nome(), c.stock(), tipo.getUnidade(), saida.getQuantidade()));
            return "redirect:/processos/saida-contentor/nova";
        }
        // Baixa no contentor
        saida.setCodigo(codigoService.proximoCodigo(SaidaContentor.PREFIXO));
        contentorService.ajustar(tipo, saida.getContentorId(), -saida.getQuantidade(),
                "Saída de contentor " + saida.getCodigo(),
                saida.getMotivo() != null ? saida.getMotivo().getDescricao() : "Saída do contentor");

        saida.setContentorNome(c.nome());
        saida.setVinhoNome(c.vinhoNome());
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
        // Repor as unidades no contentor
        TipoEmbalagem tipo = s.getTipoEmbalagem() != null ? s.getTipoEmbalagem() : TipoEmbalagem.GARRAFA;
        contentorService.ajustar(tipo, s.getContentorId(), s.getQuantidade(),
                "Saída de contentor " + s.getCodigo(), "Saída anulada — unidades repostas");
        repo.delete(s);
        ra.addFlashAttribute("sucesso", "Saída anulada. " + s.getQuantidade() + " " + tipo.getUnidade() + " repostas no contentor.");
        return "redirect:/processos/saida-contentor";
    }

    private void preencherOpcoes(Model model) {
        // Uma lista por tipo de contentor; o formulário mostra a do tipo escolhido.
        Map<String, List<Map<String, Object>>> porTipo = new LinkedHashMap<>();
        for (TipoEmbalagem tipo : TipoEmbalagem.values()) {
            List<Map<String, Object>> lista = new ArrayList<>();
            for (ContentorService.Opcao o : contentorService.comStock(tipo)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", o.id());
                row.put("label", o.label());
                lista.add(row);
            }
            porTipo.put(tipo.name(), lista);
        }
        model.addAttribute("contentoresPorTipo", porTipo);
        model.addAttribute("tiposEmbalagem", TipoEmbalagem.values());
        model.addAttribute("motivos", MotivoSaidaContentor.values());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }
}
