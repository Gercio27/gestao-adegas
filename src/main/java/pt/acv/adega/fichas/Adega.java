package pt.acv.adega.fichas;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import pt.acv.adega.common.BaseEntity;

/**
 * Ficha 1.7 - Adega (local fisico onde estao talhas/depositos e se processa o vinho).
 */
@Entity
@Table(name = "adega")
public class Adega extends BaseEntity {

    public static final String PREFIXO = "ADG";

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 200)
    private String localizacao;

    @Column(length = 200)
    private String morada;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public String getMorada() { return morada; }
    public void setMorada(String morada) { this.morada = morada; }
}
