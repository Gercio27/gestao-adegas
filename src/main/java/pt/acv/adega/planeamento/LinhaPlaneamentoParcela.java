package pt.acv.adega.planeamento;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Parcela;
import pt.acv.adega.fichas.Trabalhador;

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

    // ----- Dados da vindima (Fase 2), preenchidos por linha na folha da vindima -----

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vindima_responsavel_id")
    private Trabalhador responsavel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vindima_adega_entrega_id")
    private Adega adegaEntrega;

    /** Vasilame usado (baldes/contentores/reboque). */
    @Column(length = 200)
    private String vasilame;

    @Column(length = 300)
    private String meios;

    @Column(length = 300)
    private String metodos;

    /** Transporte até à adega. */
    @Column(length = 200)
    private String transporte;

    @Column(length = 500)
    private String observacoesVindima;

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

    public Trabalhador getResponsavel() { return responsavel; }
    public void setResponsavel(Trabalhador responsavel) { this.responsavel = responsavel; }

    public Adega getAdegaEntrega() { return adegaEntrega; }
    public void setAdegaEntrega(Adega adegaEntrega) { this.adegaEntrega = adegaEntrega; }

    public String getVasilame() { return vasilame; }
    public void setVasilame(String vasilame) { this.vasilame = vasilame; }

    public String getMeios() { return meios; }
    public void setMeios(String meios) { this.meios = meios; }

    public String getMetodos() { return metodos; }
    public void setMetodos(String metodos) { this.metodos = metodos; }

    public String getTransporte() { return transporte; }
    public void setTransporte(String transporte) { this.transporte = transporte; }

    public String getObservacoesVindima() { return observacoesVindima; }
    public void setObservacoesVindima(String observacoesVindima) { this.observacoesVindima = observacoesVindima; }

    public List<RegistoVindima> getVindimas() { return vindimas; }
    public void setVindimas(List<RegistoVindima> vindimas) { this.vindimas = vindimas; }

    /** Litros de vinho previstos = Kg a aplicar x 60%. */
    @Transient
    public BigDecimal getLitrosPrevistos() {
        return kgAplicar == null ? null : kgAplicar.multiply(FATOR_LITROS);
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
