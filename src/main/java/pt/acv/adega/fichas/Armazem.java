package pt.acv.adega.fichas;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import pt.acv.adega.common.BaseEntity;

/**
 * Ficha de Armazem - local fisico onde ficam guardados os contentores de
 * garrafas (produto acabado / vinho engarrafado). Distinto da Adega, que e o
 * local onde estao as talhas/depositos e se processa o vinho.
 */
@Entity
@Table(name = "armazem")
public class Armazem extends BaseEntity {

    public static final String PREFIXO = "ARM";

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
