package pt.acv.adega.processos.loteamento;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.common.BaseEntity;
import pt.acv.adega.fichas.Adega;

import java.time.LocalDate;

/**
 * Fase 6 - Loteamento. Cabecalho do lote: um vinho novo, com codigo automatico
 * (o codigo do lote). 6.1 cria o plano (linhas com a quantidade prevista por
 * deposito, so intencao). 6.2 constroi o lote em uma ou varias construcoes
 * (transfegas que dao baixa nas origens e juntam no vinho do lote).
 */
@Entity
@Table(name = "loteamento")
public class Loteamento extends BaseEntity {

    public static final String PREFIXO = "LTV";

    @NotBlank
    @Column(nullable = false, length = 160)
    private String nome;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPlaneamento;

    @Column(nullable = false)
    private boolean concluido = false;

    @Column(length = 80)
    private String criadoPor;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public LocalDate getDataPlaneamento() { return dataPlaneamento; }
    public void setDataPlaneamento(LocalDate dataPlaneamento) { this.dataPlaneamento = dataPlaneamento; }

    public boolean isConcluido() { return concluido; }
    public void setConcluido(boolean concluido) { this.concluido = concluido; }

    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
}
