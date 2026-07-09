package pt.acv.adega.planeamento;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.fichas.Trabalhador;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma vindima (colheita) de uma linha de planeamento: datas, quantidade (Kg) e
 * os dados da operacao (responsavel, vasilame, transporte, meios, metodos,
 * observacoes). Cada colheita e independente — pode ter operador/carrinha/caixa
 * diferentes das outras colheitas da mesma parcela.
 */
@Entity
@Table(name = "registo_vindima")
public class RegistoVindima {

    /** Prefixo do codigo automatico de cada colheita. */
    public static final String PREFIXO = "VDM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codigo unico e visivel da colheita (ex.: VDM-000001). */
    @Column(unique = true, length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linha_id")
    private LinhaPlaneamentoParcela linha;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    @Column(precision = 12, scale = 2)
    private BigDecimal quantidadeKg;

    // ----- Dados da operacao (proprios de cada colheita) -----

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsavel_id")
    private Trabalhador responsavel;

    @Column(length = 200)
    private String vasilame;

    @Column(length = 300)
    private String meios;

    @Column(length = 300)
    private String metodos;

    @Column(length = 200)
    private String transporte;

    @Column(length = 500)
    private String observacoes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public LinhaPlaneamentoParcela getLinha() { return linha; }
    public void setLinha(LinhaPlaneamentoParcela linha) { this.linha = linha; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public void setQuantidadeKg(BigDecimal quantidadeKg) { this.quantidadeKg = quantidadeKg; }

    public Trabalhador getResponsavel() { return responsavel; }
    public void setResponsavel(Trabalhador responsavel) { this.responsavel = responsavel; }

    public String getVasilame() { return vasilame; }
    public void setVasilame(String vasilame) { this.vasilame = vasilame; }

    public String getMeios() { return meios; }
    public void setMeios(String meios) { this.meios = meios; }

    public String getMetodos() { return metodos; }
    public void setMetodos(String metodos) { this.metodos = metodos; }

    public String getTransporte() { return transporte; }
    public void setTransporte(String transporte) { this.transporte = transporte; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    @Transient
    public boolean isVazia() {
        return dataInicio == null && dataFim == null && quantidadeKg == null
                && responsavel == null
                && (vasilame == null || vasilame.isBlank())
                && (transporte == null || transporte.isBlank());
    }
}
