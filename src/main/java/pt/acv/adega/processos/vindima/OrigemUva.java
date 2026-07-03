package pt.acv.adega.processos.vindima;

/**
 * Os quatro casos de origem da uva previstos no requisito 2.3 da Vindima.
 * A origem determina que informacao e obrigatoria (terceiro, transporte).
 */
public enum OrigemUva {
    PROPRIA("Vinha própria (vindimada por nós)"),
    TERCEIRO_VINDIMADA_POR_NOS("Vinha de terceiros, vindimada por nós"),
    ADQUIRIDA_TRANSPORTE_NOSSO("Uva adquirida, transportada por nós"),
    ADQUIRIDA_TRANSPORTE_TERCEIRO("Uva adquirida, transportada pelo terceiro");

    private final String descricao;
    OrigemUva(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }

    /** True quando ha um terceiro envolvido (todas menos a vinha propria). */
    public boolean envolveTerceiro() { return this != PROPRIA; }
}
