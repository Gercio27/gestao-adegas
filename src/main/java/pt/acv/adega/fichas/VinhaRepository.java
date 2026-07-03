package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface VinhaRepository extends JpaRepository<Vinha, Long> {
    List<Vinha> findAllByOrderByNomeAsc();
    List<Vinha> findByDataCriacaoBetweenOrderByDataCriacaoAsc(LocalDateTime inicio, LocalDateTime fim);
}
