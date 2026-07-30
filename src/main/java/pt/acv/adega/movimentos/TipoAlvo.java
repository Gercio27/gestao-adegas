package pt.acv.adega.movimentos;

/** Sobre que ficha e' que o movimento aconteceu. */
public enum TipoAlvo {
    TALHA("Talha", "L"),
    DEPOSITO("Depósito / cuba", "L"),
    CONTENTOR_GARRAFAS("Contentor de garrafas", "garrafas"),
    PALETE_BIB("Palete bag-in-box", "unidades"),
    CONSUMIVEL("Consumível", "un"),
    STOCK_ROTULADO("Vinho rotulado por entregar", "garrafas");

    private final String descricao;
    private final String unidade;

    TipoAlvo(String descricao, String unidade) {
        this.descricao = descricao;
        this.unidade = unidade;
    }

    public String getDescricao() { return descricao; }
    public String getUnidade() { return unidade; }
}
