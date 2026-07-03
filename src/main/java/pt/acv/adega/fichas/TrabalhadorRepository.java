package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TrabalhadorRepository extends JpaRepository<Trabalhador, Long> {
    List<Trabalhador> findAllByOrderByNomeAsc();
    List<Trabalhador> findByAtivoTrueOrderByNomeAsc();
    List<Trabalhador> findByDataCriacaoBetweenOrderByDataCriacaoAsc(LocalDateTime inicio, LocalDateTime fim);
}
