package pt.acv.adega.movimentos;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pt.acv.adega.fichas.*;
import pt.acv.adega.produtos.StockRotuladoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

/**
 * Historico de movimentos: o que entrou e saiu de cada ficha (talha, deposito,
 * contentor, palete, consumivel, vinho rotulado) e de cada adega/armazem.
 */
@Controller
@RequestMapping("/movimentos")
public class MovimentoStockController {

    private final MovimentoStockRepository repo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final ContentorGarrafasRepository contentorRepo;
    private final ContentorBagInBoxRepository bibRepo;
    private final ConsumivelRepository consumivelRepo;
    private final StockRotuladoRepository rotuladoRepo;
    private final AdegaRepository adegaRepo;
    private final ArmazemRepository armazemRepo;

    public MovimentoStockController(MovimentoStockRepository repo, TalhaRepository talhaRepo,
                                    DepositoRepository depositoRepo, ContentorGarrafasRepository contentorRepo,
                                    ContentorBagInBoxRepository bibRepo, ConsumivelRepository consumivelRepo,
                                    StockRotuladoRepository rotuladoRepo, AdegaRepository adegaRepo,
                                    ArmazemRepository armazemRepo) {
        this.repo = repo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.contentorRepo = contentorRepo;
        this.bibRepo = bibRepo;
        this.consumivelRepo = consumivelRepo;
        this.rotuladoRepo = rotuladoRepo;
        this.adegaRepo = adegaRepo;
        this.armazemRepo = armazemRepo;
    }

    /** Todos os movimentos, com filtros. E' a porta de entrada do menu. */
    @GetMapping
    public String geral(@RequestParam(required = false) TipoAlvo tipo,
                        @RequestParam(required = false) String local,
                        @RequestParam(required = false) String texto,
                        @RequestParam(required = false) String de,
                        @RequestParam(required = false) String ate,
                        @RequestParam(required = false) String ano,
                        @RequestParam(required = false) String mes,
                        Model model) {
        List<MovimentoStock> todas = tipo != null
                ? repo.findByTipoAlvoOrderByDataHoraDesc(tipo)
                : repo.findTop300ByOrderByDataHoraDesc();
        List<MovimentoStock> linhas = filtrar(todas, local, texto, de, ate, ano, mes);

        model.addAttribute("titulo", "Histórico de movimentos");
        model.addAttribute("subtitulo", tipo != null ? tipo.getDescricao() : "Todas as fichas");
        model.addAttribute("linhas", linhas);
        model.addAttribute("tipos", TipoAlvo.values());
        model.addAttribute("locais", locaisConhecidos());
        model.addAttribute("anos", anosCom(todas));
        model.addAttribute("meses", MESES);
        model.addAttribute("fTipo", tipo);
        model.addAttribute("fLocal", local);
        model.addAttribute("fTexto", texto);
        model.addAttribute("fDe", de);
        model.addAttribute("fAte", ate);
        model.addAttribute("fAno", inteiro(ano));
        model.addAttribute("fMes", inteiro(mes));
        model.addAttribute("filtrosVisiveis", true);
        model.addAttribute("mostrarFicha", true);
        model.addAttribute("totais", totais(linhas));
        return "movimentos/historico";
    }

    /**
     * Historico de uma ficha concreta. Abre no ano mais recente com movimentos —
     * e' o que interessa ver primeiro — e depois pode-se apertar por mes ou por
     * um dia certo.
     */
    @GetMapping("/{tipo}/{id}")
    public String daFicha(@PathVariable TipoAlvo tipo, @PathVariable Long id,
                          @RequestParam(required = false) String ano,
                          @RequestParam(required = false) String mes,
                          @RequestParam(required = false) String dia,
                          Model model) {
        List<MovimentoStock> todas = repo.findByTipoAlvoAndAlvoIdOrderByDataHoraDesc(tipo, id);
        model.addAttribute("titulo", "Histórico · " + nomeDaFicha(tipo, id));
        model.addAttribute("subtitulo", tipo.getDescricao() + verSaldo(tipo, id));
        model.addAttribute("mostrarFicha", false);
        model.addAttribute("voltarUrl", listaDe(tipo));
        aplicarFiltroDeData(model, todas, ano, mes, dia, "/movimentos/" + tipo.name() + "/" + id);
        return "movimentos/historico";
    }

