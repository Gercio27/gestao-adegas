package pt.acv.adega.processos.maturacao;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.fichas.Vinha;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;

import java.math.BigDecimal;

/**
 * Análise à maturação da uva (Fase 1, ponto 1.1). Preenche um boletim de
 * análise por vinha e casta, com os meios/métodos usados (refratómetro, prova,
 * análise laboratorial) e os resultados obtidos.
 */
@Entity
@Table(name = "processo_analise_maturacao")
public class ProcessoAnaliseMaturacao extends Processo {

    public static final String PREFIXO = "AMT";
    public static final Fase FASE = Fase.FASE_1;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vinha_id")
    private Vinha vinha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    /** Álcool provável (% vol). */
    @Column(precision = 5, scale = 2)
    private BigDecimal grauProvavel;

    /** Açúcar (g/L). */
    @Column(precision = 7, scale = 2)
    private BigDecimal acucar;

    /** Acidez total (g/L ác. tartárico). */
    @Column(precision = 6, scale = 2)
    private BigDecimal acidezTotal;

    @Column(precision = 4, scale = 2)
    private BigDecimal ph;

    public Vinha getVinha() { return vinha; }
    public void setVinha(Vinha vinha) { this.vinha = vinha; }

    public Casta getCasta() { return casta; }
    public void setCasta(Casta casta) { this.casta = casta; }

    public BigDecimal getGrauProvavel() { return grauProvavel; }
    public void setGrauProvavel(BigDecimal grauProvavel) { this.grauProvavel = grauProvavel; }

    public BigDecimal getAcucar() { return acucar; }
    public void setAcucar(BigDecimal acucar) { this.acucar = acucar; }

    public BigDecimal getAcidezTotal() { return acidezTotal; }
    public void setAcidezTotal(BigDecimal acidezTotal) { this.acidezTotal = acidezTotal; }

    public BigDecimal getPh() { return ph; }
    public void setPh(BigDecimal ph) { this.ph = ph; }
}
