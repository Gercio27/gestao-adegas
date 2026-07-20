package pt.acv.adega.processos.passagem;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Linha de uma passagem a limpo (Fase 4.4): um mosto que passa a vinho pronto,
 * com os litros que efetivamente passam (a diferenca para o original e perda,
 * borras) e, opcionalmente, a talha de destino para onde o vinho vai.
 *
 * Os campos de snapshot (litros originais, recipiente de origem) sao preenchidos
 * ao fechar, para permitir reverter (reabrir).
 */
@Entity
@Table(name = "passagem_item")
public class PassagemItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private ProcessoPassagemVinho processo;

    @Column(name = "mosto_id", nullable = false)
    private Long mostoId;

    /** Litros que efetivamente passam a limpo (<= originais; a diferenca e perda). */
    @Column(precision = 12, scale = 2)
    private BigDecimal litrosEfetivos;

    /** Talha de destino (mesma adega). Nulo = fica no recipiente onde ja esta. */
    @Column(name = "talha_destino_id")
    private Long talhaDestinoId;

    /** Descricao legivel (codigo do mosto, origem, destino) para o resumo. */
    @Column(length = 300)
    private String descricao;

    // ----- snapshots preenchidos ao fechar (para reverter) -----

    @Column(precision = 12, scale = 2)
    private BigDecimal litrosOriginais;

    @Column(name = "talha_origem_id")
    private Long talhaOrigemId;

    @Column(name = "deposito_origem_id")
    private Long depositoOrigemId;

    /** Se, ao fechar, o vinho foi mesmo movido para outra talha. */
    @Column(nullable = false)
    private boolean movido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProcessoPassagemVinho getProcesso() { return processo; }
    public void setProcesso(ProcessoPassagemVinho processo) { this.processo = processo; }

    public Long getMostoId() { return mostoId; }
    public void setMostoId(Long mostoId) { this.mostoId = mostoId; }

    public BigDecimal getLitrosEfetivos() { return litrosEfetivos; }
    public void setLitrosEfetivos(BigDecimal litrosEfetivos) { this.litrosEfetivos = litrosEfetivos; }

    public Long getTalhaDestinoId() { return talhaDestinoId; }
    public void setTalhaDestinoId(Long talhaDestinoId) { this.talhaDestinoId = talhaDestinoId; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getLitrosOriginais() { return litrosOriginais; }
    public void setLitrosOriginais(BigDecimal litrosOriginais) { this.litrosOriginais = litrosOriginais; }

    public Long getTalhaOrigemId() { return talhaOrigemId; }
    public void setTalhaOrigemId(Long talhaOrigemId) { this.talhaOrigemId = talhaOrigemId; }

    public Long getDepositoOrigemId() { return depositoOrigemId; }
    public void setDepositoOrigemId(Long depositoOrigemId) { this.depositoOrigemId = depositoOrigemId; }

    public boolean isMovido() { return movido; }
    public void setMovido(boolean movido) { this.movido = movido; }
}
