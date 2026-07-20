package pt.acv.adega.processos.moagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.Talha;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    /** Quantidade de uva moida para este recipiente (Kg). */
    @Column(precision = 12, scale = 2)
    private BigDecimal quantidadeMoidaKg;

    /** Litros de mosto resultantes (entram no recipiente e geram a ficha de mosto). */
    @Column(precision = 12, scale = 2)
    private BigDecimal litros;

    /** Álcool provável (% vol.) estimado para este mosto. Passa para a ficha de mosto. */
    @Column(name = "alcool_provavel", precision = 5, scale = 2)
    private BigDecimal alcoolProvavel;

    /**
     * Casta principal (a primeira do lote). Mantida por compatibilidade com os
     * ecrãs que mostram uma casta; para o conjunto completo usar {@link #castas}.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    /** Todas as castas deste enchimento (pode ser um lote de várias castas). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "enchimento_casta",
            joinColumns = @JoinColumn(name = "enchimento_id"),
            inverseJoinColumns = @JoinColumn(name = "casta_id"))
    private List<Casta> castas = new ArrayList<>();

    /**
     * Ids das castas vindos do formulario (multi-select). Nao persiste; sao
     * resolvidos para {@link #castas} no controlador.
     */
    @Transient
    private List<Long> castaIds = new ArrayList<>();

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

    public BigDecimal getQuantidadeMoidaKg() { return quantidadeMoidaKg; }
    public void setQuantidadeMoidaKg(BigDecimal quantidadeMoidaKg) { this.quantidadeMoidaKg = quantidadeMoidaKg; }

    public BigDecimal getLitros() { return litros; }
    public void setLitros(BigDecimal litros) { this.litros = litros; }

    public BigDecimal getAlcoolProvavel() { return alcoolProvavel; }
    public void setAlcoolProvavel(BigDecimal alcoolProvavel) { this.alcoolProvavel = alcoolProvavel; }

    public Casta getCasta() { return casta; }
    public void setCasta(Casta casta) { this.casta = casta; }

    public List<Casta> getCastas() { return castas; }
    public void setCastas(List<Casta> castas) { this.castas = castas; }

    public List<Long> getCastaIds() { return castaIds; }
    public void setCastaIds(List<Long> castaIds) { this.castaIds = castaIds; }

    /** Nomes das castas juntos (ex.: "Antão Vaz, Perrum"); cai na casta principal se a lista estiver vazia. */
    @Transient
    public String getCastasDescricao() {
        if (castas != null && !castas.isEmpty()) {
            return castas.stream().map(Casta::getNome).collect(Collectors.joining(", "));
        }
        return casta != null ? casta.getNome() : "—";
    }

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
