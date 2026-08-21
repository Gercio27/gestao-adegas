package pt.acv.adega.processos.moagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcela;
import pt.acv.adega.planeamento.RegistoVindima;

import java.math.BigDecimal;

/**
 * Quantos Kg de uma vindima concreta entraram num enchimento. E' o que permite
 * moer varias vindimas ao mesmo tempo e decidir, talha a talha, quanto se moi
 * de cada uma - e e' daqui que sai a casta (vem da parcela da vindima) e o
 * saldo por moer de cada vindima.
 */
@Entity
@Table(name = "enchimento_vindima")
public class EnchimentoVindima {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enchimento_id")
    private Enchimento enchimento;

    /**
     * Parcela de onde veio esta uva. Continua preenchida sempre — e' por ela que
     * se contam os saldos da parcela — mas quem manda no detalhe e' a
     * {@link #colheita}, quando esta indicada.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "linha_id")
    private LinhaPlaneamentoParcela linha;

    /**
     * Colheita concreta de onde saiu esta uva. E' o utilizador que a escolhe no
     * ecra da moagem. Fica a nulo nos registos feitos antes de a escolha
     * existir; nesses, a folha da vindima reparte por ordem de chegada.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "colheita_id")
    private RegistoVindima colheita;

    @Column(precision = 12, scale = 2)
    private BigDecimal quantidadeKg;

    /** Id da vindima vindo do formulario; e' resolvido para {@link #linha} no controlador. */
    @Transient
    private Long linhaId;

    /** Id da colheita vindo do formulario; e' resolvido para {@link #colheita} no controlador. */
    @Transient
    private Long colheitaId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Enchimento getEnchimento() { return enchimento; }
    public void setEnchimento(Enchimento enchimento) { this.enchimento = enchimento; }

    public LinhaPlaneamentoParcela getLinha() { return linha; }
    public void setLinha(LinhaPlaneamentoParcela linha) { this.linha = linha; }

    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public void setQuantidadeKg(BigDecimal quantidadeKg) { this.quantidadeKg = quantidadeKg; }

    public Long getLinhaId() {
        if (linhaId != null) return linhaId;
        return linha != null ? linha.getId() : null;
    }
    public void setLinhaId(Long linhaId) { this.linhaId = linhaId; }

    public RegistoVindima getColheita() { return colheita; }
    public void setColheita(RegistoVindima colheita) { this.colheita = colheita; }

    public Long getColheitaId() {
        if (colheitaId != null) return colheitaId;
        return colheita != null ? colheita.getId() : null;
    }
    public void setColheitaId(Long colheitaId) { this.colheitaId = colheitaId; }

    /** Casta desta vindima (vem da parcela) - e' o que preenche a casta do enchimento. */
    @Transient
    public Casta getCasta() {
        if (linha == null || linha.getParcela() == null) return null;
        return linha.getParcela().getCasta();
    }

    /** "Vinho / Parcela" e, se a colheita estiver indicada, o codigo dela. */
    @Transient
    public String getVindimaDescricao() {
        String base = linha != null ? linha.getEtiqueta() : "—";
        if (colheita != null && colheita.getCodigo() != null) {
            return base + " · " + colheita.getCodigo();
        }
        return base;
    }
}
