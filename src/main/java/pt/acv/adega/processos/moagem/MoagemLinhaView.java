package pt.acv.adega.processos.moagem;

import pt.acv.adega.planeamento.LinhaPlaneamentoParcela;
import pt.acv.adega.produtos.Mosto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Uma linha vindimada na folha da moagem: a linha do planeamento, a moagem
 * associada (se ja existir) e os mostos gerados (se fechada).
 */
public record MoagemLinhaView(LinhaPlaneamentoParcela linha, ProcessoMoagem moagem, List<Mosto> mostos) {
    public boolean temMoagem() { return moagem != null; }
    public boolean fechada() { return moagem != null && !moagem.isAberto(); }

    /** Kg efetivamente moidos nesta linha (0 se ainda nao ha moagem). */
    public BigDecimal moidoKg() {
        return moagem != null ? moagem.getTotalMoidoKg() : BigDecimal.ZERO;
    }

    /** Kg vindimados que ainda faltam moer (vindimado - moido). */
    public BigDecimal faltaMoer() {
        return linha.getTotalVindimadoKg().subtract(moidoKg());
    }

    /** true quando ja se moeu e ainda sobrou uva por moer. */
    public boolean sobrouPorMoer() {
        return moidoKg().signum() > 0 && faltaMoer().signum() > 0;
    }
}
