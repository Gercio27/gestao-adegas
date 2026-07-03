package pt.acv.adega.processos.certificacao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;
import pt.acv.adega.produtos.VinhoEngarrafado;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;

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

        if (p.getAlvo() == AlvoCertificacao.GRANEL && p.getVinhoGranel() == null)
            throw new CertificacaoException("Indique o vinho a granel a certificar.");
        if (p.getAlvo() == AlvoCertificacao.ENGARRAFADO && p.getEngarrafado() == null)
            throw new CertificacaoException("Indique o vinho engarrafado a certificar.");

        boolean aprovado = p.getResultado() == ResultadoCertificacao.APROVADO;
        if (p.getAlvo() == AlvoCertificacao.GRANEL) {
            Mosto m = mostoRepo.findById(p.getVinhoGranel().getId()).orElseThrow();
            m.setCertificado(aprovado);
            m.setValidadeCertificacao(aprovado ? p.getValidade() : null);
            mostoRepo.save(m);
        } else {
            VinhoEngarrafado v = engarrafadoRepo.findById(p.getEngarrafado().getId()).orElseThrow();
            v.setCertificado(aprovado);
            v.setValidadeCertificacao(aprovado ? p.getValidade() : null);
            engarrafadoRepo.save(v);
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
        if (p.getAlvo() == AlvoCertificacao.GRANEL && p.getVinhoGranel() != null) {
            Mosto m = mostoRepo.findById(p.getVinhoGranel().getId()).orElse(null);
            if (m != null) { m.setCertificado(false); m.setValidadeCertificacao(null); mostoRepo.save(m); }
        } else if (p.getEngarrafado() != null) {
            VinhoEngarrafado v = engarrafadoRepo.findById(p.getEngarrafado().getId()).orElse(null);
            if (v != null) { v.setCertificado(false); v.setValidadeCertificacao(null); engarrafadoRepo.save(v); }
        }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }
}
