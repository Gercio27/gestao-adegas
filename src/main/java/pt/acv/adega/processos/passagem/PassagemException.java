package pt.acv.adega.processos.passagem;

/** Erro de negocio ao fechar/reabrir a passagem a vinho a granel. */
public class PassagemException extends RuntimeException {
    public PassagemException(String mensagem) { super(mensagem); }
}
