package pt.acv.adega.processos.remontagem;

import jakarta.persistence.*;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;

import java.util.ArrayList;
import java.util.List;

/**
 * Processo de Remontagem (Fase 4, ponto 4.1) - mexer os mostos. Permite
 * registar a operacao indicando varias talhas/cubas de uma so vez, sem ter de
 * ir recipiente a recipiente. Nao altera volumes (e um registo da intervencao).
 */
@Entity
@Table(name = "processo_remontagem")
public class ProcessoRemontagem extends Processo {

    public static final String PREFIXO = "REM";
    public static final Fase FASE = Fase.FASE_4;

    /** Descricao (legivel) dos recipientes intervencionados, ja resolvida. */
    @Column(length = 1000)
    private String recipientes;

    /** Selecao vinda do formulario (multi-select de "TALHA:id"/"DEPOSITO:id"). */
    @Transient
    private List<String> recipienteRefs = new ArrayList<>();

    public String getRecipientes() { return recipientes; }
    public void setRecipientes(String recipientes) { this.recipientes = recipientes; }

    public List<String> getRecipienteRefs() { return recipienteRefs; }
    public void setRecipienteRefs(List<String> recipienteRefs) { this.recipienteRefs = recipienteRefs; }
}
