package pt.acv.adega.fichas;

public enum CorCasta {
    TINTA("Tinta"),
    BRANCA("Branca"),
    MOSCATEL("Moscatel");

    private final String descricao;
    CorCasta(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
