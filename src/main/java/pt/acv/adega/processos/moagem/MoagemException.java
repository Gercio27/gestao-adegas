package pt.acv.adega.processos.moagem;

/** Erro de negocio ao fechar/reabrir uma moagem (ex.: capacidade excedida). */
public class MoagemException extends RuntimeException {
    public MoagemException(String mensagem) {
        super(mensagem);
    }
}
