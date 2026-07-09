package pt.acv.adega.planeamento;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Planeamento de um vinho (Fase 1.2). Cada vinho tem nome, tipo, data de
 * planeamento e data prevista de vindima, e uma lista de parcelas com a
 * quantidade de uva (Kg) a aplicar. O saldo de cada parcela desce a medida que
 * varios vinhos a consomem (ver PlaneamentoController).
 */
@Entity
@Table(name = "planeamento_vinho")
public class PlaneamentoVinho extends BaseEntity {

    public static final String PREFIXO = "PLN";

    @NotBlank(message = "Indique o nome do vinho.")
    @Column(nullable = false, length = 160)
    private String nomeVinho;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private TipoVinho tipoVinho;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPlaneamento;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPrevistaVindima;

    @OneToMany(mappedBy = "planeamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<LinhaPlaneamentoParcela> linhas = new ArrayList<>();

    public String getNomeVinho() { return nomeVinho; }
    public void setNomeVinho(String nomeVinho) { this.nomeVinho = nomeVinho; }

    public TipoVinho getTipoVinho() { return tipoVinho; }
    public void setTipoVinho(TipoVinho tipoVinho) { this.tipoVinho = tipoVinho; }

    public LocalDate getDataPlaneamento() { return dataPlaneamento; }
    public void setDataPlaneamento(LocalDate dataPlaneamento) { this.dataPlaneamento = dataPlaneamento; }

    public LocalDate getDataPrevistaVindima() { return dataPrevistaVindima; }
    public void setDataPrevistaVindima(LocalDate dataPrevistaVindima) { this.dataPrevistaVindima = dataPrevistaVindima; }

    public List<LinhaPlaneamentoParcela> getLinhas() { return linhas; }
    public void setLinhas(List<LinhaPlaneamentoParcela> linhas) { this.linhas = linhas; }

    /** Total de uva a aplicar neste vinho (soma das parcelas), em Kg. */
    @Transient
    public BigDecimal getTotalKgAplicar() {
        return linhas.stream()
                .map(l -> l.getKgAplicar() == null ? BigDecimal.ZERO : l.getKgAplicar())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total de vinho previsto (litros) = total Kg x 60%. */
    @Transient
    public BigDecimal getTotalLitrosPrevistos() {
        return getTotalKgAplicar().multiply(LinhaPlaneamentoParcela.FATOR_LITROS);
    }
}
