package pt.acv.adega.processos.moagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Vinha;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;
import pt.acv.adega.processos.vindima.ProcessoVindima;

import java.util.ArrayList;
import java.util.List;

/**
 * Processo de Moagem e enchimento de talhas/cubas (Fase 3, ponto 3.1).
 * Ao fechar, gera automaticamente as fichas de mosto que resultaram, uma por
 * cada talha/cuba cheia (linhas de enchimento).
 */
@Entity
@Table(name = "processo_moagem")
public class ProcessoMoagem extends Processo {

    public static final String PREFIXO = "MOA";
    public static final Fase FASE = Fase.FASE_3;

    /** Adega onde decorre a moagem. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    /** Vindima de origem da uva (rastreabilidade uva -> mosto). Opcional. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vindima_id")
    private ProcessoVindima vindima;

    /** Vinha de origem (redundante/alternativa a vindima). Opcional. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vinha_id")
    private Vinha vinha;

    @OneToMany(mappedBy = "moagem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Enchimento> enchimentos = new ArrayList<>();

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public ProcessoVindima getVindima() { return vindima; }
    public void setVindima(ProcessoVindima vindima) { this.vindima = vindima; }

    public Vinha getVinha() { return vinha; }
    public void setVinha(Vinha vinha) { this.vinha = vinha; }

    public List<Enchimento> getEnchimentos() { return enchimentos; }
    public void setEnchimentos(List<Enchimento> enchimentos) { this.enchimentos = enchimentos; }
}
