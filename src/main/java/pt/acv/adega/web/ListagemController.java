package pt.acv.adega.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pt.acv.adega.fichas.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Output/Listagem (ponto 3 dos requisitos): consultar as variaveis/fichas por
 * intervalo de datas, com a informacao registada nesse intervalo.
 */
@Controller
@RequestMapping("/listagens")
public class ListagemController {

    private final CastaRepository castaRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final AdegaRepository adegaRepo;
    private final VinhaRepository vinhaRepo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;

    public ListagemController(CastaRepository castaRepo, TrabalhadorRepository trabalhadorRepo,
                              AdegaRepository adegaRepo, VinhaRepository vinhaRepo,
                              TalhaRepository talhaRepo, DepositoRepository depositoRepo) {
        this.castaRepo = castaRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.adegaRepo = adegaRepo;
        this.vinhaRepo = vinhaRepo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
    }

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "CASTAS") String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model) {

        if (inicio == null) inicio = LocalDate.now().minusMonths(12);
        if (fim == null) fim = LocalDate.now();

        LocalDateTime de = inicio.atStartOfDay();
        LocalDateTime ate = fim.atTime(LocalTime.MAX);

        List<LinhaListagem> linhas = new ArrayList<>();
        switch (tipo) {
            case "TRABALHADORES" -> trabalhadorRepo.findByDataCriacaoBetweenOrderByDataCriacaoAsc(de, ate)
                    .forEach(t -> linhas.add(new LinhaListagem(t.getCodigo(), t.getNome(),
                            t.getFuncao(), t.getDataCriacao())));
            case "ADEGAS" -> adegaRepo.findByDataCriacaoBetweenOrderByDataCriacaoAsc(de, ate)
                    .forEach(a -> linhas.add(new LinhaListagem(a.getCodigo(), a.getNome(),
                            a.getLocalizacao(), a.getDataCriacao())));
            case "VINHAS" -> vinhaRepo.findByDataCriacaoBetweenOrderByDataCriacaoAsc(de, ate)
                    .forEach(v -> linhas.add(new LinhaListagem(v.getCodigo(), v.getNome(),
                            v.getAreaTotal() + " ha", v.getDataCriacao())));
            case "TALHAS" -> talhaRepo.findByDataCriacaoBetweenOrderByDataCriacaoAsc(de, ate)
                    .forEach(t -> linhas.add(new LinhaListagem(t.getCodigo(), t.getIdentificacao(),
                            (t.getCapacidadeLitros() != null ? t.getCapacidadeLitros() + " L" : ""),
                            t.getDataCriacao())));
            case "DEPOSITOS" -> depositoRepo.findByDataCriacaoBetweenOrderByDataCriacaoAsc(de, ate)
                    .forEach(d -> linhas.add(new LinhaListagem(d.getCodigo(), d.getIdentificacao(),
                            (d.getCapacidadeLitros() != null ? d.getCapacidadeLitros() + " L" : ""),
                            d.getDataCriacao())));
            default -> castaRepo.findByDataCriacaoBetweenOrderByDataCriacaoAsc(de, ate)
                    .forEach(c -> linhas.add(new LinhaListagem(c.getCodigo(), c.getNome(),
                            c.getCor() != null ? c.getCor().getDescricao() : "", c.getDataCriacao())));
        }

        model.addAttribute("tipo", tipo);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("linhas", linhas);
        return "listagens/index";
    }
}
