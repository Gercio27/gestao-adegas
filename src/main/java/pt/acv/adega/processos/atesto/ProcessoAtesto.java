package pt.acv.adega.processos.atesto;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;

import java.math.BigDecimal;

/**
 * Processo de Atesto (Fase 4, ponto 4.2). Transfere mosto de um recipiente
 * (origem) para outro (destino). Ao fechar, aplica o controlo de capacidade
 * nos dois lados: o destino nao pode passar a sua capacidade e a origem nao
 * pode ficar a dar mais do que tem.
 */
@Entity
@Table(name = "processo_atesto")
public class ProcessoAtesto extends Processo {

    public static final String PREFIXO = "ATE";
    public static final Fase FASE = Fase.FASE_4;

    /** Adega do recipiente atestado (destino); ajuda a filtrar os recipientes. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    // --- Origem (de onde sai o mosto) ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "talha_origem_id")
    private Talha talhaOrigem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deposito_origem_id")
    private Deposito depositoOrigem;

    // --- Destino (recipiente atestado) ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "talha_destino_id")
    private Talha talhaDestino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deposito_destino_id")
    private Deposito depositoDestino;

    @Column(precision = 12, scale = 2)
    private BigDecimal litros;

    /** Referencias vindas do formulario ("TALHA:id"/"DEPOSITO:id"). Nao persistem. */
    @Transient
    private String origemRef;
    @Transient
    private String destinoRef;

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public Talha getTalhaOrigem() { return talhaOrigem; }
    public void setTalhaOrigem(Talha talhaOrigem) { this.talhaOrigem = talhaOrigem; }

    public Deposito getDepositoOrigem() { return depositoOrigem; }
    public void setDepositoOrigem(Deposito depositoOrigem) { this.depositoOrigem = depositoOrigem; }

    public Talha getTalhaDestino() { return talhaDestino; }
    public void setTalhaDestino(Talha talhaDestino) { this.talhaDestino = talhaDestino; }

    public Deposito getDepositoDestino() { return depositoDestino; }
    public void setDepositoDestino(Deposito depositoDestino) { this.depositoDestino = depositoDestino; }

    public BigDecimal getLitros() { return litros; }
    public void setLitros(BigDecimal litros) { this.litros = litros; }

    public String getOrigemRef() {
        if (origemRef != null) return origemRef;
        if (talhaOrigem != null) return "TALHA:" + talhaOrigem.getId();
        if (depositoOrigem != null) return "DEPOSITO:" + depositoOrigem.getId();
        return "";
    }
    public void setOrigemRef(String origemRef) { this.origemRef = origemRef; }

    public String getDestinoRef() {
        if (destinoRef != null) return destinoRef;
        if (talhaDestino != null) return "TALHA:" + talhaDestino.getId();
        if (depositoDestino != null) return "DEPOSITO:" + depositoDestino.getId();
        return "";
    }
    public void setDestinoRef(String destinoRef) { this.destinoRef = destinoRef; }

    @Transient
    public String getOrigemDescricao() {
        if (talhaOrigem != null) return "Talha " + talhaOrigem.getIdentificacao();
        if (depositoOrigem != null) return "Depósito " + depositoOrigem.getIdentificacao();
        return "—";
    }

    @Transient
    public String getDestinoDescricao() {
        if (talhaDestino != null) return "Talha " + talhaDestino.getIdentificacao();
        if (depositoDestino != null) return "Depósito " + depositoDestino.getIdentificacao();
        return "—";
    }
}
