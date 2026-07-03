package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConsumivelRepository extends JpaRepository<Consumivel, Long> {
    List<Consumivel> findAllByOrderByTipoAscDescricaoAsc();
    List<Consumivel> findByTipoOrderByDescricaoAsc(TipoConsumivel tipo);
}
