package pt.acv.adega.processos.passagem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras da passagem de mosto a vinho pronto a granel (Fase 4.5). Ao fechar,
 * os mostos selecionados (em fermentacao) passam a VINHO_GRANEL. Reversivel.
 */
@Service
public class PassagemService {

    private final ProcessoPassagemVinhoRepository repo;
    private final MostoRepository mostoRepo;

    public PassagemService(ProcessoPassagemVinhoRepository repo, MostoRepository mostoRepo) {
        this.repo = repo;
        this.mostoRepo = mostoRepo;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoPassagemVinho p = repo.findById(id)
                .orElseThrow(() -> new PassagemException("Processo não encontrado."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new PassagemException("O processo já está fechado.");

        List<Long> ids = parseIds(p.getMostosIdsCsv());
        if (ids.isEmpty()) throw new PassagemException("Selecione pelo menos um mosto para passar a vinho a granel.");

        int convertidos = 0;
        for (Long mid : ids) {
            Mosto m = mostoRepo.findById(mid).orElse(null);
            if (m != null && m.getEstado() == EstadoMosto.EM_FERMENTACAO) {
                m.setEstado(EstadoMosto.VINHO_GRANEL);
                mostoRepo.save(m);
                convertidos++;
            }
        }
        if (convertidos == 0) {
            throw new PassagemException("Nenhum dos mostos selecionados está em fermentação.");
        }

        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
    }

    @Transactional
    public void reabrir(Long id) {
        ProcessoPassagemVinho p = repo.findById(id)
                .orElseThrow(() -> new PassagemException("Processo não encontrado."));
        if (p.getEstado() == EstadoProcesso.ABERTO) return;
        for (Long mid : parseIds(p.getMostosIdsCsv())) {
            Mosto m = mostoRepo.findById(mid).orElse(null);
            if (m != null && m.getEstado() == EstadoMosto.VINHO_GRANEL) {
                m.setEstado(EstadoMosto.EM_FERMENTACAO);
                mostoRepo.save(m);
            }
        }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }

    private List<Long> parseIds(String csv) {
        List<Long> ids = new ArrayList<>();
        if (csv == null || csv.isBlank()) return ids;
        for (String s : csv.split(",")) {
            try { ids.add(Long.valueOf(s.trim())); } catch (Exception ignored) { }
        }
        return ids;
    }
}
