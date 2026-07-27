package pt.acv.adega.processos.engarrafamento;

/**
 * Em que e' que o vinho e' embalado neste processo. Decide para onde vao as
 * unidades no fim: contentores de garrafas ou contentores/paletes de
 * bag-in-box.
 */
public enum TipoEmbalagem {
    GARRAFA("Garrafas"),
    BAG_IN_BOX("Bag-in-box");

    private final String descricao;
    TipoEmbalagem(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
