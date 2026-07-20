package pt.acv.adega.tratamentos;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.common.BaseEntity;
import pt.acv.adega.fichas.Adega;

import java.time.LocalDate;

/**
 * Análise a um vinho (mosto ou granel) numa adega. Funciona como o tratamento
 * enológico, mas em vez de descrever um tratamento aplicado guarda o PDF da
 * análise (na base de dados). Repetível: histórico de análises do vinho.
 */
@Entity
@Table(name = "analise_vinho")
public class AnaliseVinho extends BaseEntity {

    public static final String PREFIXO = "ANL";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoriaVinho categoria = CategoriaVinho.MOSTO;

    @Column(length = 160)
    private String vinhoNome;

    /** Snapshot dos recipientes (talhas/depósitos + litros) onde o vinho estava. */
    @Column(length = 600)
    private String recipientesDescricao;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataAnalise;

    @Column(length = 1000)
    private String descricao;

    // ----- PDF da análise (guardado na base de dados) -----

    @Lob
    @Column(name = "analise_pdf")
    private byte[] analisePdf;

    @Column(length = 200)
    private String analisePdfNome;

    @Column(length = 100)
    private String analisePdfTipo;

    @Column(length = 120)
    private String criadoPor;

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public CategoriaVinho getCategoria() { return categoria; }
    public void setCategoria(CategoriaVinho categoria) { this.categoria = categoria; }

    public String getVinhoNome() { return vinhoNome; }
    public void setVinhoNome(String vinhoNome) { this.vinhoNome = vinhoNome; }

    public String getRecipientesDescricao() { return recipientesDescricao; }
    public void setRecipientesDescricao(String recipientesDescricao) { this.recipientesDescricao = recipientesDescricao; }

    public LocalDate getDataAnalise() { return dataAnalise; }
    public void setDataAnalise(LocalDate dataAnalise) { this.dataAnalise = dataAnalise; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public byte[] getAnalisePdf() { return analisePdf; }
    public void setAnalisePdf(byte[] analisePdf) { this.analisePdf = analisePdf; }

    public String getAnalisePdfNome() { return analisePdfNome; }
    public void setAnalisePdfNome(String analisePdfNome) { this.analisePdfNome = analisePdfNome; }

    public String getAnalisePdfTipo() { return analisePdfTipo; }
    public void setAnalisePdfTipo(String analisePdfTipo) { this.analisePdfTipo = analisePdfTipo; }

    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }

    @Transient
    public boolean isTemPdf() { return analisePdf != null && analisePdf.length > 0; }
}
