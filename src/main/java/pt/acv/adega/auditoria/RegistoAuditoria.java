package pt.acv.adega.auditoria;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registo de auditoria: uma linha por alteracao feita na plataforma, com o
 * utilizador responsavel, a data/hora, a accao e o detalhe tecnico. Serve para
 * o administrador consultar quem fez o que e quando.
 */
@Entity
@Table(name = "registo_auditoria")
public class RegistoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    /** Username da conta que fez a alteracao. */
    @Column(length = 60)
    private String username;

    /** Nome do utilizador no momento da accao (para leitura facil). */
    @Column(length = 120)
    private String nomeUtilizador;

    /** Descricao legivel da accao (ex.: "Fechou · Análise à maturação (#5)"). */
    @Column(length = 200)
    private String descricao;

    /** Metodo HTTP (POST/PUT/DELETE/PATCH). */
    @Column(length = 10)
    private String metodo;

    /** Caminho do pedido (detalhe tecnico). */
    @Column(length = 300)
    private String caminho;

    /** Codigo de estado HTTP do resultado. */
    private int estado;

    @Column(length = 45)
    private String ip;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNomeUtilizador() { return nomeUtilizador; }
    public void setNomeUtilizador(String nomeUtilizador) { this.nomeUtilizador = nomeUtilizador; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public String getCaminho() { return caminho; }
    public void setCaminho(String caminho) { this.caminho = caminho; }

    public int getEstado() { return estado; }
    public void setEstado(int estado) { this.estado = estado; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    /** true quando o resultado foi bem-sucedido (2xx ou redirecionamento 3xx). */
    @Transient
    public boolean isSucesso() {
        return estado >= 200 && estado < 400;
    }
}
