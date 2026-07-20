package pt.acv.adega.processos.saidacontentor;

/**
 * Motivo de saida de garrafas de um contentor (produto acabado).
 */
public enum MotivoSaidaContentor {
    CERTIFICACAO("Certificação"),
    PROVA("Prova"),
    RESERVA_ADEGA("Reserva da Adega"),
    PROMOCAO("Promoção"),
    TRANSFERENCIA_ENTRE_ADEGAS("Transferência entre adegas"),
    OUTRAS("Outras");

    private final String descricao;
    MotivoSaidaContentor(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
