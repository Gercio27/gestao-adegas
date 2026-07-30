package pt.acv.adega.produtos;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Armazem;

/**
 * Garrafas ja rotuladas e encaixotadas, por vinho e por local.
 *
 * Depois da rotulagem as garrafas saem do contentor e ficam em caixas, que
 * ficam na mesma adega/armazem. E' daqui que o comercial entrega - o contentor
 * ja nao as tem.
 */
@Entity
@Table(name = "stock_rotulado")
public class StockRotulado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vinho engarrafado a que este stock pertence. */
    @Column(name = "vinho_engarrafado_id", nullable = false)
    private Long vinhoEngarrafadoId;

    /** Nome legivel, para as listas nao terem de ir buscar o produto. */
    @Column(length = 160)
    private String vinhoNome;

    // Onde estao as caixas: numa adega OU num armazem.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adega_id")
    private Adega adega;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "armazem_id")
    private Armazem armazem;

    /** Garrafas rotuladas que estao aqui (ainda por entregar). */
    @Column(nullable = false)
    private int garrafas;

    /** Caixas correspondentes. */
    @Column(nullable = false)
    private int caixas;

    @Column(nullable = false)
    private int garrafasPorCaixa = 6;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVinhoEngarrafadoId() { return vinhoEngarrafadoId; }
    public void setVinhoEngarrafadoId(Long vinhoEngarrafadoId) { this.vinhoEngarrafadoId = vinhoEngarrafadoId; }

    public String getVinhoNome() { return vinhoNome; }
    public void setVinhoNome(String vinhoNome) { this.vinhoNome = vinhoNome; }

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }

    public int getGarrafas() { return garrafas; }
    public void setGarrafas(int garrafas) { this.garrafas = garrafas; }

    public int getCaixas() { return caixas; }
    public void setCaixas(int caixas) { this.caixas = caixas; }

    public int getGarrafasPorCaixa() { return garrafasPorCaixa; }
    public void setGarrafasPorCaixa(int garrafasPorCaixa) { this.garrafasPorCaixa = garrafasPorCaixa; }

    @Transient
    public String getLocalRef() {
        if (adega != null) return "ADEGA:" + adega.getId();
        if (armazem != null) return "ARMAZEM:" + armazem.getId();
        return "";
    }

    @Transient
    public String getLocalNome() {
        if (adega != null) return "Adega " + adega.getNome();
        if (armazem != null) return "Armazém " + armazem.getNome();
        return "—";
    }

    /** Caixas cheias que ainda ca' estao (as garrafas podem nao dar caixa certa). */
    @Transient
    public int getCaixasInteiras() {
        return garrafasPorCaixa > 0 ? garrafas / garrafasPorCaixa : 0;
    }

    @Transient
    public String getDescricao() {
        return (vinhoNome != null ? vinhoNome : "vinho") + " · " + getLocalNome()
                + " · " + garrafas + " garrafas (" + getCaixasInteiras() + " caixas)";
    }
}
