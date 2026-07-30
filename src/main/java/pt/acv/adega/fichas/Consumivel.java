package pt.acv.adega.fichas;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.acv.adega.common.BaseEntity;

/**
 * Consumivel gerido com stock: garrafas, rolhas, rotulos, capsulas, caixas,
 * etiquetas. Permite alertas quando o stock desce abaixo do minimo.
 */
@Entity
@Table(name = "consumivel")
public class Consumivel extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConsumivel tipo = TipoConsumivel.GARRAFA;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String descricao;

    /** Capacidade em ml (relevante para garrafas: permite calcular nº de garrafas). */
    private Integer capacidadeMl;

    @Column(nullable = false)
    private int stock = 0;

    /** Stock minimo para gerar alerta (opcional). */
    private Integer stockMinimo;

    @Column(length = 20)
    private String unidade = "un";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Propriedade propriedade = Propriedade.PROPRIO;

    @Column(length = 120)
    private String terceiro;

    public TipoConsumivel getTipo() { return tipo; }
    public void setTipo(TipoConsumivel tipo) { this.tipo = tipo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getCapacidadeMl() { return capacidadeMl; }
    public void setCapacidadeMl(Integer capacidadeMl) { this.capacidadeMl = capacidadeMl; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    public Propriedade getPropriedade() { return propriedade; }
    public void setPropriedade(Propriedade propriedade) { this.propriedade = propriedade; }

    public String getTerceiro() { return terceiro; }
    public void setTerceiro(String terceiro) { this.terceiro = terceiro; }

    /** True quando ha stock minimo definido e o stock atual esta igual ou abaixo. */
    @Transient
    public boolean isAbaixoMinimo() {
        return stockMinimo != null && stock <= stockMinimo;
    }

    /** Nao ha nada em stock — nao da para usar em processo nenhum. */
    @Transient
    public boolean isSemStock() { return stock <= 0; }

    /** Chegou ao minimo mas ainda ha — e' so' aviso. */
    @Transient
    public boolean isNoMinimo() { return !isSemStock() && isAbaixoMinimo(); }

    /** "vermelho" (sem stock), "amarelo" (no minimo) ou "" (normal). */
    @Transient
    public String getEstadoStock() {
        if (isSemStock()) return "vermelho";
        if (isNoMinimo()) return "amarelo";
        return "";
    }

    /** Texto para os seletores dos processos: descricao + estado do stock. */
    @Transient
    public String getEtiquetaStock() {
        String base = codigoOuVazio() + descricao + " · stock " + stock;
        if (isSemStock()) return base + " — SEM STOCK";
        if (isNoMinimo()) return base + " — no mínimo";
        return base;
    }

    private String codigoOuVazio() {
        return getCodigo() != null ? getCodigo() + " · " : "";
    }
}
