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

    /**
     * Esta colheita ja foi usada nalguma moagem? Se sim, nao pode ser apagada.
     * Escrito a mao porque {@code existsByColheitaId} apanharia o campo
     * transiente {@code colheitaId} em vez da ligacao a colheita.
     */
    @Query("select count(ev) > 0 from EnchimentoVindima ev where ev.colheita.id = ?1")
    boolean colheitaJaUsada(Long colheitaId);

    /** Kg ja atribuidos a moagens, por colheita. So' conta o que tem colheita indicada. */
    @Query("select ev.colheita.id, coalesce(sum(ev.quantidadeKg), 0) from EnchimentoVindima ev "
            + "where ev.colheita is not null group by ev.colheita.id")
    List<Object[]> totaisPorColheita();

    /**
     * Que moagens usaram a uva de cada vindima e quantos Kg levaram. Devolve
     * [linhaId, colheitaId, codigo da moagem, data de inicio, data de criacao,
     * estado, Kg]. O colheitaId vem a nulo nos registos feitos antes de a
     * escolha da colheita existir.
     */
    @Query("select ev.linha.id, ev.colheita.id, m.codigo, m.dataHoraInicio, m.dataCriacao, m.estado, "
            + "coalesce(sum(ev.quantidadeKg), 0) "
            + "from EnchimentoVindima ev join ev.enchimento e join e.moagem m "
            + "where ev.linha is not null "
            + "group by ev.linha.id, ev.colheita.id, m.id, m.codigo, m.dataHoraInicio, m.dataCriacao, m.estado "
            + "order by ev.linha.id, m.id")
    List<Object[]> moagensPorVindima();
}
