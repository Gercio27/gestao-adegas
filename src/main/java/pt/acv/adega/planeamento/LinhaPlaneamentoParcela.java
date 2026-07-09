package pt.acv.adega.planeamento;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Parcela;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    // ----- Dados da vindima (Fase 2) -----

    /** Adega de entrega da uva desta parcela (usada para agrupar na moagem). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vindima_adega_entrega_id")
    private Adega adegaEntrega;

    /** Colheitas desta parcela. Cada colheita tem os seus proprios dados de operacao. */
    @OneToMany(mappedBy = "linha", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<RegistoVindima> vindimas = new ArrayList<>();

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

    public Adega getAdegaEntrega() { return adegaEntrega; }
    public void setAdegaEntrega(Adega adegaEntrega) { this.adegaEntrega = adegaEntrega; }

    public List<RegistoVindima> getVindimas() { return vindimas; }
    public void setVindimas(List<RegistoVindima> vindimas) { this.vindimas = vindimas; }

    /** Litros de vinho previstos = Kg a aplicar x 60%. */
    @Transient
    public BigDecimal getLitrosPrevistos() {
        return kgAplicar == null ? null : kgAplicar.multiply(FATOR_LITROS);
    }

    /** Etiqueta legivel "Vinho / Parcela" (para dropdowns e rastreabilidade). */
    @Transient
    public String getEtiqueta() {
        String vinho = planeamento != null && planeamento.getNomeVinho() != null ? planeamento.getNomeVinho() : "?";
        String parc = "?";
        if (parcela != null) {
            if (parcela.getNome() != null && !parcela.getNome().isBlank()) parc = parcela.getNome();
            else if (parcela.getIdentificacao() != null && !parcela.getIdentificacao().isBlank()) parc = parcela.getIdentificacao();
            else parc = "Parcela " + parcela.getId();
        }
        return vinho + " / " + parc;
    }

    /** Soma dos Kg de todas as vindimas registadas nesta linha. */
    @Transient
    public BigDecimal getTotalVindimadoKg() {
        return vindimas.stream()
                .map(v -> v.getQuantidadeKg() == null ? BigDecimal.ZERO : v.getQuantidadeKg())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** true se a soma vindimada excede o Kg a aplicar planeado (apenas aviso). */
    @Transient
    public boolean isVindimaExcede() {
        return kgAplicar != null && getTotalVindimadoKg().compareTo(kgAplicar) > 0;
    }
}
