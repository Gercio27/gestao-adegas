package pt.acv.adega.common;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Base de todas as fichas: id interno, codigo automatico do sistema e datas
 * de criacao/atualizacao. O "codigo" e o identificador visivel ao utilizador
 * (ex.: CAS-000001) e nunca deve ser alterado manualmente.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, length = 20)
    private String codigo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    @PrePersist
    void aoCriar() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = this.dataCriacao;
    }

    @PreUpdate
    void aoAtualizar() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
}
