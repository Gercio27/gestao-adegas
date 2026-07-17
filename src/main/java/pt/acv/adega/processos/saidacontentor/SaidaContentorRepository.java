package pt.acv.adega.processos.saidacontentor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SaidaContentorRepository extends JpaRepository<SaidaContentor, Long> {
    List<SaidaContentor> findAllByOrderByDataCriacaoDesc();
    List<SaidaContentor> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
