package pt.acv.adega.processos.engarrafamento;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;
import java.util.List;

public interface ProcessoEngarrafamentoRepository extends JpaRepository<ProcessoEngarrafamento, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoEngarrafamento> findAllByOrderByDataCriacaoDesc();
    List<ProcessoEngarrafamento> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
