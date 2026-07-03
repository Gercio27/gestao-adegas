package pt.acv.adega.processos.comercial;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.acv.adega.processos.EstadoProcesso;
import java.util.List;

public interface ProcessoPassagemComercialRepository extends JpaRepository<ProcessoPassagemComercial, Long> {
    long countByEstado(EstadoProcesso estado);
    List<ProcessoPassagemComercial> findAllByOrderByDataCriacaoDesc();
    List<ProcessoPassagemComercial> findByCriadoPorOrderByDataCriacaoDesc(String criadoPor);
}
