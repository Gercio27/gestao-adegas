package pt.acv.adega.processos.moagem;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Vinha;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcela;
import pt.acv.adega.planeamento.PlaneamentoVinho;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Processo de Moagem e enchimento de talhas/cubas (Fase 3, ponto 3.1). Uma
 * moagem e feita por adega + vinho e mói uma ou mais vindimas (parcelas
 * colhidas) desse vinho entregues nessa adega. Ao fechar, gera as fichas de
 * mosto (uma por enchimento) e soma os volumes aos recipientes.
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

    /** Vinho a que se destina a uva moída (planeamento). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plano_id")
    private PlaneamentoVinho plano;

    /** Vindimas (parcelas colhidas) que esta moagem mói. Rastreabilidade uva->mosto. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "moagem_vindima",
            joinColumns = @JoinColumn(name = "moagem_id"),
            inverseJoinColumns = @JoinColumn(name = "linha_id"))
    private List<LinhaPlaneamentoParcela> vindimas = new ArrayList<>();

    /** Vinha de origem (opcional, redundante). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vinha_id")
    private Vinha vinha;

    @OneToMany(mappedBy = "moagem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Enchimento> enchimentos = new ArrayList<>();

    /**
     * Objetivo de Kg a moer, definido a mao. Historico: as moagens criadas a
     * partir da "sobra" de outra guardavam aqui um retrato dos Kg que faltavam
     * nesse instante. Esse retrato ficava desatualizado assim que outra moagem
     * mexia na mesma vindima, e fazia com que a mesma uva fosse contada duas
     * vezes. Hoje o que falta moer e' sempre calculado a partir das vindimas
     * (ver MoagemController.resumoDaMoagem), por isso este campo so' e' usado
     * como ultimo recurso, nas moagens que nao tenham vindima nenhuma associada.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal objetivoKgManual;

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public PlaneamentoVinho getPlano() { return plano; }
    public void setPlano(PlaneamentoVinho plano) { this.plano = plano; }

    public List<LinhaPlaneamentoParcela> getVindimas() { return vindimas; }
    public void setVindimas(List<LinhaPlaneamentoParcela> vindimas) { this.vindimas = vindimas; }

    public Vinha getVinha() { return vinha; }
    public void setVinha(Vinha vinha) { this.vinha = vinha; }

    public List<Enchimento> getEnchimentos() { return enchimentos; }
    public void setEnchimentos(List<Enchimento> enchimentos) { this.enchimentos = enchimentos; }

    public BigDecimal getObjetivoKgManual() { return objetivoKgManual; }
    public void setObjetivoKgManual(BigDecimal objetivoKgManual) { this.objetivoKgManual = objetivoKgManual; }

    /**
     * Total de uva colhida nas vindimas desta moagem. E' o total da colheita,
     * partilhado com as outras moagens que moam as mesmas vindimas — nao e' o
     * que falta moer. Para isso ver o resumo calculado no controlador.
     */
    @Transient
    public BigDecimal getTotalVindimadoKg() {
        if (vindimas.isEmpty() && objetivoKgManual != null) return objetivoKgManual;
        return vindimas.stream()
                .map(LinhaPlaneamentoParcela::getTotalVindimadoKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

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

    /**
     * Data em que a moagem foi feita. E' a data de inicio indicada pelo
     * utilizador; se ele nao a preencheu, vale o dia em que a moagem foi
     * criada, para o ecra nunca ficar sem data.
     */
    @Transient
    public LocalDate getDataDaMoagem() {
        if (getDataHoraInicio() != null) return getDataHoraInicio().toLocalDate();
        return getDataCriacao() != null ? getDataCriacao().toLocalDate() : null;
    }

    /** Dia em que a moagem foi fechada (null enquanto estiver aberta). */
    @Transient
    public LocalDate getDataDoFecho() {
        return getDataFecho() != null ? getDataFecho().toLocalDate() : null;
    }

    /** Castas das vindimas desta moagem, para as listagens e o histórico. */
    @Transient
    public String getCastasDescricao() {
        List<String> nomes = new ArrayList<>();
        for (LinhaPlaneamentoParcela l : vindimas) {
            if (l.getParcela() == null || l.getParcela().getCasta() == null) continue;
            String n = l.getParcela().getCasta().getNome();
            if (n != null && !nomes.contains(n)) nomes.add(n);
        }
        return nomes.isEmpty() ? "—" : String.join(", ", nomes);
    }

    /** Recipientes cheios nesta moagem ("Talha T1, Depósito D2"). */
    @Transient
    public String getRecipientesDescricao() {
        List<String> nomes = new ArrayList<>();
        for (Enchimento e : enchimentos) {
            String n = e.getRecipienteDescricao();
            if (n != null && !"—".equals(n) && !nomes.contains(n)) nomes.add(n);
        }
        return nomes.isEmpty() ? "—" : String.join(", ", nomes);
    }
}
