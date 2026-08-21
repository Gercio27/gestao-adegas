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
     * Que moagens usaram a uva de cada vindima e quantos Kg levaram. Devolve
     * [linhaId, codigo da moagem, data de inicio, estado, Kg]. E' o que permite
     * a folha da vindima mostrar para onde foi a uva de cada parcela.
     */
    @Query("select ev.linha.id, m.codigo, m.dataHoraInicio, m.dataCriacao, m.estado, "
            + "coalesce(sum(ev.quantidadeKg), 0) "
            + "from EnchimentoVindima ev join ev.enchimento e join e.moagem m "
            + "where ev.linha is not null "
            + "group by ev.linha.id, m.id, m.codigo, m.dataHoraInicio, m.dataCriacao, m.estado "
            + "order by m.codigo")
    List<Object[]> moagensPorVindima();
}
