package pt.acv.adega.processos.atesto;

/** Erro de negocio ao fechar/reabrir um atesto (capacidade, origem insuficiente...). */
public class AtestoException extends RuntimeException {
    public AtestoException(String mensagem) { super(mensagem); }
}
