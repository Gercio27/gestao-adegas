package pt.acv.adega.processos.saidacontentor;

import jakarta.persistence.*;
import pt.acv.adega.common.BaseEntity;
import pt.acv.adega.fichas.TipoEmbalagem;

import java.time.LocalDateTime;

/**
 * Registo de saida de garrafas de um contentor por um motivo que nao a entrega
 * ao comercial: certificacao, prova, reserva da adega, promocao ou outras.
 * Ao guardar, da baixa das garrafas no contentor; ao eliminar, repoe-as.
 */
@Entity
@Table(name = "saida_contentor")
public class SaidaContentor extends BaseEntity {

    public static final String PREFIXO = "SCT";

    /** De que tipo e' o contentor de onde saiu: garrafas ou bag-in-box. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_embalagem", nullable = false, length = 12)
    private TipoEmbalagem tipoEmbalagem = TipoEmbalagem.GARRAFA;

    @Column(name = "contentor_id")
    private Long contentorId;

    @Column(length = 160)
    private String contentorNome;

    @Column(length = 160)
    private String vinhoNome;

    @Column(nullable = false)
    private int quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MotivoSaidaContentor motivo = MotivoSaidaContentor.OUTRAS;

    /** Descricao livre quando o motivo e OUTRAS. */
    @Column(length = 250)
    private String observacao;

    private LocalDateTime dataSaida;

    @Column(length = 80)
    private String criadoPor;

    public TipoEmbalagem getTipoEmbalagem() { return tipoEmbalagem; }
    public void setTipoEmbalagem(TipoEmbalagem tipoEmbalagem) { this.tipoEmbalagem = tipoEmbalagem; }

    /** "garrafa(s)" ou "unidade(s)", conforme o tipo de contentor. */
    @Transient
    public String getUnidadeNome() {
        return tipoEmbalagem != null ? tipoEmbalagem.getUnidade() : "garrafa(s)";
    }

    public Long getContentorId() { return contentorId; }
    public void setContentorId(Long contentorId) { this.contentorId = contentorId; }

    public String getContentorNome() { return contentorNome; }
    public void setContentorNome(String contentorNome) { this.contentorNome = contentorNome; }

    public String getVinhoNome() { return vinhoNome; }
    public void setVinhoNome(String vinhoNome) { this.vinhoNome = vinhoNome; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public MotivoSaidaContentor getMotivo() { return motivo; }
    public void setMotivo(MotivoSaidaContentor motivo) { this.motivo = motivo; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public LocalDateTime getDataSaida() { return dataSaida; }
    public void setDataSaida(LocalDateTime dataSaida) { this.dataSaida = dataSaida; }

    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
}
