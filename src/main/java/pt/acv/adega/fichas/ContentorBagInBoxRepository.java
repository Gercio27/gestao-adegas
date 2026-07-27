package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContentorBagInBoxRepository extends JpaRepository<ContentorBagInBox, Long> {
    List<ContentorBagInBox> findAllByOrderByNomeAsc();
    List<ContentorBagInBox> findByVinhoEmbaladoIdOrderByNomeAsc(Long vinhoEmbaladoId);
    List<ContentorBagInBox> findByUnidadesAtuaisGreaterThanOrderByNomeAsc(int minimo);
}
