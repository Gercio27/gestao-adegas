package pt.acv.adega.processos.rotulagem;

/** Erro de negocio ao fechar/reabrir a rotulagem (stock insuficiente...). */
public class RotulagemException extends RuntimeException {
    public RotulagemException(String mensagem) { super(mensagem); }
}
