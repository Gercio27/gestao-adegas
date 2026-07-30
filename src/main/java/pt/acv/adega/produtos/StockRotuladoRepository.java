package pt.acv.adega.produtos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRotuladoRepository extends JpaRepository<StockRotulado, Long> {

    List<StockRotulado> findByGarrafasGreaterThanOrderByVinhoNomeAsc(int minimo);

    List<StockRotulado> findByVinhoEngarrafadoId(Long vinhoEngarrafadoId);
}
