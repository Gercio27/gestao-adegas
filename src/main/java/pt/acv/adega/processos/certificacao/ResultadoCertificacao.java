package pt.acv.adega.processos.certificacao;

public enum ResultadoCertificacao {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    REPROVADO("Reprovado");

    private final String descricao;
    ResultadoCertificacao(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
