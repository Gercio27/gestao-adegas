package pt.acv.adega.processos;

/**
 * Um processo abre-se (ABERTO) e depois fecha-se (FECHADO). Ao fechar podem
 * despoletar-se efeitos automaticos (ex.: moagem gera mostos, engarrafamento
 * da baixa de vinho a granel).
 */
public enum EstadoProcesso {
    ABERTO("Aberto"),
    FECHADO("Fechado");

    private final String descricao;
    EstadoProcesso(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
