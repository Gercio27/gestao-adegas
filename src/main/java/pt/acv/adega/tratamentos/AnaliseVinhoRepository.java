package pt.acv.adega.tratamentos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnaliseVinhoRepository extends JpaRepository<AnaliseVinho, Long> {
    List<AnaliseVinho> findAllByOrderByVinhoNomeAscDataAnaliseAsc();
}
