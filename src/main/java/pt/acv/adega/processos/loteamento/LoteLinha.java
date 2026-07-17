package pt.acv.adega.processos.loteamento;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Linha planeada de um lote (6.1): quantidade prevista a entrar no lote a partir
 * de um deposito de origem. E so intencao — nao da baixa.
 */
@Entity
@Table(name = "lote_linha")
public class LoteLinha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loteamento_id", nullable = false)
    private Long loteamentoId;

    @Column(name = "mosto_origem_id")
    private Long mostoOrigemId;

    @Column(length = 250)
    private String origemDescricao;

    @Column(precision = 12, scale = 2)
    private BigDecimal litrosPlaneados;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoteamentoId() { return loteamentoId; }
    public void setLoteamentoId(Long loteamentoId) { this.loteamentoId = loteamentoId; }

    public Long getMostoOrigemId() { return mostoOrigemId; }
    public void setMostoOrigemId(Long mostoOrigemId) { this.mostoOrigemId = mostoOrigemId; }

    public String getOrigemDescricao() { return origemDescricao; }
    public void setOrigemDescricao(String origemDescricao) { this.origemDescricao = origemDescricao; }

    public BigDecimal getLitrosPlaneados() { return litrosPlaneados; }
    public void setLitrosPlaneados(BigDecimal litrosPlaneados) { this.litrosPlaneados = litrosPlaneados; }
}
