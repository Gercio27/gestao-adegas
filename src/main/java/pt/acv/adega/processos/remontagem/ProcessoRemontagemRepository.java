package pt.acv.adega.processos.remontagem;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;
import java.util.List;

public interface ProcessoRemontagemRepository extends JpaRepository<ProcessoRemontagem, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoRemontagem> findAllByOrderByDataCriacaoDesc();
    List<ProcessoRemontagem> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
