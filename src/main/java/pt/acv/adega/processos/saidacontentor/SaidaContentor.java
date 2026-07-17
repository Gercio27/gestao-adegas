package pt.acv.adega.processos.saidacontentor;

import jakarta.persistence.*;
import pt.acv.adega.common.BaseEntity;

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
