package pt.acv.adega.fichas;

/**
 * Formato/tipo de garrafa que um contentor comporta.
 *
 * Cada formato tem um MÁXIMO FIXO de garrafas por contentor (informação de
 * referência): a bordalesa é mais esguia, cabem mais; a borgonhesa é mais
 * bojuda, cabem menos. É permitido ultrapassar este máximo — o sistema apenas
 * informa quanto falta ou quanto excede.
 *
 * NOTA para o cliente: confirmar/ajustar estes dois valores aos reais.
 */
public enum TipoGarrafa {
    BORGONHESA("Borgonhesa", 500),
    BORDALESA("Bordalesa", 600);

    private final String descricao;
    private final int maximoGarrafas;

    TipoGarrafa(String descricao, int maximoGarrafas) {
        this.descricao = descricao;
        this.maximoGarrafas = maximoGarrafas;
    }

    public String getDescricao() { return descricao; }

    /** Máximo fixo de garrafas deste formato por contentor (referência). */
    public int getMaximoGarrafas() { return maximoGarrafas; }
}
