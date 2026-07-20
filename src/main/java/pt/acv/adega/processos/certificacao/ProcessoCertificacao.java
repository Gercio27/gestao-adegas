package pt.acv.adega.processos.certificacao;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.VinhoEngarrafado;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    /** Amostra enviada ao CVR/IVV (a granel): um dos mostos certificados. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vinho_granel_id")
    private Mosto vinhoGranel;

    /** Amostra enviada ao CVR/IVV (engarrafado): um dos lotes certificados. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "engarrafado_id")
    private VinhoEngarrafado engarrafado;

    /** Ids de todos os itens certificados (mostos ou engarrafados), separados por vírgula. */
    @Column(length = 1000)
    private String itensIdsCsv;

    /** Descrição legível dos itens certificados. */
    @Column(length = 1000)
    private String itensDescricao;

    /** Seleção do formulário (ids dos depósitos/lotes a certificar). */
    @Transient
    private List<Long> itemIds = new ArrayList<>();

    /** Id do item escolhido como amostra (do formulário). */
    @Transient
    private Long amostraId;

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

    // ----- Engarrafado: de que contentor saem as garrafas para certificação -----

    /** Contentor de onde saem as garrafas para certificação (alvo ENGARRAFADO). */
    @Column(name = "contentor_id")
    private Long contentorId;

    /** Descrição legível do contentor (nome + local), para o resumo. */
    @Column(length = 200)
    private String contentorDescricao;

    /** Quantas garrafas foram tiradas para certificação. */
    @Column(nullable = false)
    private int garrafasCertificacao = 0;

    // ----- PDF do certificado (guardado na base de dados) -----

    @Lob
    @Column(name = "certificado_pdf")
    private byte[] certificadoPdf;

    @Column(length = 200)
    private String certificadoPdfNome;

    @Column(length = 100)
    private String certificadoPdfTipo;

    public Long getContentorId() { return contentorId; }
    public void setContentorId(Long contentorId) { this.contentorId = contentorId; }

    public String getContentorDescricao() { return contentorDescricao; }
    public void setContentorDescricao(String contentorDescricao) { this.contentorDescricao = contentorDescricao; }

    public int getGarrafasCertificacao() { return garrafasCertificacao; }
    public void setGarrafasCertificacao(int garrafasCertificacao) { this.garrafasCertificacao = garrafasCertificacao; }

    public byte[] getCertificadoPdf() { return certificadoPdf; }
    public void setCertificadoPdf(byte[] certificadoPdf) { this.certificadoPdf = certificadoPdf; }

    public String getCertificadoPdfNome() { return certificadoPdfNome; }
    public void setCertificadoPdfNome(String certificadoPdfNome) { this.certificadoPdfNome = certificadoPdfNome; }

    public String getCertificadoPdfTipo() { return certificadoPdfTipo; }
    public void setCertificadoPdfTipo(String certificadoPdfTipo) { this.certificadoPdfTipo = certificadoPdfTipo; }

    @Transient
    public boolean isTemPdf() { return certificadoPdf != null && certificadoPdf.length > 0; }

    public AlvoCertificacao getAlvo() { return alvo; }
    public void setAlvo(AlvoCertificacao alvo) { this.alvo = alvo; }

    public Mosto getVinhoGranel() { return vinhoGranel; }
    public void setVinhoGranel(Mosto vinhoGranel) { this.vinhoGranel = vinhoGranel; }

    public VinhoEngarrafado getEngarrafado() { return engarrafado; }
    public void setEngarrafado(VinhoEngarrafado engarrafado) { this.engarrafado = engarrafado; }

    public String getItensIdsCsv() { return itensIdsCsv; }
    public void setItensIdsCsv(String itensIdsCsv) { this.itensIdsCsv = itensIdsCsv; }

    public String getItensDescricao() { return itensDescricao; }
    public void setItensDescricao(String itensDescricao) { this.itensDescricao = itensDescricao; }

    public List<Long> getItemIds() { return itemIds; }
    public void setItemIds(List<Long> itemIds) { this.itemIds = itemIds; }

    public Long getAmostraId() { return amostraId; }
    public void setAmostraId(Long amostraId) { this.amostraId = amostraId; }

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
