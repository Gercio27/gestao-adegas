package pt.acv.adega.processos.passagem;

import jakarta.persistence.*;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;

import java.util.ArrayList;
import java.util.List;

/**
 * Processo "O vinho está pronto" (Fase 4, ponto 4.5): passar mosto para vinho
 * pronto a granel. Pode incluir uma ou varias talhas/cubas. Ao fechar, os
 * mostos selecionados passam ao estado VINHO_GRANEL e entram no mapa de
 * existencias de vinhos a granel.
 */
@Entity
@Table(name = "processo_passagem_vinho")
public class ProcessoPassagemVinho extends Processo {

    public static final String PREFIXO = "PVG";
    public static final Fase FASE = Fase.FASE_4;

    /** Codigos dos mostos convertidos (legivel). */
    @Column(length = 1000)
    private String mostosDescricao;

    /** Ids dos mostos convertidos, separados por virgula (para reverter). */
    @Column(length = 1000)
    private String mostosIdsCsv;

    /** Selecao do formulario (ids dos mostos em fermentacao). */
    @Transient
    private List<Long> mostoIds = new ArrayList<>();

    public String getMostosDescricao() { return mostosDescricao; }
    public void setMostosDescricao(String mostosDescricao) { this.mostosDescricao = mostosDescricao; }

    public String getMostosIdsCsv() { return mostosIdsCsv; }
    public void setMostosIdsCsv(String mostosIdsCsv) { this.mostosIdsCsv = mostosIdsCsv; }

    public List<Long> getMostoIds() { return mostoIds; }
    public void setMostoIds(List<Long> mostoIds) { this.mostoIds = mostoIds; }
}
