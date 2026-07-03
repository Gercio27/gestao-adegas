package pt.acv.adega.common;

import jakarta.persistence.*;

/**
 * Contador por prefixo/tipo de ficha, usado para gerar o codigo automatico
 * sequencial (ex.: prefixo "CAS" -> CAS-000001, CAS-000002, ...).
 */
@Entity
@Table(name = "contador_codigo")
public class ContadorCodigo {

    @Id
    @Column(length = 10)
    private String prefixo;

    @Column(nullable = false)
    private long ultimoValor;

    protected ContadorCodigo() { }

    public ContadorCodigo(String prefixo) {
        this.prefixo = prefixo;
        this.ultimoValor = 0;
    }

    public String getPrefixo() { return prefixo; }

    public long getUltimoValor() { return ultimoValor; }

    public long proximo() {
        return ++this.ultimoValor;
    }
}
