package pt.acv.adega.planeamento;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.common.BaseEntity;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.fichas.Vinha;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linha de planeamento dos vinhos (Fase 1.2). Para cada vinha e casta define-se
 * a quantidade prevista colher, a data prevista de vindima, a adega de destino,
 * o vinho a fazer e a % de participacao dessa casta nesse vinho. O mapa de
 * planeamento junta a estas linhas os resultados das analises a maturacao.
 */
@Entity
@Table(name = "planeamento_vinho")
public class PlaneamentoVinho extends BaseEntity {

    public static final String PREFIXO = "PLN";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vinha_id")
    private Vinha vinha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    @Column(precision = 12, scale = 2)
    private BigDecimal quantidadePrevistaKg;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPrevistaVindima;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_destino_id")
    private Adega adegaDestino;

    @Column(length = 160)
    private String nomeVinho;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentagem;

    public Vinha getVinha() { return vinha; }
    public void setVinha(Vinha vinha) { this.vinha = vinha; }

    public Casta getCasta() { return casta; }
    public void setCasta(Casta casta) { this.casta = casta; }

    public BigDecimal getQuantidadePrevistaKg() { return quantidadePrevistaKg; }
    public void setQuantidadePrevistaKg(BigDecimal quantidadePrevistaKg) { this.quantidadePrevistaKg = quantidadePrevistaKg; }

    public LocalDate getDataPrevistaVindima() { return dataPrevistaVindima; }
    public void setDataPrevistaVindima(LocalDate dataPrevistaVindima) { this.dataPrevistaVindima = dataPrevistaVindima; }

    public Adega getAdegaDestino() { return adegaDestino; }
    public void setAdegaDestino(Adega adegaDestino) { this.adegaDestino = adegaDestino; }

    public String getNomeVinho() { return nomeVinho; }
    public void setNomeVinho(String nomeVinho) { this.nomeVinho = nomeVinho; }

    public BigDecimal getPercentagem() { return percentagem; }
    public void setPercentagem(BigDecimal percentagem) { this.percentagem = percentagem; }
}
