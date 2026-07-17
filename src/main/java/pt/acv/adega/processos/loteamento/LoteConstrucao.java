package pt.acv.adega.processos.loteamento;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Construcao de um lote (6.2): uma transfega efetiva que da baixa num deposito
 * de origem e junta no vinho do lote (deposito de destino). Um lote pode ser
 * construido em varias construcoes (numero 1, 2, ...).
 */
@Entity
@Table(name = "lote_construcao")
public class LoteConstrucao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loteamento_id", nullable = false)
    private Long loteamentoId;

    @Column(nullable = false)
    private int numero;

    @Column(name = "mosto_origem_id")
    private Long mostoOrigemId;

    @Column(length = 250)
    private String origemDescricao;

    @Column(length = 40)
    private String destinoRef;

    @Column(length = 200)
    private String destinoDescricao;

    @Column(precision = 12, scale = 2)
    private BigDecimal litros;

    @Column(name = "mosto_destino_id")
    private Long mostoDestinoId;

    @Column(nullable = false)
    private boolean destinoCriado = false;

    private LocalDateTime data;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoteamentoId() { return loteamentoId; }
    public void setLoteamentoId(Long loteamentoId) { this.loteamentoId = loteamentoId; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public Long getMostoOrigemId() { return mostoOrigemId; }
    public void setMostoOrigemId(Long mostoOrigemId) { this.mostoOrigemId = mostoOrigemId; }

    public String getOrigemDescricao() { return origemDescricao; }
    public void setOrigemDescricao(String origemDescricao) { this.origemDescricao = origemDescricao; }

    public String getDestinoRef() { return destinoRef; }
    public void setDestinoRef(String destinoRef) { this.destinoRef = destinoRef; }

    public String getDestinoDescricao() { return destinoDescricao; }
    public void setDestinoDescricao(String destinoDescricao) { this.destinoDescricao = destinoDescricao; }

    public BigDecimal getLitros() { return litros; }
    public void setLitros(BigDecimal litros) { this.litros = litros; }

    public Long getMostoDestinoId() { return mostoDestinoId; }
    public void setMostoDestinoId(Long mostoDestinoId) { this.mostoDestinoId = mostoDestinoId; }

    public boolean isDestinoCriado() { return destinoCriado; }
    public void setDestinoCriado(boolean destinoCriado) { this.destinoCriado = destinoCriado; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }
}
