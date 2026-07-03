package pt.acv.adega.security;

import jakarta.validation.constraints.NotBlank;

/**
 * Formulario de utilizador. Separa a palavra-passe em texto simples (opcional
 * na edicao) da password cifrada guardada na entidade.
 */
public class UtilizadorForm {

    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    private String nome;

    private Perfil perfil = Perfil.OPERADOR;

    private boolean ativo = true;

    /** Em texto simples; obrigatoria ao criar, opcional ao editar. */
    private String password;

    public UtilizadorForm() { }

    public static UtilizadorForm de(Utilizador u) {
        UtilizadorForm f = new UtilizadorForm();
        f.id = u.getId();
        f.username = u.getUsername();
        f.nome = u.getNome();
        f.perfil = u.getPerfil();
        f.ativo = u.isAtivo();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
