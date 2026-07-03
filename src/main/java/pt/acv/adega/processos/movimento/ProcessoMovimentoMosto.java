package pt.acv.adega.processos.movimento;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;
import pt.acv.adega.produtos.Mosto;

import java.math.BigDecimal;

/**
 * Entrada (4.7) ou Saída/venda (4.8) de mosto, com documento de acompanhamento (DA).
 * ENTRADA: cria uma ficha de mosto num recipiente (controlo de capacidade).
 * SAIDA: da baixa de um mosto existente. Em ambos os casos emite o DA ao fechar.
 */
@Entity
@Table(name = "processo_movimento_mosto")
public class ProcessoMovimentoMosto extends Processo {

    public static final String PREFIXO = "MOV";
    public static final Fase FASE = Fase.FASE_4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimento tipo = TipoMovimento.ENTRADA;

    @Column(precision = 12, scale = 2)
    private BigDecimal litros;

    // --- ENTRADA: recipiente destino + casta ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "talha_destino_id")
    private Talha talhaDestino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deposito_destino_id")
    private Deposito depositoDestino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    // --- SAIDA: mosto de origem ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mosto_origem_id")
    private Mosto mostoOrigem;

    /** Fornecedor (entrada) ou cliente (saida). */
    @Column(length = 160)
    private String contraparte;

    @Column(length = 200)
    private String transporte;

    /** Numero do documento de acompanhamento, atribuido no fecho. */
    @Column(length = 20)
    private String numeroDA;

    @Transient
    private String destinoRef;

    public TipoMovimento getTipo() { return tipo; }
    public void setTipo(TipoMovimento tipo) { this.tipo = tipo; }

    public BigDecimal getLitros() { return litros; }
    public void setLitros(BigDecimal litros) { this.litros = litros; }

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
