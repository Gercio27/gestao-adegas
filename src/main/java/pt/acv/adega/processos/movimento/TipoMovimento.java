package pt.acv.adega.processos.movimento;

public enum TipoMovimento {
    ENTRADA("Entrada de mosto (externa)"),
    SAIDA("Saída / venda de mosto");

    private final String descricao;
    TipoMovimento(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
