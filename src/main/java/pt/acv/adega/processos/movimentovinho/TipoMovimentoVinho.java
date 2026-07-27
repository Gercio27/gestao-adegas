package pt.acv.adega.processos.movimentovinho;

public enum TipoMovimentoVinho {
    ENTRADA("Entrada de vinho a granel (externa)"),
    SAIDA("Saída / venda de vinho a granel"),
    TRANSFEGA("Transfega (mudar de depósito, mesmo vinho)"),
    /**
     * Saida intra-empresa: transferencia interna entre adegas/armazens.
     * NOTA: o nome tem de caber na coluna varchar(10) ja existente na base de
     * dados (ver ProcessoMovimentoVinho.tipo) - por isso e' INTRA_EMP.
     */
    INTRA_EMP("Saída Intra-Empresa (entre adegas/armazéns)");

    private final String descricao;
    TipoMovimentoVinho(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
