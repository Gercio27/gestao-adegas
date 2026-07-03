package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TalhaRepository extends JpaRepository<Talha, Long> {
    List<Talha> findAllByOrderByIdentificacaoAsc();
    List<Talha> findByDataCriacaoBetweenOrderByDataCriacaoAsc(LocalDateTime inicio, LocalDateTime fim);
}
