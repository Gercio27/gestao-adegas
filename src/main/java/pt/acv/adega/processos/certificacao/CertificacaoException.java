package pt.acv.adega.processos.certificacao;

/** Erro de negocio ao fechar/reabrir a certificacao. */
public class CertificacaoException extends RuntimeException {
    public CertificacaoException(String mensagem) { super(mensagem); }
}
