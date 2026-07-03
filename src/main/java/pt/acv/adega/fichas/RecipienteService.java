package pt.acv.adega.fichas;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilitario partilhado para lidar com recipientes (talhas ou depositos) nos
 * processos: gera as opcoes para os seletores e resolve a referencia
 * "TALHA:id" / "DEPOSITO:id" para a entidade correspondente.
 */
@Service
public class RecipienteService {

    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;

    public RecipienteService(TalhaRepository talhaRepo, DepositoRepository depositoRepo) {
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
    }

    public record Opcao(String ref, String label) { }

    /** Um recipiente resolvido e exatamente uma talha OU um deposito. */
    public record Recipiente(Talha talha, Deposito deposito) {
        public boolean vazio() { return talha == null && deposito == null; }
    }

    public List<Opcao> opcoes() {
        List<Opcao> lista = new ArrayList<>();
        talhaRepo.findAllByOrderByIdentificacaoAsc().forEach(t ->
                lista.add(new Opcao("TALHA:" + t.getId(),
                        "Talha " + t.getIdentificacao() + estado(t.getCapacidadeLitros(), t.getVolumeAtualLitros()))));
        depositoRepo.findAllByOrderByIdentificacaoAsc().forEach(d ->
                lista.add(new Opcao("DEPOSITO:" + d.getId(),
                        "Depósito " + d.getIdentificacao() + estado(d.getCapacidadeLitros(), d.getVolumeAtualLitros()))));
        return lista;
    }

    public Recipiente resolver(String ref) {
        if (ref == null || !ref.contains(":")) return new Recipiente(null, null);
        String[] p = ref.split(":", 2);
        Long id;
        try { id = Long.valueOf(p[1].trim()); } catch (Exception e) { return new Recipiente(null, null); }
        if ("TALHA".equals(p[0])) return new Recipiente(talhaRepo.findById(id).orElse(null), null);
        if ("DEPOSITO".equals(p[0])) return new Recipiente(null, depositoRepo.findById(id).orElse(null));
        return new Recipiente(null, null);
    }

    private String estado(BigDecimal cap, BigDecimal vol) {
        BigDecimal v = vol == null ? BigDecimal.ZERO : vol;
        if (cap == null) return " (" + v.toPlainString() + " L, sem cap.)";
        return " (" + v.toPlainString() + "/" + cap.toPlainString() + " L)";
    }
}
