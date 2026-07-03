package pt.acv.adega.produtos;

/**
 * Estados do mosto ao longo da fermentacao (produtos 2.1 / 2.2 dos requisitos).
 */
public enum EstadoMosto {
    EM_FERMENTACAO("Mosto em fermentação"),
    ATESTADO("Mosto em fermentação (atestado)"),
    VINHO_GRANEL("Vinho pronto a granel");

    private final String descricao;
    EstadoMosto(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
