package pt.acv.adega.fichas;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Parcela de uma vinha: identificacao, casta plantada e area ocupada (ha).
 */
@Entity
@Table(name = "parcela")
public class Parcela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80)
    private String identificacao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    @Column(precision = 10, scale = 4)
    private BigDecimal areaHa;

    /** Ano em que a parcela (casta) foi plantada. */
    private Integer anoPlantacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vinha_id")
    private Vinha vinha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdentificacao() { return identificacao; }
    public void setIdentificacao(String identificacao) { this.identificacao = identificacao; }

    public Casta getCasta() { return casta; }
    public void setCasta(Casta casta) { this.casta = casta; }

    public BigDecimal getAreaHa() { return areaHa; }
    public void setAreaHa(BigDecimal areaHa) { this.areaHa = areaHa; }

    public Integer getAnoPlantacao() { return anoPlantacao; }
    public void setAnoPlantacao(Integer anoPlantacao) { this.anoPlantacao = anoPlantacao; }

    public Vinha getVinha() { return vinha; }
    public void setVinha(Vinha vinha) { this.vinha = vinha; }
}
