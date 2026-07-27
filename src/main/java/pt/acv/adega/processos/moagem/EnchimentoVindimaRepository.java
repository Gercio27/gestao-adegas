package pt.acv.adega.processos.moagem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EnchimentoVindimaRepository extends JpaRepository<EnchimentoVindima, Long> {

    /**
     * Kg ja atribuidos a moagens, por vindima. Conta as moagens abertas tambem:
     * assim que se guarda a moagem os Kg ficam comprometidos e o saldo da
     * vindima desce, sem ser preciso fechar.
     */
    @Query("select ev.linha.id, coalesce(sum(ev.quantidadeKg), 0) from EnchimentoVindima ev "
            + "where ev.linha is not null group by ev.linha.id")
    List<Object[]> totaisPorVindima();
}
