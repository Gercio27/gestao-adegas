package pt.acv.adega.processos.passagem;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;
import java.util.List;

public interface ProcessoPassagemVinhoRepository extends JpaRepository<ProcessoPassagemVinho, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoPassagemVinho> findAllByOrderByDataCriacaoDesc();
    List<ProcessoPassagemVinho> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
