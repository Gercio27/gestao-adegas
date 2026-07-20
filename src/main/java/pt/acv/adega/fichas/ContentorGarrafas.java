package pt.acv.adega.fichas;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import pt.acv.adega.common.BaseEntity;

/**
 * Ficha de Contentor de Garrafas - recipiente fisico onde ficam as garrafas do
 * vinho engarrafado (produto acabado). Tem uma capacidade em garrafas de um
 * formato (borgonhesa/bordalesa) e fica num armazem ou numa adega.
 *
 * Guarda tambem a ocupacao atual: quantas garrafas tem, de que vinho engarrafado
 * (por id + nome legivel) e se essas garrafas ja foram rotuladas (Fase 7). Isto
 * permite: (a) registar stock inicial de uma adega a meio; (b) o engarrafamento
 * colocar garrafas por contentor; (c) o comercial ver as garrafas disponiveis
 * por vinho em cada armazem/adega.
 */
@Entity
@Table(name = "contentor_garrafas")
public class ContentorGarrafas extends BaseEntity {

    public static final String PREFIXO = "CTG";

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoGarrafa tipoGarrafa = TipoGarrafa.BORDALESA;

    /** Capacidade em numero de garrafas. */
    @Column(nullable = false)
    private int capacidadeGarrafas;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "armazem_id")
    private Armazem armazem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    /** Garrafas atualmente dentro do contentor. */
    @Column(nullable = false)
    private int garrafasAtuais = 0;

    /** Id do vinho engarrafado que ocupa o contentor (produto 2.5), se algum. */
    @Column(name = "vinho_engarrafado_id")
    private Long vinhoEngarrafadoId;

    /** Nome legivel do vinho que ocupa o contentor. */
    @Column(length = 160)
    private String vinhoNome;

    /** As garrafas neste contentor ja foram rotuladas (Fase 7). */
    @Column(nullable = false)
    private boolean rotulado = false;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public TipoGarrafa getTipoGarrafa() { return tipoGarrafa; }
    public void setTipoGarrafa(TipoGarrafa tipoGarrafa) { this.tipoGarrafa = tipoGarrafa; }

    public int getCapacidadeGarrafas() { return capacidadeGarrafas; }
    public void setCapacidadeGarrafas(int capacidadeGarrafas) { this.capacidadeGarrafas = capacidadeGarrafas; }

    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public int getGarrafasAtuais() { return garrafasAtuais; }
    public void setGarrafasAtuais(int garrafasAtuais) { this.garrafasAtuais = garrafasAtuais; }

    public Long getVinhoEngarrafadoId() { return vinhoEngarrafadoId; }
    public void setVinhoEngarrafadoId(Long vinhoEngarrafadoId) { this.vinhoEngarrafadoId = vinhoEngarrafadoId; }

    public String getVinhoNome() { return vinhoNome; }
    public void setVinhoNome(String vinhoNome) { this.vinhoNome = vinhoNome; }

    public boolean isRotulado() { return rotulado; }
    public void setRotulado(boolean rotulado) { this.rotulado = rotulado; }

    /** Identificacao do local (armazem ou adega) onde o contentor se encontra. */
    @Transient
    public String getLocalizacao() {
        if (armazem != null) return "Armazém " + armazem.getNome();
        if (adega != null) return "Adega " + adega.getNome();
        return "—";
    }

    @Transient
    public boolean isVazio() { return garrafasAtuais <= 0; }

    /** Espaco livre (garrafas) ainda disponivel no contentor. */
    @Transient
    public int getEspacoLivre() {
        int livre = capacidadeGarrafas - garrafasAtuais;
        return Math.max(livre, 0);
    }

    /** Máximo fixo do formato (borgonhesa/bordalesa) deste contentor. */
    @Transient
    public int getMaximoFormato() {
        return tipoGarrafa != null ? tipoGarrafa.getMaximoGarrafas() : 0;
    }

    /** Quantas garrafas faltam para chegar ao máximo (0 se já está no máximo ou acima). */
    @Transient
    public int getFaltamParaMaximo() {
        return Math.max(0, capacidadeGarrafas - garrafasAtuais);
    }

    /** True quando o contentor tem mais garrafas do que o seu máximo (permitido, mas avisado). */
    @Transient
    public boolean isAcimaDoMaximo() {
        return capacidadeGarrafas > 0 && garrafasAtuais > capacidadeGarrafas;
    }

    /** Garrafas a mais para além do máximo (0 se dentro do máximo). */
    @Transient
    public int getExcedente() {
        return Math.max(0, garrafasAtuais - capacidadeGarrafas);
    }
}
