package pt.acv.adega.produtos;

import jakarta.persistence.*;
import pt.acv.adega.common.BaseEntity;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.Talha;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ficha de Mosto (produto 2.1). NAO e criada manualmente: resulta do processo
 * de Moagem (Fase 3), que a preenche automaticamente para cada talha/cuba cheia.
 * Guarda a localizacao (talha ou deposito), o volume, a casta e a rastreabilidade
 * de origem (moagem / vindima / uva).
 */
@Entity
@Table(name = "mosto")
public class Mosto extends BaseEntity {

    public static final String PREFIXO = "MST";

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal litros = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    /** Localizacao: um mosto esta numa talha OU num deposito/cuba. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "talha_id")
    private Talha talha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deposito_id")
    private Deposito deposito;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMosto estado = EstadoMosto.EM_FERMENTACAO;

    /** Descricao legivel da origem (ex.: "Moagem MOA-000001 · Vindima VDM-000001"). */
    @Column(length = 300)
    private String origemDescricao;

    /** Id do processo de moagem que o gerou (para reverter em caso de reabertura). */
    @Column(name = "origem_moagem_id")
    private Long origemMoagemId;

    /** Id do movimento de entrada externo que o gerou (para reverter). */
    @Column(name = "origem_movimento_id")
    private Long origemMovimentoId;

    private LocalDateTime dataProducao;

    /** Codigo do lote (Fase 4.6) a que este vinho/mosto pertence. */
    @Column(length = 20)
    private String loteCodigo;

    /** Certificacao (Fase 5.5) do vinho a granel. */
    @Column(nullable = false)
    private boolean certificado = false;

    private java.time.LocalDate validadeCertificacao;

    public String getLoteCodigo() { return loteCodigo; }
    public void setLoteCodigo(String loteCodigo) { this.loteCodigo = loteCodigo; }

    public boolean isCertificado() { return certificado; }
    public void setCertificado(boolean certificado) { this.certificado = certificado; }

    public java.time.LocalDate getValidadeCertificacao() { return validadeCertificacao; }
    public void setValidadeCertificacao(java.time.LocalDate validadeCertificacao) { this.validadeCertificacao = validadeCertificacao; }

    public BigDecimal getLitros() { return litros; }
    public void setLitros(BigDecimal litros) { this.litros = litros; }

    public Casta getCasta() { return casta; }
    public void setCasta(Casta casta) { this.casta = casta; }

    public Talha getTalha() { return talha; }
    public void setTalha(Talha talha) { this.talha = talha; }

    public Deposito getDeposito() { return deposito; }
    public void setDeposito(Deposito deposito) { this.deposito = deposito; }

    public EstadoMosto getEstado() { return estado; }
    public void setEstado(EstadoMosto estado) { this.estado = estado; }

    public String getOrigemDescricao() { return origemDescricao; }
    public void setOrigemDescricao(String origemDescricao) { this.origemDescricao = origemDescricao; }

    public Long getOrigemMoagemId() { return origemMoagemId; }
    public void setOrigemMoagemId(Long origemMoagemId) { this.origemMoagemId = origemMoagemId; }

    public Long getOrigemMovimentoId() { return origemMovimentoId; }
    public void setOrigemMovimentoId(Long origemMovimentoId) { this.origemMovimentoId = origemMovimentoId; }

    public LocalDateTime getDataProducao() { return dataProducao; }
    public void setDataProducao(LocalDateTime dataProducao) { this.dataProducao = dataProducao; }

    /** Identificacao da talha/deposito onde o mosto se encontra. */
    @Transient
    public String getLocalizacao() {
        if (talha != null) return "Talha " + talha.getIdentificacao();
        if (deposito != null) return "Depósito " + deposito.getIdentificacao();
        return "—";
    }
}
