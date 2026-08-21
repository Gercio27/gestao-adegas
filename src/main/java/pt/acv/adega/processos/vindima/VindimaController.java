package pt.acv.adega.processos.vindima;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.*;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcela;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcelaRepository;
import pt.acv.adega.planeamento.PlaneamentoVinho;
import pt.acv.adega.planeamento.PlaneamentoVinhoRepository;
import pt.acv.adega.planeamento.RegistoVindima;
import pt.acv.adega.planeamento.RegistoVindimaRepository;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.processos.moagem.EnchimentoVindimaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Processo de Vindima (Fase 2). Abre-se, preenche-se e fecha-se.
 * Acesso: o utilizador que abriu ve os seus; o administrador ve todos.
 */
@Controller
@RequestMapping("/processos/vindima")
public class VindimaController {

    private final ProcessoVindimaRepository repo;
    private final VinhaRepository vinhaRepo;
    private final CastaRepository castaRepo;
    private final AdegaRepository adegaRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;
    private final PlaneamentoVinhoRepository planeamentoRepo;
    private final LinhaPlaneamentoParcelaRepository linhaRepo;
    private final RegistoVindimaRepository registoVindimaRepo;
    private final EnchimentoVindimaRepository enchimentoVindimaRepo;

    public VindimaController(ProcessoVindimaRepository repo, VinhaRepository vinhaRepo,
                             CastaRepository castaRepo, AdegaRepository adegaRepo,
                             TrabalhadorRepository trabalhadorRepo, CodigoService codigoService,
                             PlaneamentoVinhoRepository planeamentoRepo, LinhaPlaneamentoParcelaRepository linhaRepo,
                             RegistoVindimaRepository registoVindimaRepo,
                             EnchimentoVindimaRepository enchimentoVindimaRepo) {
        this.repo = repo;
        this.vinhaRepo = vinhaRepo;
        this.castaRepo = castaRepo;
        this.adegaRepo = adegaRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.codigoService = codigoService;
        this.planeamentoRepo = planeamentoRepo;
        this.linhaRepo = linhaRepo;
        this.registoVindimaRepo = registoVindimaRepo;
        this.enchimentoVindimaRepo = enchimentoVindimaRepo;
    }

    /** Fase 2 — Folha da vindima sobre todo o planeamento (vinhos e parcelas). */
    @GetMapping
    public String folha(Model model) {
        List<PlaneamentoVinho> vinhos = planeamentoRepo.findAllByOrderByNomeVinhoAsc();
        model.addAttribute("vinhos", vinhos);
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("moidoPorParcela", moidoPorParcela());
        repartirPorColheita(vinhos, model);
        return "processos/vindima/folha";
    }

