package pt.acv.adega.movimentos;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Livro de movimentos: uma linha por cada entrada ou saida numa talha,
 * deposito, contentor, palete, consumivel ou stock rotulado.
 *
 * Serve para o historico de cada ficha: o que entrou, o que saiu, quando, por
 * causa de que processo e quem o fez. Os processos escrevem aqui ao fechar (e
 * ao reabrir, com o sinal trocado).
 */
@Entity
@Table(name = "movimento_stock", indexes = {
        @Index(name = "idx_mov_alvo", columnList = "tipo_alvo,alvo_id")
})
public class MovimentoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alvo", nullable = false, length = 24)
    private TipoAlvo tipoAlvo;

    @Column(name = "alvo_id", nullable = false)
    private Long alvoId;

    /** Nome legivel da ficha, para o historico nao depender dela existir. */
    @Column(name = "alvo_nome", length = 200)
    private String alvoNome;

    /** Local (adega/armazem) onde estava, para se poder filtrar por adega. */
    @Column(length = 200)
    private String local;

    /** Positivo = entrou; negativo = saiu. */
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal quantidade = BigDecimal.ZERO;

    @Column(length = 12)
    private String unidade;

    /** Quanto ficou depois deste movimento (para conferir de relance). */
    @Column(precision = 14, scale = 2)
    private BigDecimal saldo;

    /** Fase/processo que o causou (ex.: "Moagem MOA-000001"). */
    @Column(length = 120)
    private String origem;

    /** O que se passou, em linguagem corrente. */
    @Column(length = 300)
    private String descricao;

    /** Vinho envolvido, quando faz sentido. */
    @Column(name = "vinho_nome", length = 160)
    private String vinhoNome;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(length = 80)
    private String utilizador;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoAlvo getTipoAlvo() { return tipoAlvo; }
    public void setTipoAlvo(TipoAlvo tipoAlvo) { this.tipoAlvo = tipoAlvo; }

    public Long getAlvoId() { return alvoId; }
    public void setAlvoId(Long alvoId) { this.alvoId = alvoId; }

    public String getAlvoNome() { return alvoNome; }
    public void setAlvoNome(String alvoNome) { this.alvoNome = alvoNome; }

    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getVinhoNome() { return vinhoNome; }
    public void setVinhoNome(String vinhoNome) { this.vinhoNome = vinhoNome; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public String getUtilizador() { return utilizador; }
    public void setUtilizador(String utilizador) { this.utilizador = utilizador; }

    @Transient
    public boolean isEntrada() { return quantidade != null && quantidade.signum() > 0; }

    /** "+120" ou "-30", ja com a unidade. */
    @Transient
    public String getQuantidadeTexto() {
        if (quantidade == null) return "—";
        String sinal = quantidade.signum() > 0 ? "+" : "";
        return sinal + quantidade.stripTrailingZeros().toPlainString()
                + (unidade != null ? " " + unidade : "");
    }
}
