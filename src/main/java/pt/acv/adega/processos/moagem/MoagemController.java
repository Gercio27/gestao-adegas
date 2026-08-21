package pt.acv.adega.processos.moagem;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
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
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Fase 3 — Moagem. Uma moagem faz-se numa adega, para um vinho, e mói as
 * colheitas entregues nessa adega — de qualquer vinho. A uva planeada para um
 * vinho pode acabar noutro, e e' o utilizador que decide isso aqui.
 */
@Controller
@RequestMapping("/processos/moagem")
public class MoagemController {

    private final ProcessoMoagemRepository repo;
    private final MoagemService moagemService;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final CastaRepository castaRepo;
    private final AdegaRepository adegaRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final LinhaPlaneamentoParcelaRepository linhaRepo;
    private final PlaneamentoVinhoRepository planeamentoRepo;
    private final EnchimentoRepository enchimentoRepo;
    private final EnchimentoVindimaRepository enchimentoVindimaRepo;
    private final MostoRepository mostoRepo;
    private final RegistoVindimaRepository registoVindimaRepo;
    private final SaldoColheitasService saldoColheitas;
    private final CodigoService codigoService;

    public MoagemController(ProcessoMoagemRepository repo, MoagemService moagemService,
                            TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                            CastaRepository castaRepo, AdegaRepository adegaRepo,
                            TrabalhadorRepository trabalhadorRepo, LinhaPlaneamentoParcelaRepository linhaRepo,
                            PlaneamentoVinhoRepository planeamentoRepo, EnchimentoRepository enchimentoRepo,
                            EnchimentoVindimaRepository enchimentoVindimaRepo,
                            MostoRepository mostoRepo, RegistoVindimaRepository registoVindimaRepo,
                            SaldoColheitasService saldoColheitas, CodigoService codigoService) {
        this.repo = repo;
        this.moagemService = moagemService;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.castaRepo = castaRepo;
        this.adegaRepo = adegaRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.linhaRepo = linhaRepo;
        this.planeamentoRepo = planeamentoRepo;
        this.enchimentoRepo = enchimentoRepo;
        this.enchimentoVindimaRepo = enchimentoVindimaRepo;
        this.mostoRepo = mostoRepo;
        this.registoVindimaRepo = registoVindimaRepo;
        this.saldoColheitas = saldoColheitas;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String folha(Model model) {
        // Vindimas disponíveis (para o seletor por adega + vinho, filtrado no cliente).
        // Cada uma leva já o saldo por moer: o que foi vindimado menos o que
        // outras moagens (mesmo abertas) já lhe tiraram.
        Map<Long, BigDecimal> usado = kgUsadoPorVindima();
        // O que ja foi moido de cada colheita vem do servico partilhado com a
        // folha da vindima: conta tanto as moagens que indicaram a colheita como
        // as antigas, que so' indicavam a parcela. Sem isto, uma colheita ja
        // moida numa moagem antiga voltava a aparecer aqui como intacta.
        SaldoColheitasService.Rastreio rastreio = saldoColheitas.calcular();

        List<Map<String, Object>> vindimas = new ArrayList<>();
        List<Map<String, Object>> colheitas = new ArrayList<>();
        // As parcelas que ainda tem uva por moer, para tambem as oferecer as
        // moagens ja abertas — nao so' ao formulario de moagem nova.
        List<LinhaPlaneamentoParcela> comUvaPorMoer = new ArrayList<>();
        // Parcelas que ficaram de fora e porque. Sem isto, uma parcela que nao
        // aparece nao tem explicacao nenhuma no ecra — e a meio da vindima e'
        // exatamente quando nao ha' tempo para andar a adivinhar.
        List<Map<String, Object>> deFora = new ArrayList<>();

        for (PlaneamentoVinho p : planeamentoRepo.findAllByOrderByNomeVinhoAsc()) {
            for (LinhaPlaneamentoParcela l : p.getLinhas()) {
                BigDecimal moido = usado.getOrDefault(l.getId(), BigDecimal.ZERO);
                BigDecimal disponivel = l.getTotalVindimadoKg().subtract(moido);

                String motivo = null;
                if (l.getTotalVindimadoKg().signum() <= 0) {
                    motivo = "ainda não tem colheitas registadas";
                } else if (l.getAdegaEntrega() == null) {
                    motivo = "a colheita ficou sem adega de entrega — corrija na Fase 2";
                } else if (disponivel.signum() <= 0) {
                    motivo = "já foi toda moída (" + moido.toPlainString() + " kg)";
                }
                if (motivo != null) {
                    deFora.add(deFora(l, p, motivo));
                    continue;
                }

                String parc = nomeParcela(l);
                String casta = (l.getParcela() != null && l.getParcela().getCasta() != null) ? l.getParcela().getCasta().getNome() : "—";
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", l.getId());
                m.put("adegaId", l.getAdegaEntrega().getId());
                m.put("adegaNome", l.getAdegaEntrega().getNome());
                m.put("planoId", p.getId());
                // Vinho para que a uva foi planeada. A moagem pode usa-la para
                // outro vinho; o ecra mostra de onde ela vem para se saber.
                m.put("vinho", p.getNomeVinho());
                m.put("label", parc + " (" + casta + ")");
                m.put("casta", casta);
                m.put("vindimado", l.getTotalVindimadoKg().toPlainString());
                m.put("moido", moido.toPlainString());
                m.put("disponivel", disponivel.toPlainString());
                m.put("texto", textoSaldo(disponivel));
                vindimas.add(m);
                comUvaPorMoer.add(l);
                colheitas.addAll(colheitasDaParcela(l, parc, casta, p.getNomeVinho(), rastreio));
            }
        }
        model.addAttribute("vindimasDisponiveis", vindimas);
        model.addAttribute("colheitasDisponiveis", colheitas);
        model.addAttribute("parcelasDeFora", deFora);
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("planos", planeamentoRepo.findAllByOrderByNomeVinhoAsc());
        model.addAttribute("recipientes", recipienteOpcoes());
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());

        List<ProcessoMoagem> moagens = repo.findAllByOrderByDataCriacaoDesc();
        model.addAttribute("moagens", moagens);
        // Kg de cada moagem, sempre calculados a partir das vindimas.
        Map<Long, ResumoMoagem> resumos = new HashMap<>();
        for (ProcessoMoagem mo : moagens) resumos.put(mo.getId(), resumoDaMoagem(mo, usado));
        model.addAttribute("resumos", resumos);
        Map<Long, List<Mosto>> mostosPorMoagem = new HashMap<>();
        for (ProcessoMoagem mo : moagens) {
            if (!mo.isAberto()) mostosPorMoagem.put(mo.getId(), mostoRepo.findByOrigemMoagemId(mo.getId()));
        }
        model.addAttribute("mostosPorMoagem", mostosPorMoagem);

        // Vindimas de cada moagem aberta, com o saldo por moer — para o
        // formulário de acrescentar enchimentos repartir os Kg por vindima.
        Map<Long, List<Map<String, Object>>> vindimasPorMoagem = new HashMap<>();
        Map<Long, List<Map<String, Object>>> colheitasPorMoagem = new HashMap<>();
        for (ProcessoMoagem mo : moagens) {
            if (!mo.isAberto()) continue;
            List<Map<String, Object>> linhas = new ArrayList<>();
            List<Map<String, Object>> cols = new ArrayList<>();
            Set<Long> jaNaMoagem = new HashSet<>();
            mo.getVindimas().forEach(l -> jaNaMoagem.add(l.getId()));

            // 1) As parcelas que a moagem ja tem. Aparecem sempre, mesmo sem uva
            //    por moer, para se ver o estado do que ja se moeu aqui.
            for (LinhaPlaneamentoParcela l : mo.getVindimas()) {
                linhas.add(parcelaDaMoagem(l, usado, true));
                cols.addAll(colheitasDaParcela(l, nomeParcela(l), castaDe(l),
                        l.getPlaneamento() != null ? l.getPlaneamento().getNomeVinho() : null, rastreio));
            }
            // 2) E o resto da uva por moer entregue na mesma adega. Sem isto, uma
            //    moagem aberta so' conseguia moer as parcelas escolhidas quando
            //    foi criada — uva colhida ou corrigida depois nunca la chegava.
            Long adegaMo = mo.getAdega() != null ? mo.getAdega().getId() : null;
            if (adegaMo != null) {
                for (LinhaPlaneamentoParcela l : comUvaPorMoer) {
                    if (jaNaMoagem.contains(l.getId())) continue;
                    if (l.getAdegaEntrega() == null || !adegaMo.equals(l.getAdegaEntrega().getId())) continue;
                    linhas.add(parcelaDaMoagem(l, usado, false));
                    cols.addAll(colheitasDaParcela(l, nomeParcela(l), castaDe(l),
                            l.getPlaneamento() != null ? l.getPlaneamento().getNomeVinho() : null, rastreio));
                }
            }
            vindimasPorMoagem.put(mo.getId(), linhas);
            colheitasPorMoagem.put(mo.getId(), cols);
        }
        model.addAttribute("vindimasPorMoagem", vindimasPorMoagem);
        model.addAttribute("colheitasPorMoagem", colheitasPorMoagem);
        return "processos/moagem/folha";
    }

