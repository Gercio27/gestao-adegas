package pt.acv.adega.processos.engarrafamento;

import jakarta.persistence.*;
import pt.acv.adega.fichas.Consumivel;
import pt.acv.adega.fichas.TipoBagInBox;
import pt.acv.adega.fichas.TipoEmbalagem;
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

    /**
     * Garrafas ou bag-in-box. Decide para onde vao as unidades no fim e como se
     * calculam os litros: com bag-in-box, litros = unidades x litros do formato.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_embalagem", nullable = false, length = 12)
    private TipoEmbalagem tipoEmbalagem = TipoEmbalagem.GARRAFA;

    /** Formato do bag-in-box (3, 5, 10 ou 20 L). So' usado quando e' bag-in-box. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_bag", length = 12)
    private TipoBagInBox tipoBag;

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

    /** Distribuicao das garrafas por contentor: "contentorId:qtd;contentorId:qtd". */
    @Column(length = 1000)
    private String distribuicaoContentores;

    /** Descricao legivel dos contentores usados (nome + garrafas). */
    @Column(length = 1000)
    private String contentoresDescricao;

    public String getDistribuicaoContentores() { return distribuicaoContentores; }
    public void setDistribuicaoContentores(String distribuicaoContentores) { this.distribuicaoContentores = distribuicaoContentores; }

    public String getContentoresDescricao() { return contentoresDescricao; }
    public void setContentoresDescricao(String contentoresDescricao) { this.contentoresDescricao = contentoresDescricao; }

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

    public TipoEmbalagem getTipoEmbalagem() { return tipoEmbalagem; }
    public void setTipoEmbalagem(TipoEmbalagem tipoEmbalagem) { this.tipoEmbalagem = tipoEmbalagem; }

    public TipoBagInBox getTipoBag() { return tipoBag; }
    public void setTipoBag(TipoBagInBox tipoBag) { this.tipoBag = tipoBag; }

    @Transient
    public boolean isBagInBox() { return tipoEmbalagem == TipoEmbalagem.BAG_IN_BOX; }

    /** "garrafa(s)" ou "unidade(s)", para os textos dos ecrãs. */
    @Transient
    public String getUnidadeNome() { return isBagInBox() ? "unidade(s)" : "garrafa(s)"; }

    /** Litros que estas unidades representam (só faz sentido no bag-in-box). */
    @Transient
    public BigDecimal getLitrosDasUnidades() {
        if (!isBagInBox() || tipoBag == null) return BigDecimal.ZERO;
        return tipoBag.getLitrosPorUnidade().multiply(BigDecimal.valueOf(numeroGarrafas));
    }
}
