package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContentorGarrafasRepository extends JpaRepository<ContentorGarrafas, Long> {
    List<ContentorGarrafas> findAllByOrderByNomeAsc();
    List<ContentorGarrafas> findByVinhoEngarrafadoId(Long vinhoEngarrafadoId);
    List<ContentorGarrafas> findByVinhoEngarrafadoIdOrderByNomeAsc(Long vinhoEngarrafadoId);
    List<ContentorGarrafas> findByRotuladoTrueAndGarrafasAtuaisGreaterThanOrderByNomeAsc(int minimo);
}
