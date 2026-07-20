package pt.acv.adega.tratamentos;

import org.springframework.stereotype.Service;
import pt.acv.adega.planeamento.PlaneamentoVinho;
import pt.acv.adega.processos.moagem.ProcessoMoagem;
import pt.acv.adega.processos.moagem.ProcessoMoagemRepository;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.util.*;

/**
 * Dados de seleção partilhados pelos tratamentos enológicos e pelas análises:
 * dada uma categoria (mosto ou vinho a granel), devolve as adegas, os vinhos de
 * cada adega e os recipientes (talhas/depósitos) onde cada vinho está, com as
 * quantidades. Serve para o formulário: escolher adega → vinho → ver onde está.
 */
@Service
public class LocalizacaoVinhoService {

    private final MostoRepository mostoRepo;
    private final ProcessoMoagemRepository moagemRepo;

    public LocalizacaoVinhoService(MostoRepository mostoRepo, ProcessoMoagemRepository moagemRepo) {
        this.mostoRepo = mostoRepo;
        this.moagemRepo = moagemRepo;
    }

    /** Estrutura por categoria: MOSTO (fermentação/atesto) e GRANEL. */
    public Map<String, Object> dadosPorCategoria() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put(CategoriaVinho.MOSTO.name(), dados(List.of(EstadoMosto.EM_FERMENTACAO, EstadoMosto.ATESTADO)));
        out.put(CategoriaVinho.GRANEL.name(), dados(List.of(EstadoMosto.VINHO_GRANEL)));
        return out;
    }

    /** Para um conjunto de estados, agrega adegas, vinhos por adega e recipientes por (adega|vinho). */
    private Map<String, Object> dados(List<EstadoMosto> estados) {
        Map<Long, PlaneamentoVinho> plano = new HashMap<>();
        for (ProcessoMoagem mo : moagemRepo.findAll()) {
            if (mo.getPlano() != null) plano.put(mo.getId(), mo.getPlano());
        }

        Map<Long, String> adegaNomes = new LinkedHashMap<>();
        Map<Long, LinkedHashSet<String>> vinhosTmp = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> recipientes = new LinkedHashMap<>();

        for (EstadoMosto est : estados) {
            for (Mosto m : mostoRepo.findByEstadoOrderByDataProducaoDesc(est)) {
                Long adegaId = null;
                String local = "—";
                if (m.getTalha() != null && m.getTalha().getAdega() != null) {
                    adegaId = m.getTalha().getAdega().getId();
                    local = "Talha " + m.getTalha().getIdentificacao();
                    adegaNomes.putIfAbsent(adegaId, m.getTalha().getAdega().getNome());
                } else if (m.getDeposito() != null && m.getDeposito().getAdega() != null) {
                    adegaId = m.getDeposito().getAdega().getId();
                    local = "Depósito " + m.getDeposito().getIdentificacao();
                    adegaNomes.putIfAbsent(adegaId, m.getDeposito().getAdega().getNome());
                }
                if (adegaId == null) continue;
                String nome = nomeVinho(m, plano);
                if (nome == null || nome.isBlank()) continue;

                vinhosTmp.computeIfAbsent(adegaId, k -> new LinkedHashSet<>()).add(nome);
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("local", local);
                r.put("codigo", m.getCodigo());
                r.put("litros", m.getLitros() == null ? "0" : m.getLitros().toPlainString());
                recipientes.computeIfAbsent(adegaId + "|" + nome, k -> new ArrayList<>()).add(r);
            }
        }

        List<Map<String, Object>> adegas = new ArrayList<>();
        adegaNomes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.nullsLast(String::compareToIgnoreCase)))
                .forEach(e -> {
                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("id", e.getKey());
                    a.put("nome", e.getValue());
                    adegas.add(a);
                });
        Map<String, List<String>> vinhosPorAdega = new LinkedHashMap<>();
        for (Map.Entry<Long, LinkedHashSet<String>> e : vinhosTmp.entrySet()) {
            vinhosPorAdega.put(String.valueOf(e.getKey()), new ArrayList<>(e.getValue()));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("adegas", adegas);
        out.put("vinhosPorAdega", vinhosPorAdega);
        out.put("recipientes", recipientes);
        return out;
    }

    private String nomeVinho(Mosto m, Map<Long, PlaneamentoVinho> plano) {
        if (m.getVinhoNome() != null && !m.getVinhoNome().isBlank()) return m.getVinhoNome();
        PlaneamentoVinho w = m.getOrigemMoagemId() != null ? plano.get(m.getOrigemMoagemId()) : null;
        return w != null ? w.getNomeVinho() : null;
    }
}
