package pt.acv.adega.fichas;

/**
 * Em que e' que o vinho acabado esta embalado: garrafas (contentores de
 * garrafas) ou bag-in-box (contentores/paletes de bag-in-box). Usado por todas
 * as fases que mexem em produto acabado, para saberem em que ficha ir buscar o
 * stock.
 */
public enum TipoEmbalagem {
    GARRAFA("Garrafas", "garrafa(s)"),
    BAG_IN_BOX("Bag-in-box", "unidade(s)");

    private final String descricao;
    private final String unidade;

    TipoEmbalagem(String descricao, String unidade) {
        this.descricao = descricao;
        this.unidade = unidade;
    }

    public String getDescricao() { return descricao; }

    /** Como se chamam as unidades desta embalagem, para os textos dos ecras. */
    public String getUnidade() { return unidade; }
}
