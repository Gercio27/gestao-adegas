package pt.acv.adega.tratamentos;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.common.BaseEntity;
import pt.acv.adega.fichas.Adega;

import java.time.LocalDate;

/**
 * Tratamento enológico aplicado a um vinho (mosto ou granel) numa adega, numa
 * data. É transversal às fases e repetível: cada registo é um tratamento; o
 * histórico de um vinho é a lista dos seus registos ao longo do tempo.
 */
@Entity
@Table(name = "tratamento_enologico")
public class TratamentoEnologico extends BaseEntity {

    public static final String PREFIXO = "TRT";

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
    private LocalDate dataTratamento;

    /** Tratamento aplicado (produto, dose, notas). */
    @Column(length = 1000)
    private String descricao;

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

    public LocalDate getDataTratamento() { return dataTratamento; }
    public void setDataTratamento(LocalDate dataTratamento) { this.dataTratamento = dataTratamento; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
}
