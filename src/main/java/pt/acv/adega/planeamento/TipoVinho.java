package pt.acv.adega.planeamento;

/** Tipo de vinho a produzir no planeamento. */
public enum TipoVinho {
    BRANCO("Branco"),
    TINTO("Tinto"),
    ESPUMANTE("Espumante");

    private final String descricao;

    TipoVinho(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }
}
