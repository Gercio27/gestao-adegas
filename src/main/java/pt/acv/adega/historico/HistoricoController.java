package pt.acv.adega.historico;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Separador Historico: primeiro escolhe-se o processo, depois filtra-se. Os
 * filtros sao sempre os mesmos (ano, mes, dia, estado, tipo, adega, vinho,
 * responsavel, origem, destino) e so' aparecem os que fazem sentido para o
 * processo escolhido — quer dizer, aqueles em que as linhas tem mesmo valores.
 */
@Controller
@RequestMapping("/historico")
public class HistoricoController {

    private final HistoricoService service;

    public HistoricoController(HistoricoService service) {
        this.service = service;
    }

    /** Escolher o processo. */
    @GetMapping
    public String indice(Model model) {
        model.addAttribute("processos", service.descritores().values());
        return "historico/indice";
    }

    @GetMapping("/{chave}")
    public String doProcesso(@PathVariable String chave,
                             @RequestParam(required = false) String ano,
                             @RequestParam(required = false) String mes,
                             @RequestParam(required = false) String dia,
                             @RequestParam(required = false) String estado,
                             @RequestParam(required = false) String tipo,
                             @RequestParam(required = false) String adega,
                             @RequestParam(required = false) String vinho,
                             @RequestParam(required = false) String responsavel,
                             @RequestParam(required = false) String origem,
                             @RequestParam(required = false) String destino,
                             Model model) {
        Map<String, DescritorProcesso> todos = service.descritores();
        DescritorProcesso d = todos.get(chave);
        if (d == null) return "redirect:/historico";

        List<LinhaHistorico> todas = d.carregar().get();

        LocalDate diaEscolhido = data(dia);
        Integer mesEscolhido = diaEscolhido != null ? null : inteiro(mes);
        Integer anoEscolhido = diaEscolhido != null ? null : inteiro(ano);

        List<LinhaHistorico> linhas = new ArrayList<>();
        for (LinhaHistorico l : todas) {
            if (diaEscolhido != null) {
                if (l.data() == null || !l.data().equals(diaEscolhido)) continue;
            } else {
                if (anoEscolhido != null && (l.data() == null || l.data().getYear() != anoEscolhido)) continue;
                if (mesEscolhido != null && (l.data() == null || l.data().getMonthValue() != mesEscolhido)) continue;
            }
            if (naoBate(estado, l.estado())) continue;
            if (naoBate(tipo, l.tipo())) continue;
            if (naoBate(adega, l.adega())) continue;
            if (naoBate(vinho, l.vinho())) continue;
            if (naoBate(responsavel, l.responsavel())) continue;
            if (naoBate(origem, l.origem())) continue;
            if (naoBate(destino, l.destino())) continue;
            linhas.add(l);
        }

        model.addAttribute("processos", todos.values());
        model.addAttribute("d", d);
        model.addAttribute("linhas", linhas);
        model.addAttribute("totalGeral", todas.size());
        model.addAttribute("meses", MESES);
        model.addAttribute("anos", valoresDeAno(todas));
        // Opcoes de cada filtro: saem sempre da lista completa, nao da filtrada.
        model.addAttribute("opcoes", opcoes(todas));
        model.addAttribute("fAno", anoEscolhido);
        model.addAttribute("fMes", mesEscolhido);
        model.addAttribute("fDia", dia);
        model.addAttribute("fEstado", estado);
        model.addAttribute("fTipo", tipo);
        model.addAttribute("fAdega", adega);
        model.addAttribute("fVinho", vinho);
        model.addAttribute("fResponsavel", responsavel);
        model.addAttribute("fOrigem", origem);
        model.addAttribute("fDestino", destino);
        return "historico/processo";
    }

    // ----- auxiliares -----

    /** Um filtro vazio deixa passar tudo; caso contrario tem de ser igual. */
    private boolean naoBate(String filtro, String valor) {
        return filtro != null && !filtro.isBlank() && !filtro.equals(valor);
    }

    /**
     * Valores distintos de cada campo, para encher os seletores. Um campo sem
     * valor nenhum nao entra no mapa — e' assim que o ecra sabe que nem o
     * filtro nem a coluna desse campo devem aparecer neste processo.
     */
    private Map<String, List<String>> opcoes(List<LinhaHistorico> linhas) {
        Map<String, TreeSet<String>> juntos = new LinkedHashMap<>();
        for (LinhaHistorico l : linhas) {
            junta(juntos, "estado", l.estado());
            junta(juntos, "tipo", l.tipo());
            junta(juntos, "adega", l.adega());
            junta(juntos, "vinho", l.vinho());
            junta(juntos, "responsavel", l.responsavel());
            junta(juntos, "origem", l.origem());
            junta(juntos, "destino", l.destino());
            junta(juntos, "detalhe", l.detalhe());
        }
        Map<String, List<String>> out = new LinkedHashMap<>();
        juntos.forEach((k, v) -> out.put(k, new ArrayList<>(v)));
        return out;
    }

    private void junta(Map<String, TreeSet<String>> m, String chave, String valor) {
        if (valor == null || valor.isBlank()) return;
        m.computeIfAbsent(chave, k -> new TreeSet<>()).add(valor);
    }

    private List<Integer> valoresDeAno(List<LinhaHistorico> linhas) {
        TreeSet<Integer> anos = new TreeSet<>(Comparator.reverseOrder());
        for (LinhaHistorico l : linhas) if (l.data() != null) anos.add(l.data().getYear());
        return new ArrayList<>(anos);
    }

    private Integer inteiro(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.valueOf(s.trim()); } catch (Exception e) { return null; }
    }

    private LocalDate data(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }

    public record Mes(int numero, String nome) { }

    static final List<Mes> MESES = List.of(
            new Mes(1, "Janeiro"), new Mes(2, "Fevereiro"), new Mes(3, "Março"),
            new Mes(4, "Abril"), new Mes(5, "Maio"), new Mes(6, "Junho"),
            new Mes(7, "Julho"), new Mes(8, "Agosto"), new Mes(9, "Setembro"),
            new Mes(10, "Outubro"), new Mes(11, "Novembro"), new Mes(12, "Dezembro"));
}
