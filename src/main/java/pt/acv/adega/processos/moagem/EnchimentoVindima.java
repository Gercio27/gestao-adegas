package pt.acv.adega.processos.moagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcela;

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

    /** Vindima (parcela colhida) de onde veio esta uva. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "linha_id")
    private LinhaPlaneamentoParcela linha;

    @Column(precision = 12, scale = 2)
    private BigDecimal quantidadeKg;

    /** Id da vindima vindo do formulario; e' resolvido para {@link #linha} no controlador. */
    @Transient
    private Long linhaId;

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

    /** Casta desta vindima (vem da parcela) - e' o que preenche a casta do enchimento. */
    @Transient
    public Casta getCasta() {
        if (linha == null || linha.getParcela() == null) return null;
        return linha.getParcela().getCasta();
    }

    @Transient
    public String getVindimaDescricao() {
        return linha != null ? linha.getEtiqueta() : "—";
    }
}
