package pt.acv.adega.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Gera codigos automaticos do sistema, sequenciais por prefixo/tipo de ficha.
 * Formato: PREFIXO-000001. O bloqueio pessimista garante que nao ha codigos
 * repetidos mesmo com varios utilizadores em simultaneo.
 *
 * <p>Antes de dar um codigo, poe o contador a par dos que ja existem na base de
 * dados. Se o contador ficar atras — por uma reposicao, uma importacao ou uma
 * limpeza — voltaria a gerar um codigo repetido e a gravacao rebentava contra a
 * restricao de unicidade, deixando o utilizador sem conseguir criar fichas.
 * Preferimos saltar numeros a isso.
 */
@Service
public class CodigoService {

    private static final Logger log = LoggerFactory.getLogger(CodigoService.class);

    private final ContadorCodigoRepository repo;

    @PersistenceContext
    private EntityManager em;

    /**
     * Entidades que tem um campo "codigo". Sai do metamodelo, sem ir a base de
     * dados, e nao muda durante a execucao — por isso calcula-se uma vez so'.
     */
    private volatile List<String> entidadesComCodigo;

    public CodigoService(ContadorCodigoRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public String proximoCodigo(String prefixo) {
        ContadorCodigo contador = repo.findByPrefixo(prefixo)
                .orElseGet(() -> repo.save(new ContadorCodigo(prefixo)));
        // Verifica-se sempre, e nao so' a primeira vez: o contador pode ficar
        // atras a meio da vida da aplicacao (uma limpeza, uma importacao), e
        // nessa altura o utilizador ficaria sem conseguir criar fichas.
        porAPar(contador, prefixo);
        long valor = contador.proximo();
        repo.save(contador);
        return String.format("%s-%06d", prefixo, valor);
    }

    /** Adianta o contador ate ao maior numero ja usado neste prefixo. */
    private void porAPar(ContadorCodigo contador, String prefixo) {
        long maior = maiorJaUsado(prefixo);
        if (maior > contador.getUltimoValor()) {
            log.warn("Contador de códigos '{}' estava em {} mas já existe o {}-{} — posto a par.",
                    prefixo, contador.getUltimoValor(), prefixo, String.format("%06d", maior));
            contador.naoFicarAtrasDe(maior);
            repo.save(contador);
        }
    }

    /**
     * Maior numero ja usado neste prefixo, procurado em todas as entidades que
     * tenham um campo "codigo". E' generico de proposito: o problema nao e' so'
     * das castas, qualquer ficha ou processo pode ficar com o contador atrasado.
     */
    private long maiorJaUsado(String prefixo) {
        long maior = 0;
        for (String entidade : entidadesComCodigo()) {
            try {
                // Os codigos tem numero de largura fixa com zeros a esquerda, por
                // isso o maior alfabetico e' tambem o maior numero — um max() por
                // entidade chega, e usa o indice do codigo.
                String codigo = em.createQuery(
                                "select max(e.codigo) from " + entidade + " e where e.codigo like :p", String.class)
                        .setParameter("p", prefixo + "-%")
                        .getSingleResult();
                maior = Math.max(maior, numeroDe(codigo));
            } catch (Exception e) {
                // Entidade sem tabela nesta base de dados, ou codigo noutro
                // formato: nao ha nada a aprender daqui, segue para a seguinte.
                log.debug("Não foi possível ler códigos de {}: {}", entidade, e.getMessage());
            }
        }
        return maior;
    }

    /** Nomes das entidades com um campo "codigo" de texto. Só do metamodelo. */
    private List<String> entidadesComCodigo() {
        List<String> nomes = entidadesComCodigo;
        if (nomes == null) {
            nomes = new ArrayList<>();
            for (EntityType<?> tipo : em.getMetamodel().getEntities()) {
                boolean temCodigo = tipo.getAttributes().stream()
                        .anyMatch(a -> "codigo".equals(a.getName()) && a.getJavaType() == String.class);
                if (temCodigo) nomes.add(tipo.getName());
            }
            entidadesComCodigo = nomes;
        }
        return nomes;
    }

    /** "CAS-000073" -> 73; 0 se nao vier no formato esperado. */
    private long numeroDe(String codigo) {
        if (codigo == null) return 0;
        int traco = codigo.lastIndexOf('-');
        if (traco < 0 || traco == codigo.length() - 1) return 0;
        try {
            return Long.parseLong(codigo.substring(traco + 1).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
