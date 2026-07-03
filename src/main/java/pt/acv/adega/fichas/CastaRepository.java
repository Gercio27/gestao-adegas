package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface CastaRepository extends JpaRepository<Casta, Long> {
    List<Casta> findAllByOrderByNomeAsc();
    boolean existsByNomeIgnoreCase(String nome);
    long countByDataCriacaoBetween(LocalDateTime inicio, LocalDateTime fim);
    List<Casta> findByDataCriacaoBetweenOrderByDataCriacaoAsc(LocalDateTime inicio, LocalDateTime fim);
}
