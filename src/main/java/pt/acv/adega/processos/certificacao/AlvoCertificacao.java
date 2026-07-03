package pt.acv.adega.processos.certificacao;

public enum AlvoCertificacao {
    GRANEL("Vinho a granel"),
    ENGARRAFADO("Vinho engarrafado");

    private final String descricao;
    AlvoCertificacao(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
