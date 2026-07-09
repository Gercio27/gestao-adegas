package pt.acv.adega.planeamento;

import pt.acv.adega.processos.maturacao.ProcessoAnaliseMaturacao;

import java.math.BigDecimal;

/**
 * Linha do mapa de planeamento: a linha (parcela + Kg a aplicar), o saldo atual
 * da parcela e a analise a maturacao mais recente da vinha/casta dessa parcela.
 */
public record LinhaMapa(LinhaPlaneamentoParcela linha, BigDecimal saldo, ProcessoAnaliseMaturacao analise) {
    public boolean temAnalise() { return analise != null; }
}
