package pt.acv.adega.planeamento;

import pt.acv.adega.processos.maturacao.ProcessoAnaliseMaturacao;

import java.math.BigDecimal;

/**
 * Linha do mapa de planeamento: a linha (parcela + Kg a aplicar), o saldo atual
 * da parcela e a analise a maturacao mais recente da vinha/casta dessa parcela.
 *
 * <p>A massa volumica e o pH podem estar escritos a mao na propria linha do
 * planeamento. Quando estao, e' esse o valor que conta; caso contrario mostra-se
 * o da analise a maturacao.
 */
public record LinhaMapa(LinhaPlaneamentoParcela linha, BigDecimal saldo, ProcessoAnaliseMaturacao analise) {

    public boolean temAnalise() { return analise != null; }

    /** Massa volumica a mostrar: a escrita a mao, ou a da analise a maturacao. */
    public BigDecimal getMassaVolumica() {
        if (linha != null && linha.getMassaVolumica() != null) return linha.getMassaVolumica();
        return analise != null ? analise.getAcucar() : null;
    }

    /** pH a mostrar: o escrito a mao, ou o da analise a maturacao. */
    public BigDecimal getPh() {
        if (linha != null && linha.getPh() != null) return linha.getPh();
        return analise != null ? analise.getPh() : null;
    }

    /** true quando o valor mostrado foi escrito a mao (e nao veio da analise). */
    public boolean isMassaVolumicaManual() {
        return linha != null && linha.getMassaVolumica() != null;
    }

    public boolean isPhManual() {
        return linha != null && linha.getPh() != null;
    }
}
