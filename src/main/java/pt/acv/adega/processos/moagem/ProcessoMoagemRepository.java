package pt.acv.adega.processos.moagem;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;

import java.util.List;
import java.util.Optional;

public interface ProcessoMoagemRepository extends JpaRepository<ProcessoMoagem, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoMoagem> findAllByOrderByDataCriacaoDesc();
    List<ProcessoMoagem> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);

    /** Moagem associada a uma linha de planeamento vindimada (1 por linha). */
    Optional<ProcessoMoagem> findFirstByOrigemVindimaId(Long origemVindimaId);
}
