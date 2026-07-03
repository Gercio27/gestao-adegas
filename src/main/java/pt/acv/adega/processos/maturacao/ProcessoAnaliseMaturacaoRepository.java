package pt.acv.adega.processos.maturacao;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;

import java.util.List;
import java.util.Optional;

public interface ProcessoAnaliseMaturacaoRepository extends JpaRepository<ProcessoAnaliseMaturacao, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoAnaliseMaturacao> findAllByOrderByDataCriacaoDesc();
    List<ProcessoAnaliseMaturacao> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);

    /** Análise à maturação mais recente para uma vinha+casta (para o planeamento). */
    Optional<ProcessoAnaliseMaturacao> findFirstByVinhaIdAndCastaIdOrderByDataCriacaoDesc(Long vinhaId, Long castaId);
}
