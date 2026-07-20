package pt.acv.adega.processos.comercial;

import jakarta.persistence.*;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;
import pt.acv.adega.produtos.VinhoEngarrafado;

/**
 * Processo de Passagem ao setor comercial (Fase 8). Entrega uma quantidade de
 * garrafas de um vinho engarrafado (rotulado) a um cliente e emite a Nota de
 * Entrega. Ao fechar, da baixa das garrafas disponiveis do produto acabado.
 */
@Entity
@Table(name = "processo_comercial")
public class ProcessoPassagemComercial extends Processo {

    public static final String PREFIXO = "PCO";
    public static final Fase FASE = Fase.FASE_8;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "engarrafado_id")
    private VinhoEngarrafado engarrafado;

    /** Contentor (rotulado) de onde saem as garrafas — define o vinho e o local. */
    @Column(name = "contentor_id")
    private Long contentorId;

    /** Vinho + local (armazem/adega) de onde saiu, para a nota de entrega. */
    @Column(length = 250)
    private String origemDescricao;

    @Column(nullable = false)
    private int quantidadeGarrafas;

    public Long getContentorId() { return contentorId; }
    public void setContentorId(Long contentorId) { this.contentorId = contentorId; }

    public String getOrigemDescricao() { return origemDescricao; }
    public void setOrigemDescricao(String origemDescricao) { this.origemDescricao = origemDescricao; }

    @Column(length = 160)
    private String destinatario;

    @Column(length = 250)
    private String moradaDestino;

    /** Nome de quem recebe/confere a entrega no destino (assina a nota). */
    @Column(length = 160)
    private String responsavelRececao;

    /** Numero da nota de entrega, atribuido no fecho (ex.: NE-000001). */
    @Column(length = 20)
    private String numeroNota;

    public VinhoEngarrafado getEngarrafado() { return engarrafado; }
    public void setEngarrafado(VinhoEngarrafado engarrafado) { this.engarrafado = engarrafado; }

    public int getQuantidadeGarrafas() { return quantidadeGarrafas; }
    public void setQuantidadeGarrafas(int quantidadeGarrafas) { this.quantidadeGarrafas = quantidadeGarrafas; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getMoradaDestino() { return moradaDestino; }
    public void setMoradaDestino(String moradaDestino) { this.moradaDestino = moradaDestino; }

    public String getResponsavelRececao() { return responsavelRececao; }
    public void setResponsavelRececao(String responsavelRececao) { this.responsavelRececao = responsavelRececao; }

    public String getNumeroNota() { return numeroNota; }
    public void setNumeroNota(String numeroNota) { this.numeroNota = numeroNota; }
}