    /**
     * Diz, colheita a colheita, em que moagens a uva foi parar e quantos Kg
     * cada uma levou.
     *
     * <p>Nas moagens feitas desde que a escolha da colheita existe, isto e'
     * <b>o que o utilizador registou</b>: no ecra da moagem ele indica de que
     * colheita esta a moer.
     *
     * <p>Nos registos antigos — feitos quando so' se escolhia a parcela — a
     * colheita vem a nulo e nao ha' como saber. Esses sao repartidos por ordem
     * de chegada (primeiro as colheitas mais antigas que ainda tenham uva), e
     * so' depois do que ja' esta registado. O ecra marca-os para nao se
     * confundir o calculado com o registado.
     *
     * <p>Moer mais do que se colheu e' permitido (a pesagem no campo nem sempre
     * bate certo). Esse excesso e' mostrado a parte, no bloco da parcela.
     */
    private void repartirPorColheita(List<PlaneamentoVinho> vinhos, Model model) {
        Map<Long, List<UsoMoagem>> moagensPorParcela = moagensPorParcela();
        Map<Long, List<MoagemDaVindima>> usosPorColheita = new HashMap<>();
        Map<Long, BigDecimal> porMoerPorColheita = new HashMap<>();
        Map<Long, BigDecimal> excessoPorParcela = new HashMap<>();
        Set<Long> colheitasEstimadas = new HashSet<>();

        for (PlaneamentoVinho p : vinhos) {
            for (LinhaPlaneamentoParcela l : p.getLinhas()) {
                // As colheitas ja vem por ordem de criacao (@OrderBy("id")).
                List<RegistoVindima> colheitas = l.getVindimas();
                Map<Long, BigDecimal> saldo = new HashMap<>();
                for (RegistoVindima v : colheitas) {
                    saldo.put(v.getId(), v.getQuantidadeKg() == null ? BigDecimal.ZERO : v.getQuantidadeKg());
                }
                List<UsoMoagem> usos = moagensPorParcela.getOrDefault(l.getId(), List.of());
                BigDecimal excesso = BigDecimal.ZERO;

                // 1) O que o utilizador escolheu: vai direto a colheita dele.
                for (UsoMoagem u : usos) {
                    if (u.colheitaId() == null || !saldo.containsKey(u.colheitaId())) continue;
                    usosPorColheita.computeIfAbsent(u.colheitaId(), k -> new ArrayList<>()).add(u.moagem());
                    saldo.merge(u.colheitaId(), u.moagem().kg().negate(), BigDecimal::add);
                }
                // 2) Os registos antigos: repartidos por ordem de chegada.
                for (UsoMoagem u : usos) {
                    if (u.colheitaId() != null && saldo.containsKey(u.colheitaId())) continue;
                    BigDecimal falta = u.moagem().kg();
                    for (RegistoVindima v : colheitas) {
                        if (falta.signum() <= 0) break;
                        BigDecimal disponivel = saldo.get(v.getId());
                        if (disponivel == null || disponivel.signum() <= 0) continue;
                        BigDecimal usa = disponivel.min(falta);
                        MoagemDaVindima m = u.moagem();
                        usosPorColheita.computeIfAbsent(v.getId(), k -> new ArrayList<>())
                                .add(new MoagemDaVindima(m.codigo(), m.data(), usa, m.aberta()));
                        colheitasEstimadas.add(v.getId());
                        saldo.put(v.getId(), disponivel.subtract(usa));
                        falta = falta.subtract(usa);
                    }
                    if (falta.signum() > 0) excesso = excesso.add(falta);
                }

                // Saldo negativo = moeu-se mais dessa colheita do que se colheu.
                for (RegistoVindima v : colheitas) {
                    BigDecimal s = saldo.get(v.getId());
                    if (s != null && s.signum() < 0) {
                        excesso = excesso.add(s.negate());
                        s = BigDecimal.ZERO;
                    }
                    porMoerPorColheita.put(v.getId(), s);
                }
                if (excesso.signum() > 0) excessoPorParcela.put(l.getId(), excesso);
            }
        }

        model.addAttribute("usosPorColheita", usosPorColheita);
        model.addAttribute("porMoerPorColheita", porMoerPorColheita);
        model.addAttribute("excessoPorParcela", excessoPorParcela);
        model.addAttribute("colheitasEstimadas", colheitasEstimadas);
    }

    /** Total de Kg que as moagens ja levaram de cada parcela. */
    private Map<Long, BigDecimal> moidoPorParcela() {
        Map<Long, BigDecimal> mapa = new HashMap<>();
        for (Object[] l : enchimentoVindimaRepo.totaisPorVindima()) {
            if (l[0] == null) continue;
            mapa.put((Long) l[0], l[1] == null ? BigDecimal.ZERO : (BigDecimal) l[1]);
        }
        return mapa;
    }

    /**
     * Para onde foi a uva de cada parcela: que moagens a usaram e quantos Kg
     * levaram. Serve para, na folha da vindima, se ver logo o destino da
     * colheita sem ter de ir a Fase 3 procurar.
     */
    private Map<Long, List<UsoMoagem>> moagensPorParcela() {
        Map<Long, List<UsoMoagem>> mapa = new HashMap<>();
        for (Object[] l : enchimentoVindimaRepo.moagensPorVindima()) {
            if (l[0] == null) continue;
            Long linhaId = (Long) l[0];
            Long colheitaId = (Long) l[1];
            String codigo = (String) l[2];
            // A data da moagem e a de inicio; se nao foi preenchida, vale a de
            // criacao — a mesma regra do ecra da moagem.
            LocalDateTime inicio = (LocalDateTime) l[3];
            LocalDateTime criacao = (LocalDateTime) l[4];
            LocalDateTime quando = inicio != null ? inicio : criacao;
            boolean aberta = l[5] == EstadoProcesso.ABERTO;
            BigDecimal kg = l[6] == null ? BigDecimal.ZERO : (BigDecimal) l[6];
            mapa.computeIfAbsent(linhaId, k -> new ArrayList<>()).add(new UsoMoagem(colheitaId,
                    new MoagemDaVindima(codigo, quando != null ? quando.toLocalDate() : null, kg, aberta)));
        }
        return mapa;
    }

    /**
     * Kg que uma moagem tirou de uma parcela. Quando {@code colheitaId} vem
     * preenchido, foi o utilizador que escolheu a colheita no ecra da moagem;
     * a nulo, e' um registo antigo, feito antes de essa escolha existir.
     */
    private record UsoMoagem(Long colheitaId, MoagemDaVindima moagem) {
    }

