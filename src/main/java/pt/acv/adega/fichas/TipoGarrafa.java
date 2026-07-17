package pt.acv.adega.fichas;

/**
 * Formato/tipo de garrafa que um contentor comporta.
 */
public enum TipoGarrafa {
    BORGONHESA("Borgonhesa"),
    BORDALESA("Bordalesa");

    private final String descricao;
    TipoGarrafa(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
