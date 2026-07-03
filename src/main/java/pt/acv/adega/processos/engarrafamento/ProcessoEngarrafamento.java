package pt.acv.adega.processos.engarrafamento;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Consumivel;
import pt.acv.adega.processos.Fase;
import pt.acv.adega.processos.Processo;
import pt.acv.adega.produtos.Mosto;

import java.math.BigDecimal;

/**
 * Processo de Engarrafamento/enrolhamento (Fase 6, ponto 6.3). Usa vinho pronto
 * a granel e, ao fechar, da baixa do vinho (litros), das garrafas e das rolhas,
 * criando a ficha de vinho engarrafado.
 */
@Entity
@Table(name = "processo_engarrafamento")
public class ProcessoEngarrafamento extends Processo {

    public static final String PREFIXO = "ENG";
    public static final Fase FASE = Fase.FASE_6;

    /** Nome dado ao vinho a comercializar. */
    @Column(length = 160)
    private String nomeVinho;

    /** Vinho pronto a granel utilizado (ficha de mosto no estado VINHO_GRANEL). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vinho_granel_id")
    private Mosto vinhoGranel;

    @Column(precision = 12, scale = 2)
    private BigDecimal litrosUtilizados;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "garrafa_id")
    private Consumivel garrafa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rolha_id")
    private Consumivel rolha;

    @Column(nullable = false)
    private int numeroGarrafas;

    @Column(nullable = false)
    private int numeroRolhas;

    @Column(length = 60)
    private String lote;

    public String getNomeVinho() { return nomeVinho; }
    public void setNomeVinho(String nomeVinho) { this.nomeVinho = nomeVinho; }

    public Mosto getVinhoGranel() { return vinhoGranel; }
    public void setVinhoGranel(Mosto vinhoGranel) { this.vinhoGranel = vinhoGranel; }

    public BigDecimal getLitrosUtilizados() { return litrosUtilizados; }
    public void setLitrosUtilizados(BigDecimal litrosUtilizados) { this.litrosUtilizados = litrosUtilizados; }

    public Consumivel getGarrafa() { return garrafa; }
    public void setGarrafa(Consumivel garrafa) { this.garrafa = garrafa; }

    public Consumivel getRolha() { return rolha; }
    public void setRolha(Consumivel rolha) { this.rolha = rolha; }

    public int getNumeroGarrafas() { return numeroGarrafas; }
    public void setNumeroGarrafas(int numeroGarrafas) { this.numeroGarrafas = numeroGarrafas; }

    public int getNumeroRolhas() { return numeroRolhas; }
    public void setNumeroRolhas(int numeroRolhas) { this.numeroRolhas = numeroRolhas; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }
}
