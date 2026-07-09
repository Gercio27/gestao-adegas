package pt.acv.adega.fichas;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de parcelas. Necessario para o planeamento (converter id->Parcela
 * nos formularios e listar/atualizar parcelas).
 */
public interface ParcelaRepository extends JpaRepository<Parcela, Long> {
}
