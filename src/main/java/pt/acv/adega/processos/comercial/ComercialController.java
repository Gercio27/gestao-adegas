package pt.acv.adega.processos.comercial;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.ContentorService;
import pt.acv.adega.fichas.TipoEmbalagem;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/processos/comercial")
public class ComercialController {

    private final ProcessoPassagemComercialRepository repo;
    private final ComercialService service;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final ContentorService contentorService;
    private final CodigoService codigoService;

    public ComercialController(ProcessoPassagemComercialRepository repo, ComercialService service,
                               VinhoEngarrafadoRepository engarrafadoRepo, TrabalhadorRepository trabalhadorRepo,
                               ContentorService contentorService, CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.engarrafadoRepo = engarrafadoRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.contentorService = contentorService;
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
        // A partir do contentor escolhido, resolve o vinho engarrafado e o local de origem.
        TipoEmbalagem tipo = com.getTipoEmbalagem() != null ? com.getTipoEmbalagem() : TipoEmbalagem.GARRAFA;
        if (com.getContentorId() != null) {
            ContentorService.Opcao c = contentorService.procurar(tipo, com.getContentorId());
            if (c != null) {
                com.setOrigemDescricao(c.label());
                Long vegId = contentorService.vinhoDe(tipo, com.getContentorId());
                if (vegId != null) com.setEngarrafado(engarrafadoRepo.findById(vegId).orElse(null));
            }
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
        // Disponivel para entrega: contentores rotulados com stock, organizados
        // por tipo → local (adega/armazém) → nome do vinho. Sem isto vinha tudo
        // de todos os armazéns numa lista só, misturado.
        Map<String, Object> porTipo = new LinkedHashMap<>();
        Map<String, Object> locaisPorTipo = new LinkedHashMap<>();
        for (TipoEmbalagem tipo : TipoEmbalagem.values()) {
            Map<String, String> nomesLocais = contentorService.nomesDosLocais(tipo);
            Map<String, Map<String, List<Map<String, Object>>>> porLocal = new LinkedHashMap<>();
            for (ContentorService.Opcao o : contentorService.rotuladosComStock(tipo)) {
                String ref = contentorService.localDe(tipo, o.id());
                if (ref == null || ref.isEmpty()) continue;
                String vinho = o.vinhoNome() != null ? o.vinhoNome() : "(vinho sem nome)";
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", o.id());
                row.put("label", o.label());
                porLocal.computeIfAbsent(ref, k -> new LinkedHashMap<>())
                        .computeIfAbsent(vinho, k -> new ArrayList<>()).add(row);
            }
            List<Map<String, Object>> locais = new ArrayList<>();
            for (String ref : porLocal.keySet()) {
                Map<String, Object> l = new LinkedHashMap<>();
                l.put("ref", ref);
                l.put("nome", nomesLocais.getOrDefault(ref, ref));
                locais.add(l);
            }
            porTipo.put(tipo.name(), porLocal);
            locaisPorTipo.put(tipo.name(), locais);
        }
        model.addAttribute("porTipoLocalEVinho", porTipo);
        model.addAttribute("locaisPorTipo", locaisPorTipo);
        model.addAttribute("tiposEmbalagem", TipoEmbalagem.values());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoPassagemComercial p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
