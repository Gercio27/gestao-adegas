package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArmazemRepository extends JpaRepository<Armazem, Long> {
    List<Armazem> findAllByOrderByNomeAsc();
}
