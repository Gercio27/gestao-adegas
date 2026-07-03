package pt.acv.adega.processos.rotulagem;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;
import java.util.List;

public interface ProcessoRotulagemRepository extends JpaRepository<ProcessoRotulagem, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoRotulagem> findAllByOrderByDataCriacaoDesc();
    List<ProcessoRotulagem> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
