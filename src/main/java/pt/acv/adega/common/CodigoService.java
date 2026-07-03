package pt.acv.adega.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera codigos automaticos do sistema, sequenciais por prefixo/tipo de ficha.
 * Formato: PREFIXO-000001. O bloqueio pessimista garante que nao ha codigos
 * repetidos mesmo com varios utilizadores em simultaneo.
 */
@Service
public class CodigoService {

    private final ContadorCodigoRepository repo;

    public CodigoService(ContadorCodigoRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public String proximoCodigo(String prefixo) {
        ContadorCodigo contador = repo.findByPrefixo(prefixo)
                .orElseGet(() -> repo.save(new ContadorCodigo(prefixo)));
        long valor = contador.proximo();
        repo.save(contador);
        return String.format("%s-%06d", prefixo, valor);
    }
}
