package pt.acv.adega.fichas;

import java.math.BigDecimal;

/**
 * Formatos de bag-in-box usados na adega. Cada formato e' definido pelos
 * litros que a bolsa leva - ao contrario das garrafas, aqui o que interessa
 * para as contas de stock e' o volume por unidade.
 */
public enum TipoBagInBox {
    BIB_3("Bag-in-box 3 L", "3"),
    BIB_5("Bag-in-box 5 L", "5"),
    BIB_10("Bag-in-box 10 L", "10"),
    BIB_20("Bag-in-box 20 L", "20");

    private final String descricao;
    private final String litros;

    TipoBagInBox(String descricao, String litros) {
        this.descricao = descricao;
        this.litros = litros;
    }

    public String getDescricao() { return descricao; }

    /** Litros de vinho em cada unidade deste formato. */
    public BigDecimal getLitrosPorUnidade() { return new BigDecimal(litros); }
}
