package pt.acv.adega.processos;

/**
 * As 8 fases cronologicas do ciclo produtivo (da vinha ao produto acabado).
 */
public enum Fase {
    FASE_1("Fase 1", "Análise à maturação e planeamento"),
    FASE_2("Fase 2", "Vindima"),
    FASE_3("Fase 3", "Moagem e enchimento de talhas/cubas"),
    FASE_4("Fase 4", "Acompanhamento da fermentação"),
    FASE_5("Fase 5", "Vinho pronto / estágio"),
    FASE_6("Fase 6", "Engarrafamento / enrolhamento"),
    FASE_7("Fase 7", "Rotulagem / embalamento"),
    FASE_8("Fase 8", "Passagem ao setor comercial");

    private final String numero;
    private final String descricao;

    Fase(String numero, String descricao) {
        this.numero = numero;
        this.descricao = descricao;
    }

    public String getNumero() { return numero; }
    public String getDescricao() { return descricao; }
}
