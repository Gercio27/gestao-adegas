package pt.acv.adega.tratamentos;

/**
 * Categoria do vinho a intervencionar (tratamento enológico / análise): pode
 * estar em fase de mosto (fermentação) ou já como vinho pronto a granel.
 */
public enum CategoriaVinho {
    MOSTO("Mosto (em fermentação)"),
    GRANEL("Vinho a granel");

    private final String descricao;
    CategoriaVinho(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
