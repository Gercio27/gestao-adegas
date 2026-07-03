package pt.acv.adega.produtos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    List<Lote> findAllByOrderByDataCriacaoDesc();
}
