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

    /** Altura da talha (cm). Medida fisica, para a caracterizar na ficha. */
    @Column(name = "altura_cm", precision = 8, scale = 2)
    private BigDecimal alturaCm;

    /** Diametro da boca da talha (cm). */
    @Column(name = "diametro_boca_cm", precision = 8, scale = 2)
    private BigDecimal diametroBocaCm;

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

    public BigDecimal getAlturaCm() { return alturaCm; }
    public void setAlturaCm(BigDecimal alturaCm) { this.alturaCm = alturaCm; }

    public BigDecimal getDiametroBocaCm() { return diametroBocaCm; }
    public void setDiametroBocaCm(BigDecimal diametroBocaCm) { this.diametroBocaCm = diametroBocaCm; }

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

    /** Medidas juntas para mostrar de relance: "Alt. 180 cm · boca 70 cm". */
    @Transient
    public String getMedidasDescricao() {
        StringBuilder sb = new StringBuilder();
        if (alturaCm != null) sb.append("Alt. ").append(alturaCm.toPlainString()).append(" cm");
        if (diametroBocaCm != null) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("boca ").append(diametroBocaCm.toPlainString()).append(" cm");
        }
        return sb.length() == 0 ? "—" : sb.toString();
    }

    /** Litros que ainda cabem (capacidade - volume atual). Vazio se nao houver capacidade definida. */
    @Transient
    public java.math.BigDecimal getDisponivelLitros() {
        if (capacidadeLitros == null) return null;
        java.math.BigDecimal v = volumeAtualLitros == null ? java.math.BigDecimal.ZERO : volumeAtualLitros;
        java.math.BigDecimal livre = capacidadeLitros.subtract(v);
        return livre.signum() < 0 ? java.math.BigDecimal.ZERO : livre;
    }
}