    /**
     * Acrescenta UMA colheita nova a uma linha do planeamento, com os seus
     * proprios dados (operador, carrinha, caixa, datas, Kg). Cada submissao cria
     * um registo independente — nao herda dados da colheita anterior.
     */
    @PostMapping("/linha/{id}")
    @Transactional
    public String guardarLinha(@PathVariable Long id, @ModelAttribute VindimaLinhaForm form, RedirectAttributes ra) {
        LinhaPlaneamentoParcela l = linhaRepo.findById(id).orElse(null);
        if (l == null) { ra.addFlashAttribute("erro", "Linha de planeamento não encontrada."); return "redirect:/processos/vindima"; }

        // A adega de entrega é da parcela (usada pela moagem para agrupar).
        if (form.getAdegaEntrega() != null) l.setAdegaEntrega(form.getAdegaEntrega());

        boolean temDados = form.getQuantidadeKg() != null || form.getDataInicio() != null
                || form.getDataFim() != null || form.getResponsavel() != null
                || naoVazio(form.getVasilame()) || naoVazio(form.getTransporte())
                || naoVazio(form.getMeios()) || naoVazio(form.getMetodos()) || naoVazio(form.getObservacoes());

        if (temDados) {
            RegistoVindima r = new RegistoVindima();
            r.setCodigo(codigoService.proximoCodigo(RegistoVindima.PREFIXO));
            r.setDataInicio(form.getDataInicio());
            r.setDataFim(form.getDataFim());
            r.setQuantidadeKg(form.getQuantidadeKg());
            r.setResponsavel(form.getResponsavel());
            r.setVasilame(form.getVasilame());
            r.setMeios(form.getMeios());
            r.setMetodos(form.getMetodos());
            r.setTransporte(form.getTransporte());
            r.setObservacoes(form.getObservacoes());
            r.setLinha(l);
            l.getVindimas().add(r);
            linhaRepo.save(l);
            ra.addFlashAttribute("sucesso", "Colheita registada: " + r.getCodigo());
        } else {
            linhaRepo.save(l);
            ra.addFlashAttribute("sucesso", "Adega de entrega atualizada.");
        }
        return "redirect:/processos/vindima";
    }

    private boolean naoVazio(String s) { return s != null && !s.isBlank(); }

    /** Remove uma colheita específica de uma linha (para correções). */
    @PostMapping("/vindima/{registoId}/eliminar")
    @Transactional
    public String eliminarVindima(@PathVariable Long registoId, RedirectAttributes ra) {
        registoVindimaRepo.findById(registoId).ifPresent(registoVindimaRepo::delete);
        ra.addFlashAttribute("sucesso", "Colheita removida.");
        return "redirect:/processos/vindima";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoVindima p = new ProcessoVindima();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("vindima", p);
        preencherOpcoes(model);
        return "processos/vindima/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoVindima p = repo.findById(id).orElse(null);
        if (p == null) { ra.addFlashAttribute("erro", "Vindima não encontrada."); return "redirect:/processos/vindima"; }
        if (!podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/vindima"; }
        model.addAttribute("vindima", p);
        return "processos/vindima/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoVindima p = repo.findById(id).orElse(null);
        if (p == null) { ra.addFlashAttribute("erro", "Vindima não encontrada."); return "redirect:/processos/vindima"; }
        if (!podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/vindima"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/vindima/" + id; }
        model.addAttribute("vindima", p);
        preencherOpcoes(model);
        return "processos/vindima/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("vindima") ProcessoVindima vindima, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/vindima/form";
        }
        if (vindima.getId() == null) {
            vindima.setCodigo(codigoService.proximoCodigo(ProcessoVindima.PREFIXO));
            vindima.setCriadoPor(auth.getName());
        } else {
            // Preserva autor/estado do registo existente e valida acesso.
            ProcessoVindima existente = repo.findById(vindima.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/vindima";
            }
            vindima.setCriadoPor(existente.getCriadoPor());
            vindima.setEstado(existente.getEstado());
            vindima.setDataFecho(existente.getDataFecho());
        }
        repo.save(vindima);
        ra.addFlashAttribute("sucesso", "Vindima guardada: " + vindima.getCodigo());
        return "redirect:/processos/vindima/" + vindima.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoVindima p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/vindima"; }
        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
        ra.addFlashAttribute("sucesso", "Vindima fechada: " + p.getCodigo());
        return "redirect:/processos/vindima/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoVindima p = repo.findById(id).orElse(null);
        if (p == null || !isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/vindima"; }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
        ra.addFlashAttribute("sucesso", "Vindima reaberta: " + p.getCodigo());
        return "redirect:/processos/vindima/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoVindima p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/vindima"; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Vindima eliminada.");
        return "redirect:/processos/vindima";
    }

    // ----- auxiliares -----

    private void preencherOpcoes(Model model) {
        model.addAttribute("vinhas", vinhaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("origens", OrigemUva.values());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoVindima p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
