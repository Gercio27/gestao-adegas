package pt.acv.adega.processos.vindima;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;

import java.time.LocalDateTime;
import java.util.List;

public interface ProcessoVindimaRepository extends JpaRepository<ProcessoVindima, Long> {

    long countByEstado(EstadoProcesso estado);

    List<ProcessoVindima> findAllByOrderByDataCriacaoDesc();

    /** Processos abertos por um utilizador (para o operador ver os seus). */
    List<ProcessoVindima> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);

    List<ProcessoVindima> findByDataCriacaoBetweenOrderByDataCriacaoAsc(LocalDateTime inicio, LocalDateTime fim);
}
