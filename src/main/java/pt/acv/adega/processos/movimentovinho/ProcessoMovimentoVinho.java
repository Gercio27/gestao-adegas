package pt.acv.adega.processos.movimentovinho;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;
import pt.acv.adega.produtos.Mosto;

import java.math.BigDecimal;

/**
 * Fase 5 - Entradas, saidas e transfegas de vinho a granel.
 * ENTRADA: cria vinho a granel num recipiente (externo, emite DA).
 * SAIDA: da baixa de vinho a granel existente (emite DA).
 * TRANSFEGA: move litros de um recipiente para outro dentro do mesmo vinho.
 */
@Entity
@Table(name = "processo_movimento_vinho")
public class ProcessoMovimentoVinho extends Processo {

    public static final String PREFIXO = "MVG";
    public static final Fase FASE = Fase.FASE_5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentoVinho tipo = TipoMovimentoVinho.TRANSFEGA;

    @Column(precision = 12, scale = 2)
    private BigDecimal litros;

    /** Nome do vinho (ENTRADA externa; nas outras vem do mosto de origem). */
    @Column(length = 160)
    private String nomeVinho;

    // --- Destino (ENTRADA e TRANSFEGA) ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "talha_destino_id")
    private Talha talhaDestino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deposito_destino_id")
    private Deposito depositoDestino;

    /** Casta (ENTRADA externa). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    // --- Origem (SAIDA e TRANSFEGA) ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mosto_origem_id")
    private Mosto mostoOrigem;

    @Column(length = 160)
    private String contraparte;

    @Column(length = 200)
    private String transporte;

    @Column(length = 20)
    private String numeroDA;

    /** TRANSFEGA: id do mosto de destino afetado (para reverter). */
    @Column(name = "mosto_destino_id")
    private Long mostoDestinoId;

    /** TRANSFEGA: o mosto de destino foi criado por esta transfega (para reverter). */
    @Column(nullable = false)
    private boolean destinoCriado = false;

    @Transient
    private String destinoRef;

    public Long getMostoDestinoId() { return mostoDestinoId; }
    public void setMostoDestinoId(Long mostoDestinoId) { this.mostoDestinoId = mostoDestinoId; }

    public boolean isDestinoCriado() { return destinoCriado; }
    public void setDestinoCriado(boolean destinoCriado) { this.destinoCriado = destinoCriado; }

    public TipoMovimentoVinho getTipo() { return tipo; }
    public void setTipo(TipoMovimentoVinho tipo) { this.tipo = tipo; }

    public BigDecimal getLitros() { return litros; }
    public void setLitros(BigDecimal litros) { this.litros = litros; }

    public String getNomeVinho() { return nomeVinho; }
    public void setNomeVinho(String nomeVinho) { this.nomeVinho = nomeVinho; }

    public Talha getTalhaDestino() { return talhaDestino; }
    public void setTalhaDestino(Talha talhaDestino) { this.talhaDestino = talhaDestino; }

    public Deposito getDepositoDestino() { return depositoDestino; }
    public void setDepositoDestino(Deposito depositoDestino) { this.depositoDestino = depositoDestino; }

    public Casta getCasta() { return casta; }
    public void setCasta(Casta casta) { this.casta = casta; }

    public Mosto getMostoOrigem() { return mostoOrigem; }
    public void setMostoOrigem(Mosto mostoOrigem) { this.mostoOrigem = mostoOrigem; }

    public String getContraparte() { return contraparte; }
    public void setContraparte(String contraparte) { this.contraparte = contraparte; }

    public String getTransporte() { return transporte; }
    public void setTransporte(String transporte) { this.transporte = transporte; }

    public String getNumeroDA() { return numeroDA; }
    public void setNumeroDA(String numeroDA) { this.numeroDA = numeroDA; }

    public String getDestinoRef() {
        if (destinoRef != null) return destinoRef;
        if (talhaDestino != null) return "TALHA:" + talhaDestino.getId();
        if (depositoDestino != null) return "DEPOSITO:" + depositoDestino.getId();
        return "";
    }
    public void setDestinoRef(String destinoRef) { this.destinoRef = destinoRef; }

    @Transient
    public String getDestinoDescricao() {
        if (talhaDestino != null) return "Talha " + talhaDestino.getIdentificacao();
        if (depositoDestino != null) return "Depósito " + depositoDestino.getIdentificacao();
        return "—";
    }
}
