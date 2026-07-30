package pt.acv.adega.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Limpeza dos processos para voltar a testar a aplicacao do zero.
 *
 * Apaga tudo o que e' MOVIMENTO (planeamento, vindima, moagem, fermentacao,
 * certificacao, loteamento, engarrafamento, rotulagem, comercial, saidas,
 * tratamentos e analises) e os PRODUTOS que dai resultaram (mostos e vinhos
 * engarrafados), e poe os recipientes e contentores vazios.
 *
 * NAO toca nas FICHAS: adegas, armazens, talhas, depositos, contentores,
 * castas, vinhas, parcelas, trabalhadores, consumiveis e utilizadores ficam
 * todos como estao.
 */
@Service
public class LimpezaService {

    @PersistenceContext
    private EntityManager em;

    /**
     * Ordem: primeiro as tabelas-filhas e as de ligacao, so' depois as
     * tabelas-mae, para nao esbarrar nas chaves estrangeiras.
     */
    private static final List<String> TABELAS_A_APAGAR = List.of(
            // Fase 3 — moagem e enchimentos
            "enchimento_casta",
            "enchimento_vindima",
            "enchimento",
            "moagem_vindima",
            "processo_moagem",
            // Fase 4 — fermentacao
            "remontagem_talha",
            "processo_remontagem",
            "processo_atesto",
            "processo_movimento_mosto",
            "passagem_item",
            "processo_passagem_vinho",
            // Fase 5 — movimentos de vinho a granel e certificacao
            "processo_movimento_vinho",
            "processo_certificacao",
            // Fase 6 — loteamento e engarrafamento
            "lote_linha",
            "lote_construcao",
            "lote",
            "loteamento",
            "processo_engarrafamento",
            // Fases 7 e 8 — rotulagem, comercial e saidas
            "processo_rotulagem",
            "processo_comercial",
            "saida_contentor",
            // Tratamentos, analises e maturacao
            "analise_vinho",
            "tratamento_enologico",
            "processo_analise_maturacao",
            // Produtos gerados pelos processos
            "mosto_casta",
            "mosto",
            "vinho_engarrafado",
            "stock_rotulado",
            // Fases 1 e 2 — planeamento e vindima
            "registo_vindima",
            "processo_vindima",
            "planeamento_linha_parcela",
            "planeamento_vinho"
    );

    /**
     * Prefixos dos codigos das FICHAS. Os contadores destes NAO podem ser
     * reiniciados: as fichas ficam e os seus codigos ja existem, pelo que
     * recomecar do 1 daria codigos repetidos.
     */
    private static final String PREFIXOS_DAS_FICHAS =
            "'ADG','TLH','DEP','ARM','CTG','BIB','CAS','VIN','TRB'";

    /** Recipientes e contentores ficam vazios e sem vinho associado. */
    private static final List<String> ESVAZIAR = List.of(
            "UPDATE talha SET volume_atual_litros = 0",
            "UPDATE deposito SET volume_atual_litros = 0",
            "UPDATE contentor_garrafas SET garrafas_atuais = 0, vinho_engarrafado_id = NULL,"
                    + " vinho_nome = NULL, rotulado = FALSE",
            "UPDATE contentor_bag_in_box SET unidades_atuais = 0, vinho_embalado_id = NULL,"
                    + " vinho_nome = NULL, rotulado = FALSE"
    );

    /** Quantos registos existem hoje em cada tabela que vai ser apagada. */
    public long contarProcessos() {
        long total = 0;
        for (String tabela : TABELAS_A_APAGAR) {
            if ("contador_codigo".equals(tabela)) continue;   // não é dado de negócio
            total += contar(tabela);
        }
        return total;
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
     * Apaga os processos e esvazia os recipientes. Tudo numa transacao: ou
     * corre tudo, ou nao fica nada a meio.
     */
    @Transactional
    public long limparProcessos() {
        long apagados = 0;
        for (String tabela : TABELAS_A_APAGAR) {
            try {
                apagados += em.createNativeQuery("DELETE FROM " + tabela).executeUpdate();
            } catch (Exception e) {
                // Tabela inexistente nesta versão da base de dados — segue.
            }
        }
        // Numeracao dos processos recomeca no 1; a das fichas continua de onde
        // estava, senao os codigos novos chocavam com os das fichas que ficam.
        try {
            em.createNativeQuery("DELETE FROM contador_codigo WHERE prefixo NOT IN (" + PREFIXOS_DAS_FICHAS + ")")
                    .executeUpdate();
        } catch (Exception ignored) { }

        for (String sql : ESVAZIAR) {
            try { em.createNativeQuery(sql).executeUpdate(); } catch (Exception ignored) { }
        }
        return apagados;
    }
}
