package pt.acv.adega.processos.certificacao;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;
import java.util.List;

public interface ProcessoCertificacaoRepository extends JpaRepository<ProcessoCertificacao, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoCertificacao> findAllByOrderByDataCriacaoDesc();
    List<ProcessoCertificacao> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