    /**
     * As colheitas de uma parcela, cada uma com o que ainda tem por moer. E'
     * isto que o ecra da moagem mostra para o utilizador repartir os Kg: ele
     * escolhe de que colheita esta a moer, nao so' de que parcela.
     */
    private List<Map<String, Object>> colheitasDaParcela(LinhaPlaneamentoParcela l, String parc, String casta,
                                                         String vinho, SaldoColheitasService.Rastreio rastreio) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (RegistoVindima v : l.getVindimas()) {
            BigDecimal colhido = v.getQuantidadeKg() == null ? BigDecimal.ZERO : v.getQuantidadeKg();
            if (colhido.signum() <= 0) continue;
            BigDecimal moido = rastreio.moido(v.getId());
            BigDecimal disp = colhido.subtract(moido);
            // Colheita ja toda moida: nao volta a aparecer como disponivel.
            if (disp.signum() <= 0) continue;

            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", v.getId());
            c.put("linhaId", l.getId());
            c.put("adegaId", l.getAdegaEntrega() != null ? l.getAdegaEntrega().getId() : null);
            c.put("planoId", l.getPlaneamento() != null ? l.getPlaneamento().getId() : null);
            c.put("vinho", vinho);
            c.put("parcela", parc);
            c.put("casta", casta);
            c.put("codigo", v.getCodigo() != null ? v.getCodigo() : ("Colheita " + v.getId()));
            c.put("data", v.getDataInicio() != null ? v.getDataInicio().toString() : null);
            c.put("colhido", colhido.toPlainString());
            c.put("moido", moido.toPlainString());
            c.put("disponivel", disp.toPlainString());
            c.put("texto", textoSaldo(disp));
            out.add(c);
        }
        return out;
    }

    /**
     * Uma parcela na tabela de uma moagem aberta. {@code naMoagem} distingue as
     * que a moagem ja tinha das que sao oferecidas por estarem na mesma adega —
     * estas ultimas so' se juntam a moagem se o utilizador moer alguma coisa delas.
     */
    private Map<String, Object> parcelaDaMoagem(LinhaPlaneamentoParcela l, Map<Long, BigDecimal> usado,
                                                boolean naMoagem) {
        BigDecimal moido = usado.getOrDefault(l.getId(), BigDecimal.ZERO);
        BigDecimal disp = l.getTotalVindimadoKg().subtract(moido);
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", l.getId());
        v.put("label", l.getEtiqueta());
        v.put("casta", castaDe(l));
        v.put("vinho", l.getPlaneamento() != null ? l.getPlaneamento().getNomeVinho() : null);
        v.put("naMoagem", naMoagem);
        v.put("disponivel", disp.toPlainString());
        v.put("texto", textoSaldo(disp));
        return v;
    }

    private String castaDe(LinhaPlaneamentoParcela l) {
        return l.getParcela() != null && l.getParcela().getCasta() != null
                ? l.getParcela().getCasta().getNome() : "—";
    }

    /** Uma parcela que nao aparece na moagem, com a razao em linguagem corrente. */
    private Map<String, Object> deFora(LinhaPlaneamentoParcela l, PlaneamentoVinho p, String motivo) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("vinha", l.getParcela() != null && l.getParcela().getVinha() != null
                ? l.getParcela().getVinha().getNome() : "—");
        f.put("parcela", nomeParcela(l));
        f.put("vinho", p.getNomeVinho());
        f.put("adega", l.getAdegaEntrega() != null ? l.getAdegaEntrega().getNome() : null);
        f.put("vindimado", l.getTotalVindimadoKg().toPlainString());
        f.put("motivo", motivo);
        return f;
    }

    private String nomeParcela(LinhaPlaneamentoParcela l) {
        if (l.getParcela() == null) return "?";
        if (l.getParcela().getNome() != null && !l.getParcela().getNome().isBlank()) return l.getParcela().getNome();
        if (l.getParcela().getIdentificacao() != null && !l.getParcela().getIdentificacao().isBlank()) {
            return l.getParcela().getIdentificacao();
        }
        return "Parcela " + l.getParcela().getId();
    }

    /**
     * Histórico das moagens: todas as que já se fizeram, com filtro por adega,
     * vinho e datas. Serve para responder a "o que se moeu na adega X, do vinho
     * Y, em setembro" sem ter de percorrer a folha de trabalho toda.
     */
    @GetMapping("/historico")
    public String historico(@RequestParam(required = false) Long adega,
                            @RequestParam(required = false) Long plano,
                            @RequestParam(required = false) String de,
                            @RequestParam(required = false) String ate,
                            @RequestParam(required = false) String estado,
                            Model model) {
        LocalDate dDe = data(de);
        LocalDate dAte = data(ate);
        Map<Long, BigDecimal> usado = kgUsadoPorVindima();

        List<ProcessoMoagem> linhas = new ArrayList<>();
        for (ProcessoMoagem m : repo.findAllByOrderByDataCriacaoDesc()) {
            if (adega != null && (m.getAdega() == null || !adega.equals(m.getAdega().getId()))) continue;
            if (plano != null && (m.getPlano() == null || !plano.equals(m.getPlano().getId()))) continue;
            LocalDate d = m.getDataDaMoagem();
            if (dDe != null && (d == null || d.isBefore(dDe))) continue;
            if (dAte != null && (d == null || d.isAfter(dAte))) continue;
            if ("ABERTA".equals(estado) && !m.isAberto()) continue;
            if ("FECHADA".equals(estado) && m.isAberto()) continue;
            linhas.add(m);
        }

        BigDecimal totalKg = BigDecimal.ZERO;
        BigDecimal totalLitros = BigDecimal.ZERO;
        Map<Long, ResumoMoagem> resumos = new HashMap<>();
        for (ProcessoMoagem m : linhas) {
            totalKg = totalKg.add(m.getTotalMoidoKg());
            totalLitros = totalLitros.add(m.getTotalLitrosMosto());
            resumos.put(m.getId(), resumoDaMoagem(m, usado));
        }

        model.addAttribute("moagens", linhas);
        model.addAttribute("resumos", resumos);
        model.addAttribute("totalKg", totalKg);
        model.addAttribute("totalLitros", totalLitros);
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("planos", planeamentoRepo.findAllByOrderByNomeVinhoAsc());
        model.addAttribute("fAdega", adega);
        model.addAttribute("fPlano", plano);
        model.addAttribute("fDe", de);
        model.addAttribute("fAte", ate);
        model.addAttribute("fEstado", estado);
        return "processos/moagem/historico";
    }

    private LocalDate data(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }

    @PostMapping("/nova")
    @Transactional
    public String criar(@ModelAttribute MoagemForm form, Authentication auth, RedirectAttributes ra) {
        if (form.getAdega() == null || form.getPlano() == null) {
            ra.addFlashAttribute("erro", "Escolha a adega e o vinho.");
            return "redirect:/processos/moagem";
        }
        ProcessoMoagem m = new ProcessoMoagem();
        m.setCodigo(codigoService.proximoCodigo(ProcessoMoagem.PREFIXO));
        m.setCriadoPor(auth.getName());
        m.setAdega(form.getAdega());
        m.setPlano(form.getPlano());
        m.setResponsavel(form.getResponsavel());
        m.setDataHoraInicio(form.getDataInicio() != null ? form.getDataInicio().atStartOfDay() : LocalDateTime.now());
        if (form.getDataFim() != null) m.setDataHoraFim(form.getDataFim().atStartOfDay());
        if (form.getVindimaIds() != null) {
            for (Long lid : form.getVindimaIds()) {
                linhaRepo.findById(lid).ifPresent(m.getVindimas()::add);
            }
        }
        String aviso = appendEnchimentos(m, form.getEnchimentos());
        repo.save(m);
        ra.addFlashAttribute("sucesso", "Moagem criada: " + m.getCodigo());
        if (aviso != null) ra.addFlashAttribute("aviso", aviso);
        return "redirect:/processos/moagem";
    }

    @PostMapping("/{id}/enchimentos")
    @Transactional
    public String adicionarEnchimentos(@PathVariable Long id, @ModelAttribute MoagemForm form,
                                       Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a esta moagem."); return "redirect:/processos/moagem"; }
        if (!m.isAberto()) { ra.addFlashAttribute("erro", "Moagem fechada — reabra antes de alterar."); return "redirect:/processos/moagem"; }
        if (form.getResponsavel() != null) m.setResponsavel(form.getResponsavel());
        String aviso = appendEnchimentos(m, form.getEnchimentos());
        repo.save(m);
        ra.addFlashAttribute("sucesso", "Enchimentos guardados.");
        if (aviso != null) ra.addFlashAttribute("aviso", aviso);
        return "redirect:/processos/moagem";
    }

    /**
     * Corrige a análise de um enchimento: álcool provável, massa volúmica e pH.
     * Só o administrador. Ao contrário do resto da moagem, funciona também com a
     * moagem <b>fechada</b> — é a correção típica de quem recebe o boletim do
     * laboratório depois de o mosto já estar na talha. Nesse caso o valor é
     * escrito também na ficha de mosto gerada, para não ficarem os dois a
     * discordar. Volumes e Kg não se tocam, por isso não é preciso reabrir.
     */
    @PostMapping("/enchimento/{id}/analise")
    @Transactional
    public String analiseEnchimento(@PathVariable Long id,
                                    @RequestParam(required = false) BigDecimal alcoolProvavel,
                                    @RequestParam(required = false) BigDecimal massaVolumica,
                                    @RequestParam(required = false) BigDecimal ph,
                                    Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) {
            ra.addFlashAttribute("erro", "Apenas o administrador pode corrigir a análise do enchimento.");
            return "redirect:/processos/moagem";
        }
        Enchimento e = enchimentoRepo.findById(id).orElse(null);
        if (e == null) {
            ra.addFlashAttribute("erro", "Enchimento não encontrado.");
            return "redirect:/processos/moagem";
        }
        e.setAlcoolProvavel(alcoolProvavel);
        e.setMassaVolumica(massaVolumica);
        e.setPh(ph);
        enchimentoRepo.save(e);

        Mosto mosto = mostoGerado(e);
        if (mosto != null) {
            mosto.setAlcoolProvavel(alcoolProvavel);
            mosto.setMassaVolumica(massaVolumica);
            mosto.setPh(ph);
            mostoRepo.save(mosto);
            ra.addFlashAttribute("sucesso", "Análise atualizada, também na ficha de mosto " + mosto.getCodigo() + ".");
        } else {
            ra.addFlashAttribute("sucesso", "Análise do enchimento atualizada.");
        }
        return "redirect:/processos/moagem";
    }

    /**
     * Ficha de mosto que saiu deste enchimento, se a moagem já fechou. As fichas
     * novas guardam o id do enchimento; para as antigas (geradas antes de esse
     * id existir) tenta-se o recipiente + litros, e só se a correspondência for
     * única — na dúvida não se mexe em ficha nenhuma.
     */
    private Mosto mostoGerado(Enchimento e) {
        if (e.getMoagem() == null || e.getMoagem().isAberto()) return null;
        List<Mosto> gerados = mostoRepo.findByOrigemMoagemId(e.getMoagem().getId());
        for (Mosto m : gerados) {
            if (e.getId().equals(m.getOrigemEnchimentoId())) return m;
        }
        Long talhaId = e.getTalha() != null ? e.getTalha().getId() : null;
        Long depositoId = e.getDeposito() != null ? e.getDeposito().getId() : null;
        List<Mosto> iguais = new ArrayList<>();
        for (Mosto m : gerados) {
            if (m.getOrigemEnchimentoId() != null) continue;
            Long mTalha = m.getTalha() != null ? m.getTalha().getId() : null;
            Long mDeposito = m.getDeposito() != null ? m.getDeposito().getId() : null;
            if (!Objects.equals(talhaId, mTalha) || !Objects.equals(depositoId, mDeposito)) continue;
            if (e.getLitros() == null || m.getLitros() == null
                    || e.getLitros().compareTo(m.getLitros()) != 0) continue;
            iguais.add(m);
        }
        return iguais.size() == 1 ? iguais.get(0) : null;
    }

    @PostMapping("/enchimento/{id}/eliminar")
    @Transactional
    public String eliminarEnchimento(@PathVariable Long id, RedirectAttributes ra) {
        Enchimento e = enchimentoRepo.findById(id).orElse(null);
        if (e == null) { ra.addFlashAttribute("erro", "Enchimento não encontrado."); return "redirect:/processos/moagem"; }
        if (e.getMoagem() != null && !e.getMoagem().isAberto()) {
            ra.addFlashAttribute("erro", "Moagem fechada — reabra antes de remover.");
            return "redirect:/processos/moagem";
        }
        enchimentoRepo.delete(e);
        ra.addFlashAttribute("sucesso", "Enchimento removido.");
        return "redirect:/processos/moagem";
    }

    /**
     * Inicia uma nova moagem para a uva que ainda faltou moer. A quantidade nao
     * fica gravada: a nova moagem herda as mesmas vindimas e o que falta moer e'
     * recalculado sempre a partir delas. Assim a mesma uva nunca aparece a
     * dobrar em duas moagens.
     */
    @PostMapping("/{id}/nova-sobra")
    @Transactional
    public String novaSobra(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem orig = repo.findById(id).orElse(null);
        if (orig == null || !podeAceder(orig, auth)) { ra.addFlashAttribute("erro", "Sem acesso a esta moagem."); return "redirect:/processos/moagem"; }

        BigDecimal sobra = resumoDaMoagem(orig, kgUsadoPorVindima()).porMoer();
        if (sobra.signum() <= 0) {
            ra.addFlashAttribute("erro", "Já não há uva por moer nestas vindimas — não é preciso outra moagem.");
            return "redirect:/processos/moagem";
        }
        // Se ja existe uma moagem aberta e vazia para estas mesmas vindimas, e'
        // essa que deve ser usada. Evita a lista encher-se de moagens iguais.
        ProcessoMoagem jaExiste = moagemVaziaParaAsMesmasVindimas(orig);
        if (jaExiste != null) {
            ra.addFlashAttribute("aviso", "Já tinha criado a moagem " + jaExiste.getCodigo()
                    + " para estas vindimas e ainda está vazia. Use essa — sobram "
                    + sobra.toPlainString() + " kg por moer.");
            return "redirect:/processos/moagem";
        }

        ProcessoMoagem nova = new ProcessoMoagem();
        nova.setCodigo(codigoService.proximoCodigo(ProcessoMoagem.PREFIXO));
        nova.setCriadoPor(auth.getName());
        nova.setAdega(orig.getAdega());
        nova.setPlano(orig.getPlano());
        nova.setResponsavel(orig.getResponsavel());
        nova.getVindimas().addAll(orig.getVindimas());
        nova.setDataHoraInicio(LocalDateTime.now());
        repo.save(nova);
        ra.addFlashAttribute("sucesso", "Nova moagem criada para o que falta moer ("
                + sobra.toPlainString() + " kg): " + nova.getCodigo());
        return "redirect:/processos/moagem";
    }

    /** Moagem aberta, sem enchimentos, com exatamente as mesmas vindimas. */
    private ProcessoMoagem moagemVaziaParaAsMesmasVindimas(ProcessoMoagem orig) {
        Set<Long> alvo = new HashSet<>();
        orig.getVindimas().forEach(l -> alvo.add(l.getId()));
        if (alvo.isEmpty()) return null;
        for (ProcessoMoagem m : repo.findAllByOrderByDataCriacaoDesc()) {
            if (m.getId().equals(orig.getId()) || !m.isAberto()) continue;
            if (!m.getEnchimentos().isEmpty()) continue;
            Set<Long> dela = new HashSet<>();
            m.getVindimas().forEach(l -> dela.add(l.getId()));
            if (dela.equals(alvo)) return m;
        }
        return null;
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a esta moagem."); return "redirect:/processos/moagem"; }
        try {
            moagemService.fechar(id);
            ra.addFlashAttribute("sucesso", "Moagem fechada. Fichas de mosto geradas.");
        } catch (MoagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/moagem";
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/moagem"; }
        try {
            moagemService.reabrir(id);
            ra.addFlashAttribute("sucesso", "Moagem reaberta. Mostos anulados e volumes repostos.");
        } catch (MoagemException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/moagem";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoMoagem m = repo.findById(id).orElse(null);
        if (m == null || !podeAceder(m, auth)) { ra.addFlashAttribute("erro", "Sem acesso a esta moagem."); return "redirect:/processos/moagem"; }
        if (!m.isAberto()) { ra.addFlashAttribute("erro", "Reabra a moagem antes de a eliminar (para repor os mostos/volumes)."); return "redirect:/processos/moagem"; }
        repo.delete(m);
        ra.addFlashAttribute("sucesso", "Moagem eliminada.");
        return "redirect:/processos/moagem";
    }

    /**
     * Retrato dos Kg de uma moagem. O que <b>falta moer</b> nao sai desta
     * moagem: sai das vindimas dela, descontando tudo o que qualquer moagem ja
     * lhes tirou. Duas moagens da mesma vindima veem, por isso, o mesmo saldo —
     * que e' o correto, porque a uva e' a mesma.
     *
     * @param vindimado  total colhido nas vindimas desta moagem
     * @param moidoAqui  Kg moidos nesta moagem
     * @param moidoTotal Kg moidos por todas as moagens nestas vindimas
     * @param porMoer    uva destas vindimas que ainda ninguem moeu
     */
    public record ResumoMoagem(BigDecimal vindimado, BigDecimal moidoAqui,
                               BigDecimal moidoTotal, BigDecimal porMoer) {
        public boolean isTemSobra() { return porMoer.signum() > 0; }
        /** Ha' outra moagem a moer as mesmas vindimas? */
        public boolean isPartilhada() { return moidoTotal.compareTo(moidoAqui) != 0; }
    }

    /**
     * Saldo de uma vindima em linguagem corrente. Negativo quer dizer que se
     * moeu mais do que o que ficou registado na colheita — e' permitido (a
     * pesagem no campo nem sempre bate certo), mas tem de se ver.
     */
    private String textoSaldo(BigDecimal disponivel) {
        if (disponivel.signum() < 0) {
            return "0 kg (moeu " + disponivel.abs().toPlainString() + " kg a mais)";
        }
        return disponivel.toPlainString() + " kg";
    }

    private ResumoMoagem resumoDaMoagem(ProcessoMoagem m, Map<Long, BigDecimal> usado) {
        BigDecimal vindimado = BigDecimal.ZERO;
        BigDecimal moidoTotal = BigDecimal.ZERO;
        BigDecimal porMoer = BigDecimal.ZERO;
        for (LinhaPlaneamentoParcela l : m.getVindimas()) {
            BigDecimal colhido = l.getTotalVindimadoKg();
            BigDecimal jaMoido = usado.getOrDefault(l.getId(), BigDecimal.ZERO);
            vindimado = vindimado.add(colhido);
            moidoTotal = moidoTotal.add(jaMoido);
            BigDecimal falta = colhido.subtract(jaMoido);
            if (falta.signum() > 0) porMoer = porMoer.add(falta);
        }
        BigDecimal moidoAqui = m.getTotalMoidoKg();
        if (m.getVindimas().isEmpty()) {
            // Moagem sem vindimas (dados antigos): so' se pode contar o que tem.
            BigDecimal objetivo = m.getObjetivoKgManual() != null ? m.getObjetivoKgManual() : moidoAqui;
            BigDecimal falta = objetivo.subtract(moidoAqui);
            return new ResumoMoagem(objetivo, moidoAqui, moidoAqui,
                    falta.signum() > 0 ? falta : BigDecimal.ZERO);
        }
        return new ResumoMoagem(vindimado, moidoAqui, moidoTotal, porMoer);
    }

    // ----- auxiliares -----

    /**
     * Acrescenta os enchimentos vindos do formulário. Devolve uma mensagem de
     * erro se algum deles quiser moer mais Kg do que a vindima ainda tem — não
     * grava nada nesse caso (o método é chamado dentro de @Transactional).
     */
    /**
     * Acrescenta os enchimentos vindos do formulário. Moer mais do que a
     * vindima tem é permitido (acontece na prática — a pesagem no campo nem
     * sempre bate certo), mas devolve o aviso do excesso para o utilizador ver.
     */
    private String appendEnchimentos(ProcessoMoagem m, List<Enchimento> lista) {
        if (lista == null) return null;
        // Saldo já comprometido noutras moagens, por parcela e por colheita.
        Map<Long, BigDecimal> usado = kgUsadoPorVindima();
        // Mutavel: vai descontando a medida que percorre os enchimentos deste
        // formulario, para o aviso contar tambem o que se esta a moer agora.
        Map<Long, BigDecimal> usadoColheita = new HashMap<>(saldoColheitas.calcular().moidoPorColheita());
        List<String> avisos = new ArrayList<>();
        for (Enchimento e : lista) {
            if (e == null) continue;
            resolverOrigens(e);
            boolean semRecipiente = e.getRecipienteRef() == null || e.getRecipienteRef().isBlank();
            boolean semNada = e.getQuantidadeMoidaKg() == null && e.getLitros() == null
                    && e.getTotalOrigensKg().signum() == 0;
            if (semRecipiente && semNada) continue;
            e.setId(null);
            resolverRecipiente(e);
            resolverCastas(e);
            // Com vindimas indicadas, os Kg moídos são a soma delas.
            if (!e.getOrigens().isEmpty()) e.setQuantidadeMoidaKg(e.getTotalOrigensKg());

            avisos.addAll(excessos(e, usado, usadoColheita));
            e.setMoagem(m);
            m.getEnchimentos().add(e);
            // Juntar a moagem as parcelas de onde a uva saiu. Sem isto, moer de
            // uma parcela que nao foi escolhida quando a moagem foi criada
            // gravava o enchimento mas deixava-a de fora dos totais da moagem.
            for (EnchimentoVindima o : e.getOrigens()) {
                if (o.getLinha() == null) continue;
                boolean ja = m.getVindimas().stream()
                        .anyMatch(x -> x.getId() != null && x.getId().equals(o.getLinha().getId()));
                if (!ja) m.getVindimas().add(o.getLinha());
            }
        }
        return avisos.isEmpty() ? null : String.join(" ", avisos);
    }

    /**
     * Onde se está a moer mais do que o que sobrava; vai descontando. Com a
     * colheita indicada o saldo conta-se por colheita — e' o que o utilizador
     * escolheu, e e' aí que a diferença aparece.
     */
    private List<String> excessos(Enchimento e, Map<Long, BigDecimal> usado, Map<Long, BigDecimal> usadoColheita) {
        List<String> avisos = new ArrayList<>();
        for (EnchimentoVindima o : e.getOrigens()) {
            if (o.getLinha() == null || o.getQuantidadeKg() == null || o.getQuantidadeKg().signum() <= 0) continue;

            if (o.getColheita() != null) {
                Long cid = o.getColheita().getId();
                BigDecimal colhido = o.getColheita().getQuantidadeKg() == null
                        ? BigDecimal.ZERO : o.getColheita().getQuantidadeKg();
                BigDecimal disponivel = colhido.subtract(usadoColheita.getOrDefault(cid, BigDecimal.ZERO));
                if (o.getQuantidadeKg().compareTo(disponivel) > 0) {
                    avisos.add(String.format("%s: está a moer %s kg a mais do que essa colheita tinha por moer (sobravam %s kg).",
                            o.getVindimaDescricao(),
                            o.getQuantidadeKg().subtract(disponivel).toPlainString(),
                            (disponivel.signum() < 0 ? BigDecimal.ZERO : disponivel).toPlainString()));
                }
                usadoColheita.merge(cid, o.getQuantidadeKg(), BigDecimal::add);
            } else {
                Long lid = o.getLinha().getId();
                BigDecimal disponivel = o.getLinha().getTotalVindimadoKg()
                        .subtract(usado.getOrDefault(lid, BigDecimal.ZERO));
                if (o.getQuantidadeKg().compareTo(disponivel) > 0) {
                    avisos.add(String.format("%s: está a moer %s kg a mais do que tinha por moer (sobravam %s kg).",
                            o.getLinha().getEtiqueta(),
                            o.getQuantidadeKg().subtract(disponivel).toPlainString(),
                            (disponivel.signum() < 0 ? BigDecimal.ZERO : disponivel).toPlainString()));
                }
            }
            usado.merge(o.getLinha().getId(), o.getQuantidadeKg(), BigDecimal::add);
        }
        return avisos;
    }

    /**
     * Resolve a colheita (ou, em ultimo caso, a parcela) escolhida no formulário
     * e deita fora as linhas sem Kg. A parcela e' sempre preenchida — quando vem
     * a colheita, e' a parcela dela — porque os saldos da parcela contam a
     * partir dai.
     */
    private void resolverOrigens(Enchimento e) {
        List<EnchimentoVindima> validas = new ArrayList<>();
        if (e.getOrigens() != null) {
            for (EnchimentoVindima o : e.getOrigens()) {
                if (o == null || o.getQuantidadeKg() == null || o.getQuantidadeKg().signum() <= 0) continue;

                RegistoVindima colheita = null;
                if (o.getColheitaId() != null) {
                    colheita = registoVindimaRepo.findById(o.getColheitaId()).orElse(null);
                }
                LinhaPlaneamentoParcela linha = colheita != null ? colheita.getLinha() : null;
                if (linha == null && o.getLinhaId() != null) {
                    linha = linhaRepo.findById(o.getLinhaId()).orElse(null);
                }
                if (linha == null) continue;

                o.setId(null);
                o.setColheita(colheita);
                o.setLinha(linha);
                o.setEnchimento(e);
                validas.add(o);
            }
        }
        e.setOrigens(validas);
    }

    /** Kg já atribuídos a moagens, por vindima (inclui as moagens ainda abertas). */
    private Map<Long, BigDecimal> kgUsadoPorVindima() {
        Map<Long, BigDecimal> out = new HashMap<>();
        for (Object[] linha : enchimentoVindimaRepo.totaisPorVindima()) {
            if (linha[0] == null) continue;
            out.put((Long) linha[0], linha[1] == null ? BigDecimal.ZERO : (BigDecimal) linha[1]);
        }
        return out;
    }

    private List<RecipienteOpcao> recipienteOpcoes() {
        List<RecipienteOpcao> recipientes = new ArrayList<>();
        talhaRepo.findAllByOrderByIdentificacaoAsc().forEach(t ->
                recipientes.add(new RecipienteOpcao("TALHA:" + t.getId(),
                        "Talha " + t.getIdentificacao() + capacidadeTxt(t.getCapacidadeLitros(), t.getVolumeAtualLitros()),
                        cheia(t.getCapacidadeLitros(), t.getVolumeAtualLitros()))));
        depositoRepo.findAllByOrderByIdentificacaoAsc().forEach(d ->
                recipientes.add(new RecipienteOpcao("DEPOSITO:" + d.getId(),
                        "Depósito " + d.getIdentificacao() + capacidadeTxt(d.getCapacidadeLitros(), d.getVolumeAtualLitros()),
                        cheia(d.getCapacidadeLitros(), d.getVolumeAtualLitros()))));
        return recipientes;
    }

    private boolean cheia(BigDecimal capacidade, BigDecimal volume) {
        if (capacidade == null) return false;
        BigDecimal v = volume == null ? BigDecimal.ZERO : volume;
        return v.compareTo(capacidade) >= 0;
    }

    /**
     * Define as castas do enchimento. Com vindimas indicadas, a casta vem da
     * parcela de cada vindima — não é escolhida à mão. Só quando não há
     * vindimas (ex.: moagens antigas) é que se usa o multi-select.
     */
    private void resolverCastas(Enchimento e) {
        List<Casta> castas = new ArrayList<>();
        if (!e.getOrigens().isEmpty()) {
            for (EnchimentoVindima o : e.getOrigens()) {
                Casta c = o.getCasta();
                if (c != null && castas.stream().noneMatch(x -> x.getId().equals(c.getId()))) castas.add(c);
            }
            e.setCastas(castas);
            e.setCasta(castas.isEmpty() ? null : castas.get(0));
            return;
        }
        List<Long> ids = e.getCastaIds();
        // Compatibilidade: se vier a casta única (binding antigo), usa o id dela.
        if ((ids == null || ids.isEmpty()) && e.getCasta() != null && e.getCasta().getId() != null) {
            ids = List.of(e.getCasta().getId());
        }
        if (ids != null) {
            for (Long cid : ids) {
                if (cid != null) castaRepo.findById(cid).ifPresent(castas::add);
            }
        }
        e.setCastas(castas);
        e.setCasta(castas.isEmpty() ? null : castas.get(0));
    }

    private void resolverRecipiente(Enchimento e) {
        e.setTalha(null);
        e.setDeposito(null);
        String ref = e.getRecipienteRef();
        if (ref != null && ref.contains(":")) {
            String[] partes = ref.split(":", 2);
            Long rid = parseLong(partes[1]);
            if (rid != null) {
                if ("TALHA".equals(partes[0])) talhaRepo.findById(rid).ifPresent(e::setTalha);
                else if ("DEPOSITO".equals(partes[0])) depositoRepo.findById(rid).ifPresent(e::setDeposito);
            }
        }
    }

    private String capacidadeTxt(BigDecimal cap, BigDecimal vol) {
        if (cap == null) return " (sem capacidade definida)";
        BigDecimal v = vol == null ? BigDecimal.ZERO : vol;
        return " (" + v.toPlainString() + "/" + cap.toPlainString() + " L)";
    }

    private Long parseLong(String s) {
        try { return Long.valueOf(s.trim()); } catch (Exception e) { return null; }
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoMoagem m, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(m.getCriadoPor());
    }
}
