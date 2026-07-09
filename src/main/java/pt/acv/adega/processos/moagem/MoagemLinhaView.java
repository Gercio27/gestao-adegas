package pt.acv.adega.processos.moagem;

import pt.acv.adega.planeamento.LinhaPlaneamentoParcela;
import pt.acv.adega.produtos.Mosto;

import java.util.List;

/**
 * Uma linha vindimada na folha da moagem: a linha do planeamento, a moagem
 * associada (se ja existir) e os mostos gerados (se fechada).
 */
public record MoagemLinhaView(LinhaPlaneamentoParcela linha, ProcessoMoagem moagem, List<Mosto> mostos) {
    public boolean temMoagem() { return moagem != null; }
    public boolean fechada() { return moagem != null && !moagem.isAberto(); }
}
