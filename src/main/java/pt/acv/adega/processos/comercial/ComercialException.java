package pt.acv.adega.processos.comercial;

/** Erro de negocio ao fechar/reabrir a passagem ao comercial. */
public class ComercialException extends RuntimeException {
    public ComercialException(String mensagem) { super(mensagem); }
}
