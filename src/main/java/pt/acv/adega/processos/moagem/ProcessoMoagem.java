package pt.acv.adega.processos.moagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Vinha;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcela;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;

import java.math.BigDecimal;
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

    /**
     * Origem da uva: a linha do planeamento (vinho + parcela) que foi vindimada
     * (rastreabilidade uva -> mosto). Opcional.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "origem_vindima_linha_id")
    private LinhaPlaneamentoParcela origemVindima;

    /** Vinha de origem (redundante/alternativa a vindima). Opcional. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vinha_id")
    private Vinha vinha;

    @OneToMany(mappedBy = "moagem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Enchimento> enchimentos = new ArrayList<>();

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public LinhaPlaneamentoParcela getOrigemVindima() { return origemVindima; }
    public void setOrigemVindima(LinhaPlaneamentoParcela origemVindima) { this.origemVindima = origemVindima; }

    public Vinha getVinha() { return vinha; }
    public void setVinha(Vinha vinha) { this.vinha = vinha; }

    public List<Enchimento> getEnchimentos() { return enchimentos; }
    public void setEnchimentos(List<Enchimento> enchimentos) { this.enchimentos = enchimentos; }

    /** Total de uva efetivamente moída (soma dos Kg dos enchimentos). */
    @Transient
    public BigDecimal getTotalMoidoKg() {
        return enchimentos.stream()
                .map(e -> e.getQuantidadeMoidaKg() == null ? BigDecimal.ZERO : e.getQuantidadeMoidaKg())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total de litros de mosto gerados (soma dos litros dos enchimentos). */
    @Transient
    public BigDecimal getTotalLitrosMosto() {
        return enchimentos.stream()
                .map(e -> e.getLitros() == null ? BigDecimal.ZERO : e.getLitros())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