    /** Tudo o que se mexeu numa adega ou armazem, com o mesmo filtro de datas. */
    @GetMapping("/local")
    public String doLocal(@RequestParam String nome,
                          @RequestParam(required = false) String ano,
                          @RequestParam(required = false) String mes,
                          @RequestParam(required = false) String dia,
                          Model model) {
        List<MovimentoStock> todas = repo.findByLocalOrderByDataHoraDesc(nome);
        model.addAttribute("titulo", "Histórico · " + nome);
        model.addAttribute("subtitulo", "Todos os recipientes e contentores deste local");
        model.addAttribute("mostrarFicha", true);
        aplicarFiltroDeData(model, todas, ano, mes, dia, "/movimentos/local");
        model.addAttribute("localFixo", nome);
        return "movimentos/historico";
    }

    /**
     * Filtro por ano / mes / dia, partilhado pelos historicos de ficha e de
     * local. Um dia certo manda sobre tudo o resto: se o utilizador escreve a
     * data, e' esse dia que quer ver, e nao a interseccao com o ano que estava
     * escolhido (que daria uma lista vazia sem se perceber porque).
     */
    private void aplicarFiltroDeData(Model model, List<MovimentoStock> todas,
                                     String ano, String mes, String dia, String url) {
        List<Integer> anos = anosCom(todas);
        LocalDate diaEscolhido = data(dia);
        Integer mesEscolhido = diaEscolhido != null ? null : inteiro(mes);

        Integer anoEscolhido = null;
        if (diaEscolhido == null && !"todos".equalsIgnoreCase(ano)) {
            anoEscolhido = inteiro(ano);
            // Primeira visita: abre no ano mais recente que tenha movimentos.
            if (anoEscolhido == null && !anos.isEmpty()) anoEscolhido = anos.get(0);
        }

        List<MovimentoStock> linhas = new ArrayList<>();
        for (MovimentoStock m : todas) {
            if (m.getDataHora() == null) continue;
            LocalDate d = m.getDataHora().toLocalDate();
            if (diaEscolhido != null) {
                if (!d.equals(diaEscolhido)) continue;
            } else {
                if (anoEscolhido != null && d.getYear() != anoEscolhido) continue;
                if (mesEscolhido != null && d.getMonthValue() != mesEscolhido) continue;
            }
            linhas.add(m);
        }

        model.addAttribute("linhas", linhas);
        model.addAttribute("totais", totais(linhas));
        model.addAttribute("filtrosVisiveis", false);
        model.addAttribute("filtroDatas", true);
        model.addAttribute("anos", anos);
        model.addAttribute("meses", MESES);
        model.addAttribute("anoEscolhido", anoEscolhido);
        model.addAttribute("mesEscolhido", mesEscolhido);
        model.addAttribute("diaEscolhido", dia);
        model.addAttribute("urlDoAno", url);
        model.addAttribute("totalGeral", todas.size());
    }

    /** Meses para o seletor: numero + nome. */
    public record Mes(int numero, String nome) { }

    private static final List<Mes> MESES = List.of(
            new Mes(1, "Janeiro"), new Mes(2, "Fevereiro"), new Mes(3, "Março"),
            new Mes(4, "Abril"), new Mes(5, "Maio"), new Mes(6, "Junho"),
            new Mes(7, "Julho"), new Mes(8, "Agosto"), new Mes(9, "Setembro"),
            new Mes(10, "Outubro"), new Mes(11, "Novembro"), new Mes(12, "Dezembro"));

    /** Anos que têm movimentos, do mais recente para o mais antigo. */
    private List<Integer> anosCom(List<MovimentoStock> linhas) {
        TreeSet<Integer> anos = new TreeSet<>(Comparator.reverseOrder());
        for (MovimentoStock m : linhas) {
            if (m.getDataHora() != null) anos.add(m.getDataHora().getYear());
        }
        return new ArrayList<>(anos);
    }

