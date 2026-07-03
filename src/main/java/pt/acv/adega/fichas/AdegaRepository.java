package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AdegaRepository extends JpaRepository<Adega, Long> {
    List<Adega> findAllByOrderByNomeAsc();
    List<Adega> findByDataCriacaoBetweenOrderByDataCriacaoAsc(LocalDateTime inicio, LocalDateTime fim);
}
