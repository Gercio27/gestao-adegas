package pt.acv.adega.processos.moagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.Talha;

import java.math.BigDecimal;

/**
 * Linha de enchimento de uma moagem: que talha/cuba se encheu, com quantos
 * litros de mosto e de que casta. Cada linha origina uma ficha de mosto ao
 * fechar o processo.
 */
@Entity
@Table(name = "enchimento")
public class Enchimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moagem_id")
    private ProcessoMoagem moagem;

    /** Recipiente: uma talha OU um deposito/cuba. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "talha_id")
    private Talha talha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deposito_id")
    private Deposito deposito;

    @Column(precision = 12, scale = 2)
    private BigDecimal litros;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    /**
     * Referencia do recipiente vinda do formulario, no formato "TALHA:id" ou
     * "DEPOSITO:id". Nao persiste; e resolvida para talha/deposito no controlador.
     */
    @Transient
    private String recipienteRef;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProcessoMoagem getMoagem() { return moagem; }
    public void setMoagem(ProcessoMoagem moagem) { this.moagem = moagem; }

    public Talha getTalha() { return talha; }
    public void setTalha(Talha talha) { this.talha = talha; }

    public Deposito getDeposito() { return deposito; }
    public void setDeposito(Deposito deposito) { this.deposito = deposito; }

    public BigDecimal getLitros() { return litros; }
    public void setLitros(BigDecimal litros) { this.litros = litros; }

    public Casta getCasta() { return casta; }
    public void setCasta(Casta casta) { this.casta = casta; }

    public String getRecipienteRef() {
        if (recipienteRef != null) return recipienteRef;
        if (talha != null) return "TALHA:" + talha.getId();
        if (deposito != null) return "DEPOSITO:" + deposito.getId();
        return "";
    }
    public void setRecipienteRef(String recipienteRef) { this.recipienteRef = recipienteRef; }

    @Transient
    public String getRecipienteDescricao() {
        if (talha != null) return "Talha " + talha.getIdentificacao();
        if (deposito != null) return "Depósito " + deposito.getIdentificacao();
        return "—";
    }
}
