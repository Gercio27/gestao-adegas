package pt.acv.adega.processos.vindima;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.fichas.Vinha;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;

import java.math.BigDecimal;

/**
 * Processo de Vindima (Fase 2). Regista a colheita e a entrega da uva na adega,
 * com a origem (propria/terceiros) e as quantidades. A rastreabilidade comeca
 * aqui: a origem da uva fica ligada a vinha vindimada.
 */
@Entity
@Table(name = "processo_vindima")
public class ProcessoVindima extends Processo {

    public static final String PREFIXO = "VDM";
    public static final Fase FASE = Fase.FASE_2;

    /** Vinha vindimada (propria ou de terceiros previamente registada como tal). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vinha_id")
    private Vinha vinha;

    /** Casta principal colhida (opcional; ajuda a caracterizar a uva). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "casta_id")
    private Casta castaPrincipal;

    /** Quantidade colhida, em quilogramas. */
    @Column(precision = 12, scale = 2)
    private BigDecimal quantidadeKg;

    /** Adega onde a uva foi entregue. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_destino_id")
    private Adega adegaDestino;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrigemUva origem = OrigemUva.PROPRIA;

    /** Nome do terceiro (quando a origem envolve terceiros). */
    @Column(length = 120)
    private String terceiro;

    /** Vasilame usado na apanha (baldes/contentores/reboque). Texto por agora. */
    @Column(length = 200)
    private String vasilame;

    /** Meio de transporte ate a adega. Texto por agora. */
    @Column(length = 200)
    private String transporte;

    public Vinha getVinha() { return vinha; }
    public void setVinha(Vinha vinha) { this.vinha = vinha; }

    public Casta getCastaPrincipal() { return castaPrincipal; }
    public void setCastaPrincipal(Casta castaPrincipal) { this.castaPrincipal = castaPrincipal; }

    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public void setQuantidadeKg(BigDecimal quantidadeKg) { this.quantidadeKg = quantidadeKg; }

    public Adega getAdegaDestino() { return adegaDestino; }
    public void setAdegaDestino(Adega adegaDestino) { this.adegaDestino = adegaDestino; }

    public OrigemUva getOrigem() { return origem; }
    public void setOrigem(OrigemUva origem) { this.origem = origem; }

    public String getTerceiro() { return terceiro; }
    public void setTerceiro(String terceiro) { this.terceiro = terceiro; }

    public String getVasilame() { return vasilame; }
    public void setVasilame(String vasilame) { this.vasilame = vasilame; }

    public String getTransporte() { return transporte; }
    public void setTransporte(String transporte) { this.transporte = transporte; }
}
