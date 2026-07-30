package pt.acv.adega.movimentos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentoStockRepository extends JpaRepository<MovimentoStock, Long> {

    List<MovimentoStock> findByTipoAlvoAndAlvoIdOrderByDataHoraDesc(TipoAlvo tipoAlvo, Long alvoId);

    List<MovimentoStock> findByTipoAlvoOrderByDataHoraDesc(TipoAlvo tipoAlvo);

    List<MovimentoStock> findByLocalOrderByDataHoraDesc(String local);

    List<MovimentoStock> findTop300ByOrderByDataHoraDesc();
}
