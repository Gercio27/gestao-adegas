package pt.acv.adega.planeamento;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma vindima (colheita) de uma linha de planeamento: intervalo de datas e
 * quantidade colhida (Kg). Cada linha de planeamento pode ter varias (ate 5).
 * A soma dos Kg e comparada com o "Kg a aplicar" planeado dessa linha.
 */
@Entity
@Table(name = "registo_vindima")
public class RegistoVindima {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linha_id")
    private LinhaPlaneamentoParcela linha;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    @Column(precision = 12, scale = 2)
    private BigDecimal quantidadeKg;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LinhaPlaneamentoParcela getLinha() { return linha; }
    public void setLinha(LinhaPlaneamentoParcela linha) { this.linha = linha; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public void setQuantidadeKg(BigDecimal quantidadeKg) { this.quantidadeKg = quantidadeKg; }

    @Transient
    public boolean isVazia() {
        return dataInicio == null && dataFim == null && quantidadeKg == null;
    }
}
