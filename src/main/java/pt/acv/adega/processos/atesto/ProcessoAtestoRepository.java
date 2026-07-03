package pt.acv.adega.processos.atesto;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;
import java.util.List;

public interface ProcessoAtestoRepository extends JpaRepository<ProcessoAtesto, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoAtesto> findAllByOrderByDataCriacaoDesc();
    List<ProcessoAtesto> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
