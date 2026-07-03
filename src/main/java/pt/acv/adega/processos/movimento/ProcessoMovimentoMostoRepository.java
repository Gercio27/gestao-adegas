package pt.acv.adega.processos.movimento;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;
import java.util.List;

public interface ProcessoMovimentoMostoRepository extends JpaRepository<ProcessoMovimentoMosto, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoMovimentoMosto> findAllByOrderByDataCriacaoDesc();
    List<ProcessoMovimentoMosto> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
