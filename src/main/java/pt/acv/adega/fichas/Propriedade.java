package pt.acv.adega.fichas;

/**
 * Distingue bens proprios da organizacao de bens de terceiros. Exigido nos
 * requisitos para vinhas, depositos/talhas e produtos (proprio vs terceiros).
 */
public enum Propriedade {
    PROPRIO("Próprio"),
    TERCEIRO("De terceiros");

    private final String descricao;
    Propriedade(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
