package pt.acv.adega.processos.loteamento;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoteConstrucaoRepository extends JpaRepository<LoteConstrucao, Long> {
    List<LoteConstrucao> findByLoteamentoIdOrderByNumeroAsc(Long loteamentoId);
    long countByLoteamentoId(Long loteamentoId);
}
