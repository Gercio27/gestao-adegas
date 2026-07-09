package pt.acv.adega.processos.moagem;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;

import java.util.List;

public interface ProcessoMoagemRepository extends JpaRepository<ProcessoMoagem, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoMoagem> findAllByOrderByDataCriacaoDesc();
    List<ProcessoMoagem> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
