package pt.acv.adega.processos.rotulagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Armazem;
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

    /** Garrafas rotuladas = caixas x garrafas por caixa. */
    @Column(nullable = false)
    private int numeroGarrafas;

    /**
     * Onde e' feita a rotulagem. As garrafas saem dos contentores desta adega
     * ou deste armazem.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "armazem_id")
    private Armazem armazem;

    /** Caixas cheias nesta rotulagem (as garrafas saem do contentor para aqui). */
    @Column(nullable = false)
    private int caixasRotuladas;

    /** Quantas garrafas leva cada caixa (normalmente 6). */
    @Column(nullable = false)
    private int garrafasPorCaixa = 6;

    /** De que contentores sairam as garrafas: "id:qtd;id:qtd" (para reverter). */
    @Column(name = "saida_contentores", length = 1000)
    private String saidaContentores;

    @Transient
    private String localRef;

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

    /** Etiquetas (contra-rotulo, selo, etc.) aplicadas nesta rotulagem. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "etiqueta_id")
    private Consumivel etiqueta;

    @Column(nullable = false)
    private int numeroEtiquetas;

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

    public Consumivel getEtiqueta() { return etiqueta; }
    public void setEtiqueta(Consumivel etiqueta) { this.etiqueta = etiqueta; }

    public int getNumeroEtiquetas() { return numeroEtiquetas; }
    public void setNumeroEtiquetas(int numeroEtiquetas) { this.numeroEtiquetas = numeroEtiquetas; }

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }

    public int getCaixasRotuladas() { return caixasRotuladas; }
    public void setCaixasRotuladas(int caixasRotuladas) { this.caixasRotuladas = caixasRotuladas; }

    public int getGarrafasPorCaixa() { return garrafasPorCaixa; }
    public void setGarrafasPorCaixa(int garrafasPorCaixa) { this.garrafasPorCaixa = garrafasPorCaixa; }

    public String getSaidaContentores() { return saidaContentores; }
    public void setSaidaContentores(String saidaContentores) { this.saidaContentores = saidaContentores; }

    public String getLocalRef() {
        if (localRef != null) return localRef;
        if (adega != null) return "ADEGA:" + adega.getId();
        if (armazem != null) return "ARMAZEM:" + armazem.getId();
        return "";
    }
    public void setLocalRef(String localRef) { this.localRef = localRef; }

    @Transient
    public String getLocalNome() {
        if (adega != null) return "Adega " + adega.getNome();
        if (armazem != null) return "Armazém " + armazem.getNome();
        return "—";
    }
}
