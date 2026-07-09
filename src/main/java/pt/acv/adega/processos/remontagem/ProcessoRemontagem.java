package pt.acv.adega.processos.remontagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;

import java.util.ArrayList;
import java.util.List;

/**
 * Processo de Remontagem (Fase 4, ponto 4.1) - mexer os mostos. Escolhe-se a
 * adega e, para cada talha dessa adega com mosto em fermentacao, marca-se o
 * visto de "concluido". Nao altera volumes (e um registo da intervencao).
 */
@Entity
@Table(name = "processo_remontagem")
public class ProcessoRemontagem extends Processo {

    public static final String PREFIXO = "REM";
    public static final Fase FASE = Fase.FASE_4;

    /** Adega onde decorre a remontagem. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    /** Talhas intervencionadas (da adega escolhida), com o visto de concluido. */
    @OneToMany(mappedBy = "remontagem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<RemontagemTalha> talhas = new ArrayList<>();

    /** Descricao (legivel) das talhas intervencionadas, para listas/detalhe. */
    @Column(length = 1000)
    private String recipientes;

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public List<RemontagemTalha> getTalhas() { return talhas; }
    public void setTalhas(List<RemontagemTalha> talhas) { this.talhas = talhas; }

    public String getRecipientes() { return recipientes; }
    public void setRecipientes(String recipientes) { this.recipientes = recipientes; }

    /** Nº de talhas com a remontagem concluída. */
    @Transient
    public long getConcluidas() {
        return talhas.stream().filter(RemontagemTalha::isConcluido).count();
    }
}
