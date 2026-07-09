package pt.acv.adega.processos.remontagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Talha;

/**
 * Uma talha intervencionada numa remontagem, com o visto de "concluído".
 * Representa cada talha (com mosto em fermentação) da adega escolhida.
 */
@Entity
@Table(name = "remontagem_talha")
public class RemontagemTalha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remontagem_id")
    private ProcessoRemontagem remontagem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "talha_id")
    private Talha talha;

    /** Visto: a remontagem desta talha foi concluída. */
    private boolean concluido;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProcessoRemontagem getRemontagem() { return remontagem; }
    public void setRemontagem(ProcessoRemontagem remontagem) { this.remontagem = remontagem; }

    public Talha getTalha() { return talha; }
    public void setTalha(Talha talha) { this.talha = talha; }

    public boolean isConcluido() { return concluido; }
    public void setConcluido(boolean concluido) { this.concluido = concluido; }
}
