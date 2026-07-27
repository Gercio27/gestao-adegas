package pt.acv.adega.processos.movimentovinho;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;
import pt.acv.adega.produtos.Mosto;

import java.math.BigDecimal;

/**
 * Fase 5 - Entradas, saidas e transfegas de vinho a granel.
 * ENTRADA: cria vinho a granel num recipiente (externo, emite DA).
 * SAIDA: da baixa de vinho a granel existente (emite DA).
 * TRANSFEGA: move litros de um recipiente para outro dentro do mesmo vinho.
 * INTRA_EMP: saida intra-empresa - transfere vinho/mosto a granel (camiao) ou
 *   garrafas (contentores) entre adegas/armazens da empresa, com D.A. anexado.
 */
@Entity
@Table(name = "processo_movimento_vinho")
public class ProcessoMovimentoVinho extends Processo {

    public static final String PREFIXO = "MVG";
    public static final Fase FASE = Fase.FASE_5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentoVinho tipo = TipoMovimentoVinho.TRANSFEGA;

    @Column(precision = 12, scale = 2)
    private BigDecimal litros;

    /** Nome do vinho (ENTRADA externa; nas outras vem do mosto de origem). */
    @Column(length = 160)
    private String nomeVinho;

    // --- Destino (ENTRADA e TRANSFEGA) ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "talha_destino_id")
    private Talha talhaDestino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deposito_destino_id")
    private Deposito depositoDestino;

    /** Casta (ENTRADA externa). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta casta;

    // --- Origem (SAIDA e TRANSFEGA) ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mosto_origem_id")
    private Mosto mostoOrigem;

    @Column(length = 160)
    private String contraparte;

    @Column(length = 200)
    private String transporte;

    @Column(length = 20)
    private String numeroDA;

    /** TRANSFEGA: id do mosto de destino afetado (para reverter). */
    @Column(name = "mosto_destino_id")
    private Long mostoDestinoId;

    /** TRANSFEGA: o mosto de destino foi criado por esta transfega (para reverter). */
    @Column(nullable = false)
    private boolean destinoCriado = false;

    // ===== SAIDA_INTRA-EMPRESA (transferencia entre adegas/armazens) =====

    /**
     * O que se transfere: "GRANEL" (mosto/vinho em talha/deposito),
     * "ENGARRAFADO" (garrafas em contentor) ou "BAG_IN_BOX" (unidades em palete).
     */
    @Column(length = 12)
    private String produtoTipo;

    /** ENGARRAFADO: contentor de origem e de destino. */
    @Column(name = "contentor_origem_id")
    private Long contentorOrigemId;

    @Column(name = "contentor_destino_id")
    private Long contentorDestinoId;

    /** ENGARRAFADO: nº de garrafas a transferir (ou o contentor completo). */
    private Integer garrafas;

    @Column(nullable = false)
    private boolean contentorCompleto = false;

    /** GRANEL: matricula do camiao (transporte). */
    @Column(length = 20)
    private String matriculaCamiao;

    @Column(length = 160)
    private String responsavelEntrega;

    @Column(length = 160)
    private String responsavelRececao;

    /** Descricoes legiveis da origem e do destino (adega/armazem + recipiente). */
    @Column(length = 250)
    private String origemLocalDescricao;

    @Column(length = 250)
    private String destinoLocalDescricao;

    /** PDF externo do Documento de Acompanhamento (D.A.), guardado na base de dados. */
    @Lob
    @Column(name = "da_pdf")
    private byte[] daPdf;

    @Column(length = 200)
    private String daPdfNome;

    @Column(length = 100)
    private String daPdfTipo;

    /** Selecao do recipiente de destino no formulario ("TALHA:id" / "DEPOSITO:id"). Nao persiste. */
    @Transient
    private String destinoRef;

    public Long getMostoDestinoId() { return mostoDestinoId; }
    public void setMostoDestinoId(Long mostoDestinoId) { this.mostoDestinoId = mostoDestinoId; }

    public boolean isDestinoCriado() { return destinoCriado; }
    public void setDestinoCriado(boolean destinoCriado) { this.destinoCriado = destinoCriado; }

    public TipoMovimentoVinho getTipo() { return tipo; }
    public void setTipo(TipoMovimentoVinho tipo) { this.tipo = tipo; }

    public BigDecimal getLitros() { return litros; }
    public void setLitros(BigDecimal litros) { this.litros = litros; }

    public String getNomeVinho() { return nomeVinho; }
    public void setNomeVinho(String nomeVinho) { this.nomeVinho = nomeVinho; }

    public Talha getTalhaDestino() { return talhaDestino; }
    public void setTalhaDestino(Talha talhaDestino) { this.talhaDestino = talhaDestino; }

