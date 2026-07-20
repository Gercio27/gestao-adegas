package pt.acv.adega.processos.passagem;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PassagemItemRepository extends JpaRepository<PassagemItem, Long> {
    List<PassagemItem> findByProcessoId(Long processoId);
    void deleteByProcessoId(Long processoId);
}
