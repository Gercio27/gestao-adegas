package pt.acv.adega.fichas;

/**
 * Tipos de consumivel geridos com stock (pontos 1.18/1.21-1.25 dos requisitos).
 * Cada tipo tem o seu prefixo de codigo automatico.
 */
public enum TipoConsumivel {
    GARRAFA("Garrafa / Bag-in-box / Garrafão", "GAR"),
    ROLHA("Rolha", "ROL"),
    ROTULO("Rótulo", "ROT"),
    CAPSULA("Cápsula", "CAP"),
    CAIXA("Caixa", "CAI"),
    ETIQUETA("Etiqueta", "ETQ");

    private final String descricao;
    private final String prefixo;

    TipoConsumivel(String descricao, String prefixo) {
        this.descricao = descricao;
        this.prefixo = prefixo;
    }

    public String getDescricao() { return descricao; }
    public String getPrefixo() { return prefixo; }
}
