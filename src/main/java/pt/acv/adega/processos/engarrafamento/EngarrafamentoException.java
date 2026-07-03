package pt.acv.adega.processos.engarrafamento;

/** Erro de negocio ao fechar/reabrir o engarrafamento (stock/vinho insuficiente...). */
public class EngarrafamentoException extends RuntimeException {
    public EngarrafamentoException(String mensagem) { super(mensagem); }
}
