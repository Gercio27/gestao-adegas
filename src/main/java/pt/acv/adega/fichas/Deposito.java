package pt.acv.adega.fichas;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import pt.acv.adega.common.BaseEntity;

import java.math.BigDecimal;

/**
 * Ficha 1.11 / 1.15 - Cuba ou Deposito (inox, etc.) para fermentacao,
 * armazenamento ou passagem a limpo do vinho. Mesma logica de capacidade e
 * propriedade das talhas.
 */
@Entity
@Table(name = "deposito")
public class Deposito extends BaseEntity {

    public static final String PREFIXO = "DEP";

    @NotBlank
    @Column(nullable = false, length = 120)
    private String identificacao;

    @Column(length = 40)
    private String tipo; // ex.: Cuba inox, Deposito, Cuba fermentacao

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    @Column(precision = 12, scale = 2)
    private BigDecimal capacidadeLitros;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal volumeAtualLitros = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Propriedade propriedade = Propriedade.PROPRIO;

    @Column(length = 120)
    private String terceiro;

    public String getIdentificacao() { return identificacao; }
    public void setIdentificacao(String identificacao) { this.identificacao = identificacao; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public BigDecimal getCapacidadeLitros() { return capacidadeLitros; }
    public void setCapacidadeLitros(BigDecimal capacidadeLitros) { this.capacidadeLitros = capacidadeLitros; }

    public BigDecimal getVolumeAtualLitros() { return volumeAtualLitros; }
    public void setVolumeAtualLitros(BigDecimal volumeAtualLitros) { this.volumeAtualLitros = volumeAtualLitros; }

    public Propriedade getPropriedade() { return propriedade; }
    public void setPropriedade(Propriedade propriedade) { this.propriedade = propriedade; }

    public String getTerceiro() { return terceiro; }
    public void setTerceiro(String terceiro) { this.terceiro = terceiro; }

    @Transient
    public boolean isVazia() {
        return volumeAtualLitros == null || volumeAtualLitros.signum() == 0;
    }
}
