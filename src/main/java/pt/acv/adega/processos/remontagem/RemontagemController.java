package pt.acv.adega.processos.remontagem;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.AdegaRepository;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.fichas.TalhaRepository;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/processos/remontagem")
public class RemontagemController {

    private final ProcessoRemontagemRepository repo;
    private final AdegaRepository adegaRepo;
    private final TalhaRepository talhaRepo;
    private final MostoRepository mostoRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public RemontagemController(ProcessoRemontagemRepository repo, AdegaRepository adegaRepo,
                                TalhaRepository talhaRepo, MostoRepository mostoRepo,
                                TrabalhadorRepository trabalhadorRepo, CodigoService codigoService) {
        this.repo = repo;
        this.adegaRepo = adegaRepo;
        this.talhaRepo = talhaRepo;
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
        return "processos/remontagem/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoRemontagem r = new ProcessoRemontagem();
        r.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("remontagem", r);
        preencherOpcoes(model, r);
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
        preencherOpcoes(model, r);
        return "processos/remontagem/form";
    }

    @PostMapping
    @Transactional
    public String guardar(@ModelAttribute("remontagem") ProcessoRemontagem remontagem,
                          @RequestParam(name = "talhasPresentes", required = false) List<Long> talhasPresentes,
                          @RequestParam(name = "concluidos", required = false) List<Long> concluidos,
                          Authentication auth, RedirectAttributes ra) {
        ProcessoRemontagem alvo;
        if (remontagem.getId() == null) {
            alvo = remontagem;
            alvo.setCodigo(codigoService.proximoCodigo(ProcessoRemontagem.PREFIXO));
            alvo.setCriadoPor(auth.getName());
        } else {
            alvo = repo.findById(remontagem.getId()).orElse(null);
            if (alvo == null || !podeAceder(alvo, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/remontagem";
            }
            alvo.setResponsavel(remontagem.getResponsavel());
            alvo.setDataHoraInicio(remontagem.getDataHoraInicio());
            alvo.setDataHoraFim(remontagem.getDataHoraFim());
            alvo.setMeios(remontagem.getMeios());
            alvo.setMetodos(remontagem.getMetodos());
            alvo.setObservacoes(remontagem.getObservacoes());
        }
        alvo.setAdega(remontagem.getAdega());

        // Reconstroi as talhas intervencionadas a partir da seleção submetida.
        List<Long> concl = concluidos == null ? List.of() : concluidos;
        alvo.getTalhas().clear();
        StringJoiner sj = new StringJoiner(", ");
        if (talhasPresentes != null) {
            for (Long tid : talhasPresentes) {
                Talha t = talhaRepo.findById(tid).orElse(null);
                if (t == null) continue;
                boolean feito = concl.contains(tid);
                RemontagemTalha rt = new RemontagemTalha();
                rt.setTalha(t);
                rt.setConcluido(feito);
                rt.setRemontagem(alvo);
                alvo.getTalhas().add(rt);
                sj.add("Talha " + t.getIdentificacao() + (feito ? " ✓" : ""));
            }
        }
        alvo.setRecipientes(sj.length() > 0 ? sj.toString() : null);

        repo.save(alvo);
        ra.addFlashAttribute("sucesso", "Remontagem guardada: " + alvo.getCodigo());
        return "redirect:/processos/remontagem/" + alvo.getId();
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

    // ----- auxiliares -----

    private void preencherOpcoes(Model model, ProcessoRemontagem r) {
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("talhasPorAdega", talhasPorAdega());
        // Estado guardado (para editar): talha id -> concluido
        Map<Long, Boolean> selecionadas = new LinkedHashMap<>();
        for (RemontagemTalha rt : r.getTalhas()) {
            if (rt.getTalha() != null) selecionadas.put(rt.getTalha().getId(), rt.isConcluido());
        }
        model.addAttribute("talhasSelecionadas", selecionadas);
    }

    /** Mapa adega -> talhas dessa adega com mosto em fermentação (id + etiqueta). */
    private Map<Long, List<Map<String, Object>>> talhasPorAdega() {
        List<Mosto> fermentando = new ArrayList<>();
        fermentando.addAll(mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.EM_FERMENTACAO));
        fermentando.addAll(mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.ATESTADO));

        // adega -> (talha -> [litros, castas])
        Map<Long, Map<Long, TalhaAgg>> agg = new LinkedHashMap<>();
        for (Mosto m : fermentando) {
            Talha t = m.getTalha();
            if (t == null || t.getAdega() == null) continue;
            Long adegaId = t.getAdega().getId();
            Map<Long, TalhaAgg> porTalha = agg.computeIfAbsent(adegaId, k -> new LinkedHashMap<>());
            TalhaAgg a = porTalha.computeIfAbsent(t.getId(), k -> new TalhaAgg(t.getIdentificacao()));
            a.litros = a.litros.add(m.getLitros() == null ? BigDecimal.ZERO : m.getLitros());
            if (m.getCasta() != null) a.castas.add(m.getCasta().getNome());
        }

        Map<Long, List<Map<String, Object>>> out = new LinkedHashMap<>();
        for (Map.Entry<Long, Map<Long, TalhaAgg>> e : agg.entrySet()) {
            List<Map<String, Object>> linhas = new ArrayList<>();
            for (Map.Entry<Long, TalhaAgg> te : e.getValue().entrySet()) {
                TalhaAgg a = te.getValue();
                String label = "Talha " + a.ident + " · " + a.litros.toPlainString() + " L"
                        + (a.castas.isEmpty() ? "" : " (" + String.join(", ", a.castas) + ")");
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", te.getKey());
                m.put("label", label);
                linhas.add(m);
            }
            out.put(e.getKey(), linhas);
        }
        return out;
    }

    private static class TalhaAgg {
        final String ident;
        BigDecimal litros = BigDecimal.ZERO;
        final LinkedHashSet<String> castas = new LinkedHashSet<>();
        TalhaAgg(String ident) { this.ident = ident; }
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoRemontagem r, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(r.getCriadoPor());
    }
}
