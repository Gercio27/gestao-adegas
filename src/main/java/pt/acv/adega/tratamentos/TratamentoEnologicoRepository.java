package pt.acv.adega.tratamentos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TratamentoEnologicoRepository extends JpaRepository<TratamentoEnologico, Long> {
    List<TratamentoEnologico> findAllByOrderByVinhoNomeAscDataTratamentoAsc();
}
