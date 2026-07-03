package pt.acv.adega.web;

import pt.acv.adega.fichas.Consumivel;

import java.math.BigDecimal;
import java.util.List;

/** Dados agregados para o painel inicial. */
public record PainelDados(
        long numCastas, long numVinhas, long numTrabalhadores, long numAdegas,
        long numTalhas, long numDepositos, long numConsumiveis,
        long talhasOcupadas, long talhasVazias,
        long depositosOcupados, long depositosVazios,
        BigDecimal litrosMostoFermentacao, BigDecimal litrosVinhoGranel,
        long garrafasEngarrafadas, long garrafasDisponiveis,
        long processosAbertos,
        List<Consumivel> consumiveisAlerta
) {
    public long recipientesCheios() { return talhasOcupadas + depositosOcupados; }
    public long recipientesVazios() { return talhasVazias + depositosVazios; }
    public boolean temAlertas() { return !consumiveisAlerta.isEmpty(); }
}
