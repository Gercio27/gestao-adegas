package pt.acv.adega.processos.certificacao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;
import pt.acv.adega.produtos.VinhoEngarrafado;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fecho/reabertura da certificacao. Ao fechar com resultado APROVADO, marca o
 * produto (a granel ou engarrafado) como certificado, com a validade indicada.
 */
@Service
public class CertificacaoService {

    private final ProcessoCertificacaoRepository repo;
    private final MostoRepository mostoRepo;
    private final VinhoEngarrafadoRepository engarrafadoRepo;

    public CertificacaoService(ProcessoCertificacaoRepository repo, MostoRepository mostoRepo,
                               VinhoEngarrafadoRepository engarrafadoRepo) {
        this.repo = repo;
        this.mostoRepo = mostoRepo;
        this.engarrafadoRepo = engarrafadoRepo;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoCertificacao p = repo.findById(id)
                .orElseThrow(() -> new CertificacaoException("Certificação não encontrada."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new CertificacaoException("A certificação já está fechada.");

        List<Long> ids = idsParaCertificar(p);
        if (ids.isEmpty()) throw new CertificacaoException("Indique pelo menos um "
                + (p.getAlvo() == AlvoCertificacao.GRANEL ? "depósito a certificar." : "lote engarrafado a certificar."));

        boolean aprovado = p.getResultado() == ResultadoCertificacao.APROVADO;
        for (Long itemId : ids) {
            if (p.getAlvo() == AlvoCertificacao.GRANEL) {
                Mosto m = mostoRepo.findById(itemId).orElse(null);
                if (m != null) { m.setCertificado(aprovado); m.setValidadeCertificacao(aprovado ? p.getValidade() : null); mostoRepo.save(m); }
            } else {
                VinhoEngarrafado v = engarrafadoRepo.findById(itemId).orElse(null);
                if (v != null) { v.setCertificado(aprovado); v.setValidadeCertificacao(aprovado ? p.getValidade() : null); engarrafadoRepo.save(v); }
            }
        }

        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
    }

    @Transactional
    public void reabrir(Long id) {
        ProcessoCertificacao p = repo.findById(id)
                .orElseThrow(() -> new CertificacaoException("Certificação não encontrada."));
        if (p.getEstado() == EstadoProcesso.ABERTO) return;
        for (Long itemId : idsParaCertificar(p)) {
            if (p.getAlvo() == AlvoCertificacao.GRANEL) {
                Mosto m = mostoRepo.findById(itemId).orElse(null);
                if (m != null) { m.setCertificado(false); m.setValidadeCertificacao(null); mostoRepo.save(m); }
            } else {
                VinhoEngarrafado v = engarrafadoRepo.findById(itemId).orElse(null);
                if (v != null) { v.setCertificado(false); v.setValidadeCertificacao(null); engarrafadoRepo.save(v); }
            }
        }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }

    /** Ids dos itens a certificar: do CSV, ou (retro) do único alvo antigo. */
    private List<Long> idsParaCertificar(ProcessoCertificacao p) {
        List<Long> ids = new ArrayList<>();
        if (p.getItensIdsCsv() != null && !p.getItensIdsCsv().isBlank()) {
            for (String s : p.getItensIdsCsv().split(",")) {
                try { ids.add(Long.valueOf(s.trim())); } catch (Exception ignored) { }
            }
        } else if (p.getAlvo() == AlvoCertificacao.GRANEL && p.getVinhoGranel() != null) {
            ids.add(p.getVinhoGranel().getId());
        } else if (p.getEngarrafado() != null) {
            ids.add(p.getEngarrafado().getId());
        }
        return ids;
    }
}
