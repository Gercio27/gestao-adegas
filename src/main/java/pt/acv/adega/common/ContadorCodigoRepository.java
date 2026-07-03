package pt.acv.adega.common;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ContadorCodigoRepository extends JpaRepository<ContadorCodigo, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ContadorCodigo> findByPrefixo(String prefixo);
}
