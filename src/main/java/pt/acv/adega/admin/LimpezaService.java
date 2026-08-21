package pt.acv.adega.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Limpeza de dados de trabalho. O utilizador escolhe o que quer apagar — o
 * planeamento, as vindimas, as moagens, e por ai fora.
 *
 * <p>Os grupos {@link GrupoLimpeza#encadeado() encadeados} estao pela ordem do
 * processo produtivo, e essa ordem manda: <b>apagar um obriga a apagar todos os
 * que vem depois</b>, porque o que vem depois nasceu do que vem antes. Apagar as
 * vindimas e deixar as moagens deixaria moagens a apontar para uva que ja nao
 * existe. Os grupos nao encadeados (analises, historico de stock) nao tem nada
 * que dependa deles e podem ser apagados sozinhos.
 *
 * <p>NAO toca nas FICHAS: adegas, armazens, talhas, depositos, contentores,
 * castas, vinhas, parcelas, trabalhadores, consumiveis e utilizadores ficam
 * todos como estao.
 */
@Service
public class LimpezaService {

    @PersistenceContext
    private EntityManager em;

    /**
     * Os grupos, pela ordem do processo. As tabelas de cada um vao com as
     * filhas primeiro; a execucao percorre os grupos do ultimo para o primeiro,
     * para nunca esbarrar numa chave estrangeira.
     */
    private static final List<GrupoLimpeza> GRUPOS = List.of(
            new GrupoLimpeza("planeamento", "Planeamento de vinho e vindimas",
                    "Os vinhos planeados, as parcelas de cada um e todas as colheitas registadas.",
                    List.of("processo_vindima", "registo_vindima", "planeamento_linha_parcela", "planeamento_vinho"),
                    List.of("PLN", "VDM"), true),
            new GrupoLimpeza("moagem", "Moagens",
                    "As moagens e os enchimentos de cada talha/depósito.",
                    List.of("enchimento_casta", "enchimento_vindima", "enchimento", "moagem_vindima", "processo_moagem"),
                    List.of("MOA"), true),
            new GrupoLimpeza("mostos", "Mostos e vinhos a granel",
                    "As fichas de mosto geradas pelas moagens. Talhas e depósitos ficam vazios.",
                    List.of("mosto_casta", "mosto"),
                    List.of("MST"), true),
            new GrupoLimpeza("fermentacao", "Fermentação",
                    "Remontagens, atestos, movimentos de mosto e passagens a limpo.",
                    List.of("remontagem_talha", "processo_remontagem", "processo_atesto",
                            "processo_movimento_mosto", "passagem_item", "processo_passagem_vinho"),
                    List.of("REM", "ATE", "MOV", "PVG"), true),
            new GrupoLimpeza("granel", "Movimentos de vinho a granel",
                    "As saídas e entradas de vinho a granel.",
                    List.of("processo_movimento_vinho"),
                    List.of("MVG"), true),
            new GrupoLimpeza("loteamento", "Loteamentos e engarrafamentos",
                    "Os lotes construídos e os engarrafamentos.",
                    List.of("lote_linha", "lote_construcao", "lote", "loteamento", "processo_engarrafamento"),
                    List.of("LOT", "LTV", "ENG"), true),
            new GrupoLimpeza("engarrafados", "Vinhos engarrafados",
                    "As fichas de vinho engarrafado e o stock rotulado. Contentores e paletes ficam vazios.",
                    List.of("stock_rotulado", "vinho_engarrafado"),
                    List.of("VEG"), true),
            new GrupoLimpeza("finais", "Certificações, rotulagens, comercial e saídas",
                    "O que se faz depois de engarrafar: certificar, rotular, entregar e dar saída.",
                    List.of("processo_certificacao", "processo_rotulagem", "processo_comercial", "saida_contentor"),
                    List.of("CER", "RTL", "PCO", "SCT"), true),
            new GrupoLimpeza("analises", "Análises e tratamentos",
                    "Análises à maturação, análises ao vinho e tratamentos enológicos. Não depende de nada nem nada depende deles.",
                    List.of("analise_vinho", "tratamento_enologico", "processo_analise_maturacao"),
                    List.of("ANL", "TRT", "AMT"), false),
            new GrupoLimpeza("movimentos", "Histórico de movimentos de stock",
                    "O registo de entradas e saídas das talhas, depósitos e armazéns. É só o histórico — não altera volumes.",
                    List.of("movimento_stock"),
                    List.of(), false)
    );

    /** Recipientes a esvaziar quando o produto que la' esta e' apagado. */
    private static final List<String> ESVAZIAR_GRANEL = List.of(
            "UPDATE talha SET volume_atual_litros = 0",
            "UPDATE deposito SET volume_atual_litros = 0"
    );

    private static final List<String> ESVAZIAR_ENGARRAFADO = List.of(
            "UPDATE contentor_garrafas SET garrafas_atuais = 0, vinho_engarrafado_id = NULL,"
                    + " vinho_nome = NULL, rotulado = FALSE",
            "UPDATE contentor_bag_in_box SET unidades_atuais = 0, vinho_embalado_id = NULL,"
                    + " vinho_nome = NULL, rotulado = FALSE"
    );

    public List<GrupoLimpeza> grupos() {
        return GRUPOS;
    }

    public GrupoLimpeza grupo(String id) {
        return GRUPOS.stream().filter(g -> g.id().equals(id)).findFirst().orElse(null);
    }

    /** Quantos registos existem hoje em cada grupo, para o utilizador ver antes de decidir. */
    public Map<String, Long> contagensPorGrupo() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (GrupoLimpeza g : GRUPOS) {
            long total = 0;
            for (String tabela : g.tabelas()) total += contar(tabela);
            m.put(g.id(), total);
        }
        return m;
    }

    /** Total de registos em tudo o que a pagina consegue apagar. */
    public long contarProcessos() {
        return contagensPorGrupo().values().stream().mapToLong(Long::longValue).sum();
    }

    public long contar(String tabela) {
        try {
            Number n = (Number) em.createNativeQuery("SELECT COUNT(*) FROM " + tabela).getSingleResult();
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            return 0;   // tabela ainda não existe nesta base de dados
        }
    }

    /**
     * Os grupos encadeados que faltam a uma escolha para ela ser segura, ou seja
     * os que vem depois do primeiro escolhido e nao foram marcados. Vazio quando
     * a escolha ja' esta completa.
     */
    public List<GrupoLimpeza> emFalta(Set<String> escolhidos) {
        List<GrupoLimpeza> falta = new ArrayList<>();
        boolean comecou = false;
        for (GrupoLimpeza g : GRUPOS) {
            if (!g.encadeado()) continue;
            if (escolhidos.contains(g.id())) { comecou = true; continue; }
            if (comecou) falta.add(g);
        }
        return falta;
    }

    /**
     * Apaga os grupos escolhidos. Tudo numa transacao: ou corre tudo, ou nao
     * fica nada a meio. Os grupos sao percorridos do ultimo para o primeiro,
     * que e' a ordem que respeita as chaves estrangeiras.
     */
    @Transactional
    public long limpar(Set<String> escolhidos) {
        long apagados = 0;
        List<GrupoLimpeza> ordem = new ArrayList<>(GRUPOS);
        java.util.Collections.reverse(ordem);

        for (GrupoLimpeza g : ordem) {
            if (!escolhidos.contains(g.id())) continue;
            for (String tabela : g.tabelas()) {
                try {
                    apagados += em.createNativeQuery("DELETE FROM " + tabela).executeUpdate();
                } catch (Exception e) {
                    // Tabela inexistente nesta versão da base de dados — segue.
                }
            }
            // A numeracao destes codigos recomeca no 1. So' a dos grupos
            // apagados: os contadores das fichas e dos processos que ficam tem
            // de continuar de onde estavam, senao sairiam codigos repetidos.
            for (String prefixo : g.prefixos()) {
                try {
                    em.createNativeQuery("DELETE FROM contador_codigo WHERE prefixo = ?1")
                            .setParameter(1, prefixo).executeUpdate();
                } catch (Exception ignored) { }
            }
        }

        // Os recipientes so' se esvaziam quando o que estava la' dentro foi apagado.
        if (escolhidos.contains("mostos")) executar(ESVAZIAR_GRANEL);
        if (escolhidos.contains("engarrafados")) executar(ESVAZIAR_ENGARRAFADO);
        return apagados;
    }

    private void executar(List<String> comandos) {
        for (String sql : comandos) {
            try { em.createNativeQuery(sql).executeUpdate(); } catch (Exception ignored) { }
        }
    }
}
