package pt.acv.adega.processos.passagem;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.AdegaRepository;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.fichas.TalhaRepository;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.processos.moagem.ProcessoMoagem;
import pt.acv.adega.processos.moagem.ProcessoMoagemRepository;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/processos/passagem-vinho")
public class PassagemController {

    private final ProcessoPassagemVinhoRepository repo;
    private final PassagemService passagemService;
    private final MostoRepository mostoRepo;
    private final AdegaRepository adegaRepo;
    private final TalhaRepository talhaRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final ProcessoMoagemRepository moagemRepo;
    private final CodigoService codigoService;

    public PassagemController(ProcessoPassagemVinhoRepository repo, PassagemService passagemService,
                              MostoRepository mostoRepo, AdegaRepository adegaRepo, TalhaRepository talhaRepo,
                              TrabalhadorRepository trabalhadorRepo, ProcessoMoagemRepository moagemRepo,
                              CodigoService codigoService) {
        this.repo = repo;
        this.passagemService = passagemService;
        this.mostoRepo = mostoRepo;
        this.adegaRepo = adegaRepo;
        this.talhaRepo = talhaRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.moagemRepo = moagemRepo;
        this.codigoService = codigoService;
    }

    /** Nome do vinho: gravado no mosto ou, em falta (dados antigos), do planeamento da moagem. */
    private String nomeVinho(Mosto m) {
        if (m.getVinhoNome() != null && !m.getVinhoNome().isBlank()) return m.getVinhoNome();
        if (m.getOrigemMoagemId() != null) {
            ProcessoMoagem mo = moagemRepo.findById(m.getOrigemMoagemId()).orElse(null);
            if (mo != null && mo.getPlano() != null) return mo.getPlano().getNomeVinho();
        }
        return null;
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
        preencherSelecao(p, model);
        return "processos/passagem/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("passagem") ProcessoPassagemVinho passagem, BindingResult result,
                          @RequestParam(required = false) List<Long> itemMostoId,
                          @RequestParam(required = false) List<BigDecimal> itemLitros,
                          @RequestParam(required = false) List<Long> itemTalhaDestino,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/passagem/form";
        }

        boolean novo = passagem.getId() == null;
        if (novo) {
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
        }

        // Reconstroi as linhas (mosto + litros efetivos + talha destino).
        passagem.getItens().clear();
        StringJoiner resumo = new StringJoiner("; ");
        if (itemMostoId != null) {
            for (int i = 0; i < itemMostoId.size(); i++) {
                Long mid = itemMostoId.get(i);
                if (mid == null) continue;
                Mosto m = mostoRepo.findById(mid).orElse(null);
                if (m == null) continue;
                BigDecimal orig = m.getLitros() == null ? BigDecimal.ZERO : m.getLitros();
                BigDecimal efet = (itemLitros != null && i < itemLitros.size() && itemLitros.get(i) != null)
                        ? itemLitros.get(i) : orig;
                Long dest = (itemTalhaDestino != null && i < itemTalhaDestino.size()) ? itemTalhaDestino.get(i) : null;
                if (dest != null && dest == 0L) dest = null;

                PassagemItem it = new PassagemItem();
                it.setProcesso(passagem);
                it.setMostoId(mid);
                it.setLitrosEfetivos(efet);
                it.setTalhaDestinoId(dest);
                String destTxt = dest == null ? "fica em " + m.getLocalizacao()
                        : "→ " + talhaRepo.findById(dest).map(t -> "Talha " + t.getIdentificacao()).orElse("talha");
                String nome = nomeVinho(m);
                it.setDescricao((nome != null ? nome + " · " : "") + m.getCodigo() + " · "
                        + m.getLocalizacao() + " · " + efet.toPlainString() + " L · " + destTxt);
                passagem.getItens().add(it);
                resumo.add(it.getDescricao());
            }
        }
        passagem.setMostosDescricao(resumo.length() > 0 ? resumo.toString() : null);

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
        // Mostos em fermentação agrupados por adega (do recipiente onde estão),
        // com os litros para propor como valor por omissão dos litros efetivos.
        Map<Long, List<Map<String, Object>>> mostosPorAdega = new LinkedHashMap<>();
        for (Mosto m : mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.EM_FERMENTACAO)) {
            Long adegaId = adegaDe(m);
            if (adegaId == null) continue;
            String nome = nomeVinho(m);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("litros", m.getLitros() == null ? BigDecimal.ZERO : m.getLitros());
            row.put("label", (nome != null ? nome + " · " : "") + m.getCodigo() + " · " + m.getLocalizacao() + " · "
                    + (m.getLitros() == null ? "0" : m.getLitros().toPlainString()) + " L");
            mostosPorAdega.computeIfAbsent(adegaId, k -> new ArrayList<>()).add(row);
        }
        // Talhas por adega (destino possível da passagem a limpo).
        Map<Long, List<Map<String, Object>>> talhasPorAdega = new LinkedHashMap<>();
        for (Talha t : talhaRepo.findAllByOrderByIdentificacaoAsc()) {
            if (t.getAdega() == null) continue;
            BigDecimal vol = t.getVolumeAtualLitros() == null ? BigDecimal.ZERO : t.getVolumeAtualLitros();
            String cap = t.getCapacidadeLitros() == null ? " (sem cap.)"
                    : " (" + vol.toPlainString() + "/" + t.getCapacidadeLitros().toPlainString() + " L)";
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", t.getId());
            row.put("label", "Talha " + t.getIdentificacao() + cap);
            talhasPorAdega.computeIfAbsent(t.getAdega().getId(), k -> new ArrayList<>()).add(row);
        }
        model.addAttribute("mostosPorAdega", mostosPorAdega);
        model.addAttribute("talhasPorAdega", talhasPorAdega);
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
        // Por omissão nada vem pré-selecionado (registo novo).
        model.addAttribute("selecionados", new LinkedHashMap<String, Object>());
        model.addAttribute("adegaSelecionada", "");
    }

    /**
     * Repoe no formulario as linhas ja guardadas, para que editar um registo
     * aberto nao apague a selecao anterior (o guardar reconstroi as linhas a
     * partir do que vem no formulario).
     */
    private void preencherSelecao(ProcessoPassagemVinho p, Model model) {
        Map<String, Object> selecionados = new LinkedHashMap<>();
        Long adegaSel = null;
        for (PassagemItem it : p.getItens()) {
            Mosto m = mostoRepo.findById(it.getMostoId()).orElse(null);
            if (m == null) continue;
            if (adegaSel == null) adegaSel = adegaDe(m);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("litros", it.getLitrosEfetivos() == null ? BigDecimal.ZERO : it.getLitrosEfetivos());
            row.put("destino", it.getTalhaDestinoId() == null ? 0L : it.getTalhaDestinoId());
            selecionados.put(String.valueOf(it.getMostoId()), row);
        }
        model.addAttribute("selecionados", selecionados);
        model.addAttribute("adegaSelecionada", adegaSel == null ? "" : String.valueOf(adegaSel));
    }

    private Long adegaDe(Mosto m) {
        if (m.getTalha() != null && m.getTalha().getAdega() != null) return m.getTalha().getAdega().getId();
        if (m.getDeposito() != null && m.getDeposito().getAdega() != null) return m.getDeposito().getAdega().getId();
        return null;
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoPassagemVinho p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
