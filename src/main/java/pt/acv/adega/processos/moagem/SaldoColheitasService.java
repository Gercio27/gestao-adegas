package pt.acv.adega.processos.moagem;

import org.springframework.stereotype.Service;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcela;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcelaRepository;
import pt.acv.adega.planeamento.RegistoVindima;
import pt.acv.adega.processos.EstadoProcesso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Conta, colheita a colheita, quantos Kg ja foram moidos e quantos faltam.
 *
 * <p>Existe um so' porque duas paginas precisam da mesma conta — a folha da
 * vindima (para mostrar onde foi parar a uva) e a da moagem (para so' oferecer
 * o que ainda ha' por moer). Se cada uma fizesse a sua, uma diria "por moer 0"
 * e a outra ofereceria a colheita como disponivel.
 *
 * <p>Ha' dois tipos de registo a somar:
 * <ul>
 *   <li>os que indicam a colheita — o utilizador escolheu-a no ecra da moagem;
 *       vao direto a essa colheita;</li>
 *   <li>os que so' indicam a parcela — feitos antes de a escolha existir. Nao ha'
 *       como saber a colheita real, por isso sao repartidos por ordem de chegada
 *       (colheitas mais antigas primeiro) e ficam marcados como estimados.</li>
 * </ul>
 */
@Service
public class SaldoColheitasService {

    private final LinhaPlaneamentoParcelaRepository linhaRepo;
    private final EnchimentoVindimaRepository enchimentoVindimaRepo;

    public SaldoColheitasService(LinhaPlaneamentoParcelaRepository linhaRepo,
                                 EnchimentoVindimaRepository enchimentoVindimaRepo) {
        this.linhaRepo = linhaRepo;
        this.enchimentoVindimaRepo = enchimentoVindimaRepo;
    }

    /** Uma moagem que levou uva de uma colheita, e quanto levou. */
    public record Uso(String moagemCodigo, LocalDate data, BigDecimal kg, boolean aberta) {
    }

    /**
     * O retrato completo.
     *
     * @param usosPorColheita   por colheita, as moagens que a usaram
     * @param moidoPorColheita  por colheita, o total ja moido
     * @param porMoerPorColheita por colheita, o que ainda falta moer (nunca negativo)
     * @param excessoPorParcela por parcela, os Kg moidos a mais do que os colhidos
     * @param estimadas         colheitas cujo valor saiu da reparticao, nao do registo
     */
    public record Rastreio(Map<Long, List<Uso>> usosPorColheita,
                           Map<Long, BigDecimal> moidoPorColheita,
                           Map<Long, BigDecimal> porMoerPorColheita,
                           Map<Long, BigDecimal> excessoPorParcela,
                           Set<Long> estimadas) {

        public BigDecimal moido(Long colheitaId) {
            return moidoPorColheita.getOrDefault(colheitaId, BigDecimal.ZERO);
        }
    }

    /** Kg que uma moagem tirou de uma parcela; colheitaId a nulo = registo antigo. */
    private record Consumo(Long linhaId, Long colheitaId, Uso uso) {
    }

    public Rastreio calcular() {
        return calcular(linhaRepo.findAll());
    }

    public Rastreio calcular(List<LinhaPlaneamentoParcela> linhas) {
        Map<Long, List<Consumo>> porParcela = consumosPorParcela();

        Map<Long, List<Uso>> usosPorColheita = new HashMap<>();
        Map<Long, BigDecimal> moidoPorColheita = new HashMap<>();
        Map<Long, BigDecimal> porMoerPorColheita = new HashMap<>();
        Map<Long, BigDecimal> excessoPorParcela = new HashMap<>();
        Set<Long> estimadas = new HashSet<>();

        for (LinhaPlaneamentoParcela l : linhas) {
            // As colheitas ja vem por ordem de criacao (@OrderBy("id")).
            List<RegistoVindima> colheitas = l.getVindimas();
            Map<Long, BigDecimal> saldo = new LinkedHashMap<>();
            for (RegistoVindima v : colheitas) {
                saldo.put(v.getId(), v.getQuantidadeKg() == null ? BigDecimal.ZERO : v.getQuantidadeKg());
            }
            List<Consumo> consumos = porParcela.getOrDefault(l.getId(), List.of());
            BigDecimal excesso = BigDecimal.ZERO;

            // 1) O que o utilizador escolheu vai direto a colheita dele.
            for (Consumo c : consumos) {
                if (c.colheitaId() == null || !saldo.containsKey(c.colheitaId())) continue;
                registar(usosPorColheita, moidoPorColheita, c.colheitaId(), c.uso());
                saldo.merge(c.colheitaId(), c.uso().kg().negate(), BigDecimal::add);
            }
            // 2) Os registos antigos sao repartidos por ordem de chegada.
            for (Consumo c : consumos) {
                if (c.colheitaId() != null && saldo.containsKey(c.colheitaId())) continue;
                BigDecimal falta = c.uso().kg();
                for (RegistoVindima v : colheitas) {
                    if (falta.signum() <= 0) break;
                    BigDecimal disponivel = saldo.get(v.getId());
                    if (disponivel == null || disponivel.signum() <= 0) continue;
                    BigDecimal usa = disponivel.min(falta);
                    Uso u = c.uso();
                    registar(usosPorColheita, moidoPorColheita, v.getId(),
                            new Uso(u.moagemCodigo(), u.data(), usa, u.aberta()));
                    estimadas.add(v.getId());
                    saldo.put(v.getId(), disponivel.subtract(usa));
                    falta = falta.subtract(usa);
                }
                if (falta.signum() > 0) excesso = excesso.add(falta);
            }

            // Saldo negativo = moeu-se mais dessa colheita do que se colheu.
            for (RegistoVindima v : colheitas) {
                BigDecimal s = saldo.get(v.getId());
                if (s == null) s = BigDecimal.ZERO;
                if (s.signum() < 0) {
                    excesso = excesso.add(s.negate());
                    s = BigDecimal.ZERO;
                }
                porMoerPorColheita.put(v.getId(), s);
            }
            if (excesso.signum() > 0) excessoPorParcela.put(l.getId(), excesso);
        }

        return new Rastreio(usosPorColheita, moidoPorColheita, porMoerPorColheita,
                excessoPorParcela, estimadas);
    }

    private void registar(Map<Long, List<Uso>> usos, Map<Long, BigDecimal> moido, Long colheitaId, Uso u) {
        usos.computeIfAbsent(colheitaId, k -> new ArrayList<>()).add(u);
        moido.merge(colheitaId, u.kg(), BigDecimal::add);
    }

    private Map<Long, List<Consumo>> consumosPorParcela() {
        Map<Long, List<Consumo>> mapa = new HashMap<>();
        for (Object[] l : enchimentoVindimaRepo.moagensPorVindima()) {
            if (l[0] == null) continue;
            Long linhaId = (Long) l[0];
            Long colheitaId = (Long) l[1];
            String codigo = (String) l[2];
            // A data da moagem e a de inicio; se nao foi preenchida, vale a de
            // criacao — a mesma regra do ecra da moagem.
            LocalDateTime inicio = (LocalDateTime) l[3];
            LocalDateTime criacao = (LocalDateTime) l[4];
            LocalDateTime quando = inicio != null ? inicio : criacao;
            boolean aberta = l[5] == EstadoProcesso.ABERTO;
            BigDecimal kg = l[6] == null ? BigDecimal.ZERO : (BigDecimal) l[6];
            mapa.computeIfAbsent(linhaId, k -> new ArrayList<>()).add(new Consumo(linhaId, colheitaId,
                    new Uso(codigo, quando != null ? quando.toLocalDate() : null, kg, aberta)));
        }
        return mapa;
    }
}
