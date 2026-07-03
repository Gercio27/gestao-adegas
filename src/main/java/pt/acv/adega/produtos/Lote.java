package pt.acv.adega.produtos;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.common.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lote (Fase 4.6) - agrupamento de vinhos a granel para efeitos de gestao e
 * certificacao. Cada vinho a granel (mosto) pode pertencer a um lote.
 */
@Entity
@Table(name = "lote")
public class Lote extends BaseEntity {

    public static final String PREFIXO = "LOT";

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataConstituicao;

    /** Ids dos vinhos a granel a incluir no lote (selecao do formulario). */
    @Transient
    private List<Long> mostoIds = new ArrayList<>();

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public LocalDate getDataConstituicao() { return dataConstituicao; }
    public void setDataConstituicao(LocalDate dataConstituicao) { this.dataConstituicao = dataConstituicao; }

    public List<Long> getMostoIds() { return mostoIds; }
    public void setMostoIds(List<Long> mostoIds) { this.mostoIds = mostoIds; }
}
