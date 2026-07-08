package pt.acv.adega.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistoAuditoriaRepository extends JpaRepository<RegistoAuditoria, Long> {

    /** Ultimos registos (limite defensivo), do mais recente para o mais antigo. */
    List<RegistoAuditoria> findTop500ByOrderByDataHoraDesc();

    /** Ultimos registos de um utilizador especifico. */
    List<RegistoAuditoria> findTop500ByUsernameOrderByDataHoraDesc(String username);
}
