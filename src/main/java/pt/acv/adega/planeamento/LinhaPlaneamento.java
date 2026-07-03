package pt.acv.adega.planeamento;

import pt.acv.adega.processos.maturacao.ProcessoAnaliseMaturacao;

/** Linha do mapa de planeamento: o plano + a analise a maturacao mais recente. */
public record LinhaPlaneamento(PlaneamentoVinho plano, ProcessoAnaliseMaturacao analise) {
    public boolean temAnalise() { return analise != null; }
}
