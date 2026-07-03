package pt.acv.adega.security;

/**
 * Perfis de acesso. ADMIN ve tudo; OPERADOR ve os processos que abriu.
 */
public enum Perfil {
    ADMIN("Administrador"),
    OPERADOR("Operador");

    private final String descricao;

    Perfil(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }

    /** Nome da "role" usada pelo Spring Security (prefixo ROLE_). */
    public String authority() { return "ROLE_" + name(); }
}
