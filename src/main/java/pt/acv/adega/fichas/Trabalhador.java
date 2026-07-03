package pt.acv.adega.fichas;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import pt.acv.adega.common.BaseEntity;

/**
 * Ficha 1.3 - Trabalhador da organizacao.
 */
@Entity
@Table(name = "trabalhador")
public class Trabalhador extends BaseEntity {

    public static final String PREFIXO = "TRB";

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 80)
    private String funcao;

    @Column(length = 60)
    private String contacto;

    @Column(length = 20)
    private String nif;

    @Column(nullable = false)
    private boolean ativo = true;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getFuncao() { return funcao; }
    public void setFuncao(String funcao) { this.funcao = funcao; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
