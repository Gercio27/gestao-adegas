package pt.acv.adega.processos.vindima;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma moagem que levou uva, e quantos Kg levou. Serve para duas coisas: o total
 * que cada moagem tirou a uma parcela, e — depois de repartido — o que cada
 * moagem tirou a uma colheita concreta.
 *
 * <p><b>Atencao:</b> quando representa o uso numa colheita, os Kg sao
 * <i>calculados</i>, nao registados. Ao moer escolhe-se a parcela e a
 * quantidade, nunca a colheita; a reparticao por colheita e' feita por ordem de
 * chegada (ver {@link VindimaController}).
 */
public record MoagemDaVindima(String codigo, LocalDate data, BigDecimal kg, boolean aberta) {
}
