package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface DepositoRepository extends JpaRepository<Deposito, Long> {
    List<Deposito> findAllByOrderByIdentificacaoAsc();
    List<Deposito> findByDataCriacaoBetweenOrderByDataCriacaoAsc(LocalDateTime inicio, LocalDateTime fim);
}
