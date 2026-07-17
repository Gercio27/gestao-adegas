package pt.acv.adega.processos.loteamento;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoteLinhaRepository extends JpaRepository<LoteLinha, Long> {
    List<LoteLinha> findByLoteamentoId(Long loteamentoId);
    void deleteByLoteamentoId(Long loteamentoId);
}
