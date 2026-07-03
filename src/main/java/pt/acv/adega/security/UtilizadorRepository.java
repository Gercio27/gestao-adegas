package pt.acv.adega.security;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UtilizadorRepository extends JpaRepository<Utilizador, Long> {
    Optional<Utilizador> findByUsername(String username);
    boolean existsByUsername(String username);
    long countByPerfilAndAtivoTrue(pt.acv.adega.security.Perfil perfil);
}
