package pt.acv.adega.processos.movimentovinho;

public enum TipoMovimentoVinho {
    ENTRADA("Entrada de vinho a granel (externa)"),
    SAIDA("Saída / venda de vinho a granel"),
    TRANSFEGA("Transfega (mudar de depósito, mesmo vinho)");

    private final String descricao;
    TipoMovimentoVinho(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