    private Integer inteiro(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.valueOf(s.trim()); } catch (Exception e) { return null; }
    }

    // ----- auxiliares -----

    private List<MovimentoStock> filtrar(List<MovimentoStock> linhas, String local, String texto,
                                         String de, String ate, String ano, String mes) {
        LocalDate dDe = data(de);
        LocalDate dAte = data(ate);
        Integer nAno = inteiro(ano);
        Integer nMes = inteiro(mes);
        String procura = texto == null ? null : texto.trim().toLowerCase();
        List<MovimentoStock> out = new ArrayList<>();
        for (MovimentoStock m : linhas) {
            if (local != null && !local.isBlank() && !local.equals(m.getLocal())) continue;
            LocalDate d = m.getDataHora() == null ? null : m.getDataHora().toLocalDate();
            if (d == null) continue;
            if (nAno != null && d.getYear() != nAno) continue;
            if (nMes != null && d.getMonthValue() != nMes) continue;
            if (dDe != null && d.isBefore(dDe)) continue;
            if (dAte != null && d.isAfter(dAte)) continue;
            if (procura != null && !procura.isEmpty() && !contem(m, procura)) continue;
            out.add(m);
        }
        return out;
    }

    private boolean contem(MovimentoStock m, String procura) {
        return (m.getAlvoNome() != null && m.getAlvoNome().toLowerCase().contains(procura))
                || (m.getVinhoNome() != null && m.getVinhoNome().toLowerCase().contains(procura))
                || (m.getOrigem() != null && m.getOrigem().toLowerCase().contains(procura))
                || (m.getDescricao() != null && m.getDescricao().toLowerCase().contains(procura));
    }

    private LocalDate data(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }

    /** Entradas e saidas somadas por unidade, para o resumo no topo. */
    private Map<String, BigDecimal[]> totais(List<MovimentoStock> linhas) {
        Map<String, BigDecimal[]> out = new LinkedHashMap<>();
        for (MovimentoStock m : linhas) {
            if (m.getQuantidade() == null) continue;
            String u = m.getUnidade() == null ? "" : m.getUnidade();
            BigDecimal[] par = out.computeIfAbsent(u, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (m.getQuantidade().signum() > 0) par[0] = par[0].add(m.getQuantidade());
            else par[1] = par[1].add(m.getQuantidade().abs());
        }
        return out;
    }

    /** Locais que aparecem nos movimentos, mais as adegas e armazens das fichas. */
    private List<String> locaisConhecidos() {
        List<String> out = new ArrayList<>();
        adegaRepo.findAll().forEach(a -> out.add("Adega " + a.getNome()));
        armazemRepo.findAll().forEach(a -> out.add("Armazém " + a.getNome()));
        return out;
    }

    private String nomeDaFicha(TipoAlvo tipo, Long id) {
        return switch (tipo) {
            case TALHA -> talhaRepo.findById(id).map(t -> "Talha " + t.getIdentificacao()).orElse("Talha #" + id);
            case DEPOSITO -> depositoRepo.findById(id).map(d -> "Depósito " + d.getIdentificacao()).orElse("Depósito #" + id);
            case CONTENTOR_GARRAFAS -> contentorRepo.findById(id).map(ContentorGarrafas::getNome).orElse("Contentor #" + id);
            case PALETE_BIB -> bibRepo.findById(id).map(ContentorBagInBox::getNome).orElse("Palete #" + id);
            case CONSUMIVEL -> consumivelRepo.findById(id).map(Consumivel::getDescricao).orElse("Consumível #" + id);
            case STOCK_ROTULADO -> rotuladoRepo.findById(id).map(s -> s.getVinhoNome() + " · " + s.getLocalNome())
                    .orElse("Vinho rotulado #" + id);
        };
    }

    /** " · tem X" com o stock atual da ficha, quando ela ainda existe. */
    private String verSaldo(TipoAlvo tipo, Long id) {
        String s = switch (tipo) {
            case TALHA -> talhaRepo.findById(id).map(t -> t.getVolumeAtualLitros() + " L").orElse(null);
            case DEPOSITO -> depositoRepo.findById(id).map(d -> d.getVolumeAtualLitros() + " L").orElse(null);
            case CONTENTOR_GARRAFAS -> contentorRepo.findById(id).map(c -> c.getGarrafasAtuais() + " garrafas").orElse(null);
            case PALETE_BIB -> bibRepo.findById(id).map(c -> c.getUnidadesAtuais() + " unidades").orElse(null);
            case CONSUMIVEL -> consumivelRepo.findById(id).map(c -> c.getStock() + " " + c.getUnidade()).orElse(null);
            case STOCK_ROTULADO -> rotuladoRepo.findById(id).map(r -> r.getGarrafas() + " garrafas").orElse(null);
        };
        return s == null ? "" : " · tem agora " + s;
    }

    private String listaDe(TipoAlvo tipo) {
        return switch (tipo) {
            case TALHA -> "/fichas/talhas";
            case DEPOSITO -> "/fichas/depositos";
            case CONTENTOR_GARRAFAS -> "/fichas/contentores";
            case PALETE_BIB -> "/fichas/bag-in-box";
            case CONSUMIVEL -> "/fichas/consumiveis";
            case STOCK_ROTULADO -> "/produtos/rotulados";
        };
    }
}
