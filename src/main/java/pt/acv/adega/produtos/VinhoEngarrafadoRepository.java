package pt.acv.adega.produtos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VinhoEngarrafadoRepository extends JpaRepository<VinhoEngarrafado, Long> {
    List<VinhoEngarrafado> findAllByOrderByDataProducaoDesc();
    List<VinhoEngarrafado> findByRotuladoFalseOrderByDataProducaoDesc();
    List<VinhoEngarrafado> findByRotuladoTrueOrderByDataProducaoDesc();
    List<VinhoEngarrafado> findByOrigemEngarrafamentoId(Long origemEngarrafamentoId);
}
