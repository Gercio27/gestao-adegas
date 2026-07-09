package pt.acv.adega.planeamento;

import java.util.List;

/** Um vinho planeado e as suas linhas (parcelas) para o mapa de planeamento. */
public record VinhoMapa(PlaneamentoVinho plano, List<LinhaMapa> linhas) {
}
