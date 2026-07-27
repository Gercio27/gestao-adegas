package pt.acv.adega.fichas;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import pt.acv.adega.common.BaseEntity;

import java.math.BigDecimal;

/**
 * Ficha de Contentor / Palete de bag-in-box - recipiente fisico onde ficam as
 * unidades de bag-in-box de um vinho ja embalado. E' o equivalente ao Contentor
 * de Garrafas, mas para bag-in-box: em vez de garrafas conta unidades, e cada
 * unidade leva os litros do seu formato (3, 5, 10 ou 20 L).
 *
 * Guarda a ocupacao atual: quantas unidades tem, de que vinho (por id + nome
 * legivel) e se ja foram rotuladas. Fica num armazem ou numa adega.
 */
@Entity
@Table(name = "contentor_bag_in_box")
public class ContentorBagInBox extends BaseEntity {

    public static final String PREFIXO = "BIB";

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoBagInBox tipoBag = TipoBagInBox.BIB_5;

    /** Capacidade em numero de unidades (bags) do contentor/palete. */
    @Column(nullable = false)
    private int capacidadeUnidades;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "armazem_id")
    private Armazem armazem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    /** Unidades atualmente no contentor/palete. */
    @Column(nullable = false)
    private int unidadesAtuais = 0;

    /** Id do vinho embalado que ocupa o contentor, se algum. */
    @Column(name = "vinho_embalado_id")
    private Long vinhoEmbaladoId;

    /** Nome legivel do vinho que ocupa o contentor. */
    @Column(length = 160)
    private String vinhoNome;

    /** As unidades neste contentor ja foram rotuladas. */
    @Column(nullable = false)
    private boolean rotulado = false;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public TipoBagInBox getTipoBag() { return tipoBag; }
    public void setTipoBag(TipoBagInBox tipoBag) { this.tipoBag = tipoBag; }

    public int getCapacidadeUnidades() { return capacidadeUnidades; }
    public void setCapacidadeUnidades(int capacidadeUnidades) { this.capacidadeUnidades = capacidadeUnidades; }

    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public int getUnidadesAtuais() { return unidadesAtuais; }
    public void setUnidadesAtuais(int unidadesAtuais) { this.unidadesAtuais = unidadesAtuais; }

    public Long getVinhoEmbaladoId() { return vinhoEmbaladoId; }
    public void setVinhoEmbaladoId(Long vinhoEmbaladoId) { this.vinhoEmbaladoId = vinhoEmbaladoId; }

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
    public boolean isVazio() { return unidadesAtuais <= 0; }

    /** Unidades que ainda cabem (0 se ja esta cheio ou acima). */
    @Transient
    public int getEspacoLivre() { return Math.max(0, capacidadeUnidades - unidadesAtuais); }

    @Transient
    public boolean isAcimaDaCapacidade() {
        return capacidadeUnidades > 0 && unidadesAtuais > capacidadeUnidades;
    }

    @Transient
    public int getExcedente() { return Math.max(0, unidadesAtuais - capacidadeUnidades); }

    /** Litros de vinho que estao neste contentor (unidades x litros do formato). */
    @Transient
    public BigDecimal getLitrosAtuais() {
        if (tipoBag == null) return BigDecimal.ZERO;
        return tipoBag.getLitrosPorUnidade().multiply(BigDecimal.valueOf(unidadesAtuais));
    }

    /** Litros que o contentor leva quando estiver cheio. */
    @Transient
    public BigDecimal getLitrosCapacidade() {
        if (tipoBag == null) return BigDecimal.ZERO;
        return tipoBag.getLitrosPorUnidade().multiply(BigDecimal.valueOf(capacidadeUnidades));
    }
}
