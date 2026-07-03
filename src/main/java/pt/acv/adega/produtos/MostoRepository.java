package pt.acv.adega.produtos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MostoRepository extends JpaRepository<Mosto, Long> {
    List<Mosto> findAllByOrderByDataProducaoDesc();
    List<Mosto> findByOrigemMoagemId(Long origemMoagemId);
    List<Mosto> findByEstadoOrderByDataProducaoDesc(EstadoMosto estado);
    List<Mosto> findByTalhaId(Long talhaId);
    List<Mosto> findByDepositoId(Long depositoId);
    List<Mosto> findByLoteCodigo(String loteCodigo);
    List<Mosto> findByOrigemMovimentoId(Long origemMovimentoId);
    List<Mosto> findByDataCriacaoBetweenOrderByDataCriacaoAsc(LocalDateTime inicio, LocalDateTime fim);
}
