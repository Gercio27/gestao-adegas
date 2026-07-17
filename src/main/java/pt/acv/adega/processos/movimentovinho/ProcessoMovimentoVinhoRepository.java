package pt.acv.adega.processos.movimentovinho;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessoMovimentoVinhoRepository extends JpaRepository<ProcessoMovimentoVinho, Long> {
    List<ProcessoMovimentoVinho> findAllByOrderByDataCriacaoDesc();
    List<ProcessoMovimentoVinho> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
