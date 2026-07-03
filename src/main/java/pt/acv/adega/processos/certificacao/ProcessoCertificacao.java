package pt.acv.adega.processos.certificacao;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.VinhoEngarrafado;

import java.time.LocalDate;

/**
 * Pedido de certificacao (Fase 5.5 para vinho a granel; 6.4 para engarrafado).
 * Regista o pedido, a entidade (IVV/CVR), o resultado e a validade. Ao fechar
 * com resultado APROVADO, marca o produto como certificado.
 */
@Entity
@Table(name = "processo_certificacao")
public class ProcessoCertificacao extends Processo {

    public static final String PREFIXO = "CER";
    public static final Fase FASE = Fase.FASE_5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AlvoCertificacao alvo = AlvoCertificacao.GRANEL;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vinho_granel_id")
    private Mosto vinhoGranel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "engarrafado_id")
    private VinhoEngarrafado engarrafado;

    @Column(length = 120)
    private String entidade;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ResultadoCertificacao resultado = ResultadoCertificacao.PENDENTE;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataResultado;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validade;

    @Column(length = 60)
    private String numeroCertificado;

    public AlvoCertificacao getAlvo() { return alvo; }
    public void setAlvo(AlvoCertificacao alvo) { this.alvo = alvo; }

    public Mosto getVinhoGranel() { return vinhoGranel; }
    public void setVinhoGranel(Mosto vinhoGranel) { this.vinhoGranel = vinhoGranel; }

    public VinhoEngarrafado getEngarrafado() { return engarrafado; }
    public void setEngarrafado(VinhoEngarrafado engarrafado) { this.engarrafado = engarrafado; }

    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }

    public LocalDate getDataPedido() { return dataPedido; }
    public void setDataPedido(LocalDate dataPedido) { this.dataPedido = dataPedido; }

    public ResultadoCertificacao getResultado() { return resultado; }
    public void setResultado(ResultadoCertificacao resultado) { this.resultado = resultado; }

    public LocalDate getDataResultado() { return dataResultado; }
    public void setDataResultado(LocalDate dataResultado) { this.dataResultado = dataResultado; }

    public LocalDate getValidade() { return validade; }
    public void setValidade(LocalDate validade) { this.validade = validade; }

    public String getNumeroCertificado() { return numeroCertificado; }
    public void setNumeroCertificado(String numeroCertificado) { this.numeroCertificado = numeroCertificado; }

    @Transient
    public String getAlvoDescricao() {
        if (alvo == AlvoCertificacao.GRANEL) return vinhoGranel != null ? vinhoGranel.getCodigo() : "—";
        return engarrafado != null ? (engarrafado.getCodigo() + " · " + engarrafado.getNome()) : "—";
    }
}
