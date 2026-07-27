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

    /**
     * O deposito fica numa adega OU num armazem - nunca nos dois. A adega e o
     * sitio onde se processa o vinho; o armazem e so de armazenamento.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "armazem_id")
    private Armazem armazem;

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

    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }

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

    /** Identificacao do local (adega ou armazem) onde o deposito se encontra. */
    @Transient
    public String getLocalizacao() {
        if (adega != null) return "Adega " + adega.getNome();
        if (armazem != null) return "Armazém " + armazem.getNome();
        return "—";
    }

    /** Referencia do local para os seletores: "ADEGA:id" ou "ARMAZEM:id". */
    @Transient
    public String getLocalRef() {
        if (adega != null) return "ADEGA:" + adega.getId();
        if (armazem != null) return "ARMAZEM:" + armazem.getId();
        return "";
    }

    /** "ADEGA" ou "ARMAZEM" - usado pelo formulario da ficha. */
    @Transient
    public String getLocalTipo() {
        return armazem != null ? "ARMAZEM" : "ADEGA";
    }
}
