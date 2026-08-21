package pt.acv.adega.processos.vindima;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma moagem que levou uva de uma parcela vindimada, e quantos Kg levou.
 *
 * <p>Os Kg moidos sao registados por <b>parcela</b> (linha do planeamento), nao
 * por colheita: quando se moi, escolhe-se a parcela e a quantidade, nao a
 * colheita concreta. Por isso este resumo aparece no bloco da parcela e nao em
 * cada linha do historico de colheitas.
 */
public record MoagemDaVindima(String codigo, LocalDate data, BigDecimal kg, boolean aberta) {
}
