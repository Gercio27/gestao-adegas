package pt.acv.adega.planeamento;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Parcela;

import java.math.BigDecimal;

/**
 * Linha de planeamento de um vinho: uma parcela e a quantidade de uva (Kg) dessa
 * parcela a aplicar neste vinho. O nome/casta/area vem da parcela; a producao
 * prevista no ano e do lado da parcela (partilhada entre vinhos).
 */
@Entity
@Table(name = "planeamento_linha_parcela")
public class LinhaPlaneamentoParcela {

    /** Fator de conversao de uva (Kg) para vinho previsto (litros): 60%. */
    public static final BigDecimal FATOR_LITROS = new BigDecimal("0.60");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parcela_id")
    private Parcela parcela;

    /** Quilogramas de uva desta parcela a aplicar neste vinho. */
    @Column(precision = 12, scale = 2)
    private BigDecimal kgAplicar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planeamento_id")
    private PlaneamentoVinho planeamento;

    /**
     * Producao prevista da parcela (Kg/ano). Nao e persistida aqui (vive na
     * parcela) — serve so para editar/mostrar no formulario do vinho.
     */
    @Transient
    private BigDecimal producaoPrevistaKg;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Parcela getParcela() { return parcela; }
    public void setParcela(Parcela parcela) { this.parcela = parcela; }

    public BigDecimal getKgAplicar() { return kgAplicar; }
    public void setKgAplicar(BigDecimal kgAplicar) { this.kgAplicar = kgAplicar; }

    public PlaneamentoVinho getPlaneamento() { return planeamento; }
    public void setPlaneamento(PlaneamentoVinho planeamento) { this.planeamento = planeamento; }

    public BigDecimal getProducaoPrevistaKg() { return producaoPrevistaKg; }
    public void setProducaoPrevistaKg(BigDecimal producaoPrevistaKg) { this.producaoPrevistaKg = producaoPrevistaKg; }

    /** Litros de vinho previstos = Kg a aplicar x 60%. */
    @Transient
    public BigDecimal getLitrosPrevistos() {
        return kgAplicar == null ? null : kgAplicar.multiply(FATOR_LITROS);
    }
}
