package pt.acv.adega.processos.engarrafamento;

/** Erro de negocio ao fechar/reabrir o engarrafamento (stock/vinho insuficiente...). */
public class EngarrafamentoException extends RuntimeException {

    /** True quando e apenas um aviso de capacidade excedida (o utilizador pode forcar o fecho). */
    private final boolean capacidade;

    public EngarrafamentoException(String mensagem) { this(mensagem, false); }

    public EngarrafamentoException(String mensagem, boolean capacidade) {
        super(mensagem);
        this.capacidade = capacidade;
    }

    public boolean isCapacidade() { return capacidade; }
}
