package pt.acv.adega.planeamento;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaneamentoVinhoRepository extends JpaRepository<PlaneamentoVinho, Long> {
    List<PlaneamentoVinho> findAllByOrderByVinhaNomeAscCastaNomeAsc();
}
