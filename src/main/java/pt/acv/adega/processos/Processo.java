package pt.acv.adega.processos;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.common.BaseEntity;
import pt.acv.adega.fichas.Trabalhador;

import java.time.LocalDateTime;

/**
 * Base comum a todos os processos das fases. Reune os campos que os requisitos
 * pedem sempre: "quem fez, data e hora de inicio, data e hora de fim, que meios
 * utilizou, que metodos utilizou". Cada processo tem estado (aberto/fechado) e
 * guarda quem o abriu (para o controlo de acesso: o autor e o admin).
 */
@MappedSuperclass
public abstract class Processo extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoProcesso estado = EstadoProcesso.ABERTO;

    /** Quem fez / acompanhou (ficha de trabalhador). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsavel_id")
    private Trabalhador responsavel;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dataHoraInicio;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dataHoraFim;

    @Column(length = 500)
    private String meios;

    @Column(length = 500)
    private String metodos;

    @Column(length = 1000)
    private String observacoes;

    /** Username de quem abriu o processo (controlo de acesso). */
    @Column(nullable = false, updatable = false, length = 60)
    private String criadoPor;

    private LocalDateTime dataFecho;

    public EstadoProcesso getEstado() { return estado; }
    public void setEstado(EstadoProcesso estado) { this.estado = estado; }

    public Trabalhador getResponsavel() { return responsavel; }
    public void setResponsavel(Trabalhador responsavel) { this.responsavel = responsavel; }

    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }

    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(LocalDateTime dataHoraFim) { this.dataHoraFim = dataHoraFim; }

    public String getMeios() { return meios; }
    public void setMeios(String meios) { this.meios = meios; }

    public String getMetodos() { return metodos; }
    public void setMetodos(String metodos) { this.metodos = metodos; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }

    public LocalDateTime getDataFecho() { return dataFecho; }
    public void setDataFecho(LocalDateTime dataFecho) { this.dataFecho = dataFecho; }

    @Transient
    public boolean isAberto() { return estado == EstadoProcesso.ABERTO; }
}
