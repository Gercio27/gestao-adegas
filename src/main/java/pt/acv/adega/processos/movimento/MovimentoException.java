package pt.acv.adega.processos.movimento;

/** Erro de negocio ao fechar/reabrir um movimento de mosto. */
public class MovimentoException extends RuntimeException {
    public MovimentoException(String mensagem) { super(mensagem); }
}