    public Deposito getDepositoDestino() { return depositoDestino; }
    public void setDepositoDestino(Deposito depositoDestino) { this.depositoDestino = depositoDestino; }

    public Casta getCasta() { return casta; }
    public void setCasta(Casta casta) { this.casta = casta; }

    public Mosto getMostoOrigem() { return mostoOrigem; }
    public void setMostoOrigem(Mosto mostoOrigem) { this.mostoOrigem = mostoOrigem; }

    public String getContraparte() { return contraparte; }
    public void setContraparte(String contraparte) { this.contraparte = contraparte; }

    public String getTransporte() { return transporte; }
    public void setTransporte(String transporte) { this.transporte = transporte; }

    public String getNumeroDA() { return numeroDA; }
    public void setNumeroDA(String numeroDA) { this.numeroDA = numeroDA; }

    public String getDestinoRef() {
        if (destinoRef != null) return destinoRef;
        if (talhaDestino != null) return "TALHA:" + talhaDestino.getId();
        if (depositoDestino != null) return "DEPOSITO:" + depositoDestino.getId();
        return "";
    }
    public void setDestinoRef(String destinoRef) { this.destinoRef = destinoRef; }

    @Transient
    public String getDestinoDescricao() {
        if (talhaDestino != null) return "Talha " + talhaDestino.getIdentificacao();
        if (depositoDestino != null) return "Depósito " + depositoDestino.getIdentificacao();
        return "—";
    }

    // ----- getters/setters intra-empresa -----

    public String getProdutoTipo() { return produtoTipo; }
    public void setProdutoTipo(String produtoTipo) { this.produtoTipo = produtoTipo; }

    public Long getContentorOrigemId() { return contentorOrigemId; }
    public void setContentorOrigemId(Long contentorOrigemId) { this.contentorOrigemId = contentorOrigemId; }

    public Long getContentorDestinoId() { return contentorDestinoId; }
    public void setContentorDestinoId(Long contentorDestinoId) { this.contentorDestinoId = contentorDestinoId; }

    public Integer getGarrafas() { return garrafas; }
    public void setGarrafas(Integer garrafas) { this.garrafas = garrafas; }

    public boolean isContentorCompleto() { return contentorCompleto; }
    public void setContentorCompleto(boolean contentorCompleto) { this.contentorCompleto = contentorCompleto; }

    public String getMatriculaCamiao() { return matriculaCamiao; }
    public void setMatriculaCamiao(String matriculaCamiao) { this.matriculaCamiao = matriculaCamiao; }

    public String getResponsavelEntrega() { return responsavelEntrega; }
    public void setResponsavelEntrega(String responsavelEntrega) { this.responsavelEntrega = responsavelEntrega; }

    public String getResponsavelRececao() { return responsavelRececao; }
    public void setResponsavelRececao(String responsavelRececao) { this.responsavelRececao = responsavelRececao; }

    public String getOrigemLocalDescricao() { return origemLocalDescricao; }
    public void setOrigemLocalDescricao(String origemLocalDescricao) { this.origemLocalDescricao = origemLocalDescricao; }

    public String getDestinoLocalDescricao() { return destinoLocalDescricao; }
    public void setDestinoLocalDescricao(String destinoLocalDescricao) { this.destinoLocalDescricao = destinoLocalDescricao; }

    public byte[] getDaPdf() { return daPdf; }
    public void setDaPdf(byte[] daPdf) { this.daPdf = daPdf; }

    public String getDaPdfNome() { return daPdfNome; }
    public void setDaPdfNome(String daPdfNome) { this.daPdfNome = daPdfNome; }

    public String getDaPdfTipo() { return daPdfTipo; }
    public void setDaPdfTipo(String daPdfTipo) { this.daPdfTipo = daPdfTipo; }

    @Transient
    public boolean isTemDaPdf() { return daPdf != null && daPdf.length > 0; }

    /** True quando o movimento mexe em produto acabado (garrafas ou bag-in-box). */
    @Transient
    public boolean isEngarrafado() {
        return tipo == TipoMovimentoVinho.INTRA_EMP
                && ("ENGARRAFADO".equals(produtoTipo) || "BAG_IN_BOX".equals(produtoTipo));
    }

    /** Em que ficha de contentor ir buscar o stock deste movimento. */
    @Transient
    public pt.acv.adega.fichas.TipoEmbalagem getTipoEmbalagem() {
        return "BAG_IN_BOX".equals(produtoTipo)
                ? pt.acv.adega.fichas.TipoEmbalagem.BAG_IN_BOX
                : pt.acv.adega.fichas.TipoEmbalagem.GARRAFA;
    }

    /** "garrafa(s)" ou "unidade(s)", para os textos dos ecras. */
    @Transient
    public String getUnidadeNome() {
        return isEngarrafado() ? getTipoEmbalagem().getUnidade() : "litros";
    }
}
