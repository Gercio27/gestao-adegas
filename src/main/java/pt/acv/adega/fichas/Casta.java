package pt.acv.adega.fichas;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import pt.acv.adega.common.BaseEntity;

/**
 * Ficha 1.1 - Casta de uva. Carregada a partir da lista nacional por ordem
 * alfabetica no primeiro arranque; o utilizador pode acrescentar castas.
 */
@Entity
@Table(name = "casta")
public class Casta extends BaseEntity {

    /** Prefixo do codigo automatico do sistema. */
    public static final String PREFIXO = "CAS";

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private CorCasta cor;

    @Column(length = 500)
    private String observacoes;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public CorCasta getCor() { return cor; }
    public void setCor(CorCasta cor) { this.cor = cor; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
