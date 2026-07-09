package pt.acv.adega.processos.moagem;

import pt.acv.adega.planeamento.PlaneamentoVinho;

import java.util.List;

/** Um vinho e as suas linhas vindimadas, para a folha da moagem. */
public record MoagemVinhoView(PlaneamentoVinho plano, List<MoagemLinhaView> linhas) {
}
