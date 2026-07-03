package pt.acv.adega.fichas;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import pt.acv.adega.common.BaseEntity;

import java.math.BigDecimal;

/**
 * Ficha 1.10 - Talha. Recipiente de fermentacao/armazenamento tradicional.
 * Tem capacidade (litros) e volume atual; a propriedade (proprio/terceiro) e
 * necessaria porque nao pode existir mais vinho+mosto do que a capacidade
 * dos recipientes registados.
 */
@Entity
@Table(name = "talha")
public class Talha extends BaseEntity {

    public static final String PREFIXO = "TLH";

    @NotBlank
    @Column(nullable = false, length = 120)
    private String identificacao;

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

    /** Uma talha esta "vazia" quando nao tem volume registado. */
    @Transient
    public boolean isVazia() {
        return volumeAtualLitros == null || volumeAtualLitros.signum() == 0;
    }
}
