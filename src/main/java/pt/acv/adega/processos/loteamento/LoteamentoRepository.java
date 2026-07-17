package pt.acv.adega.processos.loteamento;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoteamentoRepository extends JpaRepository<Loteamento, Long> {
    List<Loteamento> findAllByOrderByDataCriacaoDesc();
    List<Loteamento> findByConcluidoFalseOrderByDataCriacaoDesc();
    boolean existsByNomeIgnoreCase(String nome);
}
