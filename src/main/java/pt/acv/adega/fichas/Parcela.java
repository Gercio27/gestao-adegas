package pt.acv.adega.fichas;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parcela de uma vinha, com os dados do registo SIVV/IVV: identificacao (nº de
 * registo SIVV), casta plantada, area, ano de plantacao e restantes campos do
 * cadastro (concelho, freguesia, regiao, tipo de cultura, area enquadramento
 * legal, etc.).
 */
@Entity
@Table(name = "parcela")
public class Parcela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificacao / nº de Registo SIVV (geocodigo). */
    @Column(length = 80)
    private String identificacao;

    /** Nome da parcela (ex.: Vale Palheiro). */
    @Column(length = 160)
    private String nome;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    @Column(length = 80)
    private String concelho;

    @Column(length = 80)
    private String freguesia;

    @Column(length = 80)
    private String regiao;

    /** Tipo de cultura (ex.: Continua). */
    @Column(length = 40)
    private String tipoCultura;

    /** Ano em que a parcela (casta) foi plantada. */
    private Integer anoPlantacao;

    /** Area de vinha (ha). */
    @Column(precision = 10, scale = 4)
    private BigDecimal areaHa;

    /** Producao prevista da parcela no ano/campanha (Kg). Base para o saldo no
     * planeamento: cada vinho consome Kg desta parcela e o saldo desce. */
    @Column(precision = 12, scale = 2)
    private BigDecimal producaoPrevistaKg;

    /** Area de enquadramento legal (ha). */
    @Column(precision = 10, scale = 4)
    private BigDecimal areaEnqLegal;

    @Column(length = 40)
    private String tipo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataUltimaAtualizacao;

    @Column(length = 500)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vinha_id")
    private Vinha vinha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdentificacao() { return identificacao; }
    public void setIdentificacao(String identificacao) { this.identificacao = identificacao; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Casta getCasta() { return casta; }
    public void setCasta(Casta casta) { this.casta = casta; }

    public String getConcelho() { return concelho; }
    public void setConcelho(String concelho) { this.concelho = concelho; }

    public String getFreguesia() { return freguesia; }
    public void setFreguesia(String freguesia) { this.freguesia = freguesia; }

    public String getRegiao() { return regiao; }
    public void setRegiao(String regiao) { this.regiao = regiao; }

    public String getTipoCultura() { return tipoCultura; }
    public void setTipoCultura(String tipoCultura) { this.tipoCultura = tipoCultura; }

    public Integer getAnoPlantacao() { return anoPlantacao; }
    public void setAnoPlantacao(Integer anoPlantacao) { this.anoPlantacao = anoPlantacao; }

    public BigDecimal getAreaHa() { return areaHa; }
    public void setAreaHa(BigDecimal areaHa) { this.areaHa = areaHa; }

    public BigDecimal getProducaoPrevistaKg() { return producaoPrevistaKg; }
    public void setProducaoPrevistaKg(BigDecimal producaoPrevistaKg) { this.producaoPrevistaKg = producaoPrevistaKg; }

    public BigDecimal getAreaEnqLegal() { return areaEnqLegal; }
    public void setAreaEnqLegal(BigDecimal areaEnqLegal) { this.areaEnqLegal = areaEnqLegal; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public LocalDate getDataUltimaAtualizacao() { return dataUltimaAtualizacao; }
    public void setDataUltimaAtualizacao(LocalDate dataUltimaAtualizacao) { this.dataUltimaAtualizacao = dataUltimaAtualizacao; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public Vinha getVinha() { return vinha; }
    public void setVinha(Vinha vinha) { this.vinha = vinha; }
}
