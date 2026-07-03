package pt.acv.adega.processos.rotulagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Consumivel;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;
import pt.acv.adega.produtos.VinhoEngarrafado;

/**
 * Processo de Rotulagem/embalamento (Fase 7). Aplica rotulos (e opcionalmente
 * capsulas e caixas) a um vinho engarrafado. Ao fechar, da baixa desses
 * consumiveis e marca o vinho como rotulado (produto acabado embalado).
 */
@Entity
@Table(name = "processo_rotulagem")
public class ProcessoRotulagem extends Processo {

    public static final String PREFIXO = "RTL";
    public static final Fase FASE = Fase.FASE_7;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "engarrafado_id")
    private VinhoEngarrafado engarrafado;

    @Column(nullable = false)
    private int numeroGarrafas;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rotulo_id")
    private Consumivel rotulo;

    @Column(nullable = false)
    private int numeroRotulos;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "capsula_id")
    private Consumivel capsula;

    @Column(nullable = false)
    private int numeroCapsulas;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "caixa_id")
    private Consumivel caixa;

    @Column(nullable = false)
    private int numeroCaixas;

    public VinhoEngarrafado getEngarrafado() { return engarrafado; }
    public void setEngarrafado(VinhoEngarrafado engarrafado) { this.engarrafado = engarrafado; }

    public int getNumeroGarrafas() { return numeroGarrafas; }
    public void setNumeroGarrafas(int numeroGarrafas) { this.numeroGarrafas = numeroGarrafas; }

    public Consumivel getRotulo() { return rotulo; }
    public void setRotulo(Consumivel rotulo) { this.rotulo = rotulo; }

    public int getNumeroRotulos() { return numeroRotulos; }
    public void setNumeroRotulos(int numeroRotulos) { this.numeroRotulos = numeroRotulos; }

    public Consumivel getCapsula() { return capsula; }
    public void setCapsula(Consumivel capsula) { this.capsula = capsula; }

    public int getNumeroCapsulas() { return numeroCapsulas; }
    public void setNumeroCapsulas(int numeroCapsulas) { this.numeroCapsulas = numeroCapsulas; }

    public Consumivel getCaixa() { return caixa; }
    public void setCaixa(Consumivel caixa) { this.caixa = caixa; }

    public int getNumeroCaixas() { return numeroCaixas; }
    public void setNumeroCaixas(int numeroCaixas) { this.numeroCaixas = numeroCaixas; }
}
