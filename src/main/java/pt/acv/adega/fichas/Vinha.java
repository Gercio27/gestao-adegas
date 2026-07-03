package pt.acv.adega.fichas;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import pt.acv.adega.common.BaseEntity;

import java.math.BigDecimal;
import java.util.*;

/**
 * Ficha 1.2 - Vinha. Composta por parcelas, cada uma com a sua casta e area.
 * Disponibiliza resumo dos totais por casta e area total.
 * Pode ser propria ou de terceiros (necessario para uvas de terceiros
 * vindimadas pela empresa - Fase 2.3).
 */
@Entity
@Table(name = "vinha")
public class Vinha extends BaseEntity {

    public static final String PREFIXO = "VIN";

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 200)
    private String localizacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Propriedade propriedade = Propriedade.PROPRIO;

    /** Nome do terceiro, obrigatorio quando a vinha e de terceiros. */
    @Column(length = 120)
    private String terceiro;

    @OneToMany(mappedBy = "vinha", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Parcela> parcelas = new ArrayList<>();

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public Propriedade getPropriedade() { return propriedade; }
    public void setPropriedade(Propriedade propriedade) { this.propriedade = propriedade; }

    public String getTerceiro() { return terceiro; }
    public void setTerceiro(String terceiro) { this.terceiro = terceiro; }

    public List<Parcela> getParcelas() { return parcelas; }
    public void setParcelas(List<Parcela> parcelas) { this.parcelas = parcelas; }

    public void addParcela(Parcela p) {
        p.setVinha(this);
        this.parcelas.add(p);
    }

    /** Area total da vinha (soma das parcelas), em hectares. */
    @Transient
    public BigDecimal getAreaTotal() {
        return parcelas.stream()
                .map(p -> p.getAreaHa() == null ? BigDecimal.ZERO : p.getAreaHa())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Resumo dos totais de area por casta. */
    @Transient
    public Map<String, BigDecimal> getResumoPorCasta() {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        for (Parcela p : parcelas) {
            String casta = p.getCasta() != null ? p.getCasta().getNome() : "(sem casta)";
            BigDecimal area = p.getAreaHa() == null ? BigDecimal.ZERO : p.getAreaHa();
            mapa.merge(casta, area, BigDecimal::add);
        }
        return mapa;
    }
}
