package pt.acv.adega.produtos;

import jakarta.persistence.*;
import pt.acv.adega.common.BaseEntity;
import pt.acv.adega.fichas.Casta;

import java.time.LocalDateTime;

/**
 * Ficha de Vinho Engarrafado (produto 2.5). Gerada pelo processo de
 * Engarrafamento (Fase 6). Guarda o nome do vinho, o numero de garrafas, o
 * lote e a rastreabilidade de origem.
 */
@Entity
@Table(name = "vinho_engarrafado")
public class VinhoEngarrafado extends BaseEntity {

    public static final String PREFIXO = "VEG";

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(nullable = false)
    private int numeroGarrafas;

    private Integer capacidadeMl;

    @Column(length = 60)
    private String lote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    /** Marcado true quando o processo de Rotulagem (Fase 7) e fechado. */
    @Column(nullable = false)
    private boolean rotulado = false;

    /** Garrafas ja entregues ao comercial (Fase 8). */
    @Column(nullable = false)
    private int garrafasEntregues = 0;

    /** Certificacao (Fase 6.4) do vinho engarrafado. */
    @Column(nullable = false)
    private boolean certificado = false;

    private java.time.LocalDate validadeCertificacao;

    @Column(length = 300)
    private String origemDescricao;

    @Column(name = "origem_engarrafamento_id")
    private Long origemEngarrafamentoId;

    private LocalDateTime dataProducao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public int getNumeroGarrafas() { return numeroGarrafas; }
    public void setNumeroGarrafas(int numeroGarrafas) { this.numeroGarrafas = numeroGarrafas; }

    public Integer getCapacidadeMl() { return capacidadeMl; }
    public void setCapacidadeMl(Integer capacidadeMl) { this.capacidadeMl = capacidadeMl; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public Casta getCasta() { return casta; }
    public void setCasta(Casta casta) { this.casta = casta; }

    public boolean isRotulado() { return rotulado; }
    public void setRotulado(boolean rotulado) { this.rotulado = rotulado; }

    public int getGarrafasEntregues() { return garrafasEntregues; }
    public void setGarrafasEntregues(int garrafasEntregues) { this.garrafasEntregues = garrafasEntregues; }

    /** Garrafas ainda disponiveis (produzidas menos entregues). */
    @Transient
    public int getDisponiveis() { return numeroGarrafas - garrafasEntregues; }

    public boolean isCertificado() { return certificado; }
    public void setCertificado(boolean certificado) { this.certificado = certificado; }

    public java.time.LocalDate getValidadeCertificacao() { return validadeCertificacao; }
    public void setValidadeCertificacao(java.time.LocalDate validadeCertificacao) { this.validadeCertificacao = validadeCertificacao; }

    public String getOrigemDescricao() { return origemDescricao; }
    public void setOrigemDescricao(String origemDescricao) { this.origemDescricao = origemDescricao; }

    public Long getOrigemEngarrafamentoId() { return origemEngarrafamentoId; }
    public void setOrigemEngarrafamentoId(Long origemEngarrafamentoId) { this.origemEngarrafamentoId = origemEngarrafamentoId; }

    public LocalDateTime getDataProducao() { return dataProducao; }
    public void setDataProducao(LocalDateTime dataProducao) { this.dataProducao = dataProducao; }
}
