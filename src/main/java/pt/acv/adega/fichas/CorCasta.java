package pt.acv.adega.fichas;

public enum CorCasta {
    TINTA("Tinta"),
    BRANCA("Branca");

    private final String descricao;
    CorCasta(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
