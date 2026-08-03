package pt.acv.adega.processos.certificacao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.fichas.ContentorBagInBox;
import pt.acv.adega.fichas.ContentorBagInBoxRepository;
import pt.acv.adega.fichas.ContentorGarrafas;
import pt.acv.adega.fichas.ContentorGarrafasRepository;
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
    private final ContentorGarrafasRepository contentorRepo;
    private final ContentorBagInBoxRepository bibRepo;

    public CertificacaoService(ProcessoCertificacaoRepository repo, MostoRepository mostoRepo,
                               ContentorGarrafasRepository contentorRepo, ContentorBagInBoxRepository bibRepo,
                               VinhoEngarrafadoRepository engarrafadoRepo) {
        this.repo = repo;
        this.mostoRepo = mostoRepo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.contentorRepo = contentorRepo;
        this.bibRepo = bibRepo;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoCertificacao p = repo.findById(id)
                .orElseThrow(() -> new CertificacaoException("Certificação não encontrada."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new CertificacaoException("A certificação já está fechada.");

        List<Long> ids = idsParaCertificar(p);
        // Contentor cheio a mao na ficha: nao ha ficha de vinho engarrafado, por
        // isso certifica-se o proprio contentor.
        boolean soContentor = ids.isEmpty() && p.getAlvo() != AlvoCertificacao.GRANEL
                && p.getContentorId() != null;
        if (ids.isEmpty() && !soContentor) throw new CertificacaoException("Indique pelo menos um "
                + (p.getAlvo() == AlvoCertificacao.GRANEL ? "depósito a certificar." : "lote engarrafado a certificar."));

        boolean aprovado = p.getResultado() == ResultadoCertificacao.APROVADO;
        for (Long itemId : ids) {
            if (p.getAlvo() == AlvoCertificacao.GRANEL) {
                Mosto m = mostoRepo.findById(itemId).orElse(null);
                if (m != null) { m.setCertificado(aprovado); m.setValidadeCertificacao(aprovado ? p.getValidade() : null); mostoRepo.save(m); }
            } else {
                VinhoEngarrafado v = engarrafadoRepo.findById(itemId).orElse(null);
                if (v != null) {
                    v.setCertificado(aprovado);
                    v.setValidadeCertificacao(aprovado ? p.getValidade() : null);
                    engarrafadoRepo.save(v);
                    // Leva a certificacao aos contentores onde esse vinho esta,
                    // para se ver na ficha sem ter de ir ao processo.
                    marcarContentores(itemId, aprovado, p);
                }
            }
        }

        if (soContentor) marcarSoOContentor(p, aprovado);

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
        List<Long> paraLimpar = idsParaCertificar(p);
        if (paraLimpar.isEmpty() && p.getAlvo() != AlvoCertificacao.GRANEL && p.getContentorId() != null) {
            marcarSoOContentor(p, false);
        }
        for (Long itemId : paraLimpar) {
            if (p.getAlvo() == AlvoCertificacao.GRANEL) {
                Mosto m = mostoRepo.findById(itemId).orElse(null);
                if (m != null) { m.setCertificado(false); m.setValidadeCertificacao(null); mostoRepo.save(m); }
            } else {
                VinhoEngarrafado v = engarrafadoRepo.findById(itemId).orElse(null);
                if (v != null) {
                    v.setCertificado(false);
                    v.setValidadeCertificacao(null);
                    engarrafadoRepo.save(v);
                    marcarContentores(itemId, false, null);
                }
            }
        }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }

    /**
     * Poe (ou tira) a certificacao nos contentores de garrafas e nas paletes de
     * bag-in-box que tenham este vinho. So' toca nos que foram certificados por
     * um processo — os que foram preenchidos a mao na ficha ficam como estao.
     */
    private void marcarContentores(Long vinhoEngarrafadoId, boolean aprovado, ProcessoCertificacao p) {
        for (ContentorGarrafas c : contentorRepo.findByVinhoEngarrafadoId(vinhoEngarrafadoId)) {
            if (!aprovado && !ehDoProcesso(c.getCertificacaoCodigo(), p)) continue;
            aplicar(aprovado, p,
                    c::setCertificado, c::setValidadeCertificacao, c::setCertificacaoCodigo,
                    c::setCertificadoPdf, c::setCertificadoPdfNome, c::setCertificadoPdfTipo);
            contentorRepo.save(c);
        }
        for (ContentorBagInBox c : bibRepo.findByVinhoEmbaladoIdOrderByNomeAsc(vinhoEngarrafadoId)) {
            if (!aprovado && !ehDoProcesso(c.getCertificacaoCodigo(), p)) continue;
            aplicar(aprovado, p,
                    c::setCertificado, c::setValidadeCertificacao, c::setCertificacaoCodigo,
                    c::setCertificadoPdf, c::setCertificadoPdfNome, c::setCertificadoPdfTipo);
            bibRepo.save(c);
        }
    }

    private boolean ehDoProcesso(String codigoNoContentor, ProcessoCertificacao p) {
        // A reabrir (p == null) so' limpa o que veio de algum processo.
        return codigoNoContentor != null && !codigoNoContentor.isBlank();
    }

    private void aplicar(boolean aprovado, ProcessoCertificacao p,
                         java.util.function.Consumer<Boolean> setCertificado,
                         java.util.function.Consumer<java.time.LocalDate> setValidade,
                         java.util.function.Consumer<String> setCodigo,
                         java.util.function.Consumer<byte[]> setPdf,
                         java.util.function.Consumer<String> setPdfNome,
                         java.util.function.Consumer<String> setPdfTipo) {
        setCertificado.accept(aprovado);
        setValidade.accept(aprovado && p != null ? p.getValidade() : null);
        setCodigo.accept(aprovado && p != null ? p.getCodigo() : null);
        setPdf.accept(aprovado && p != null ? p.getCertificadoPdf() : null);
        setPdfNome.accept(aprovado && p != null ? p.getCertificadoPdfNome() : null);
        setPdfTipo.accept(aprovado && p != null ? p.getCertificadoPdfTipo() : null);
    }

    /**
     * Certifica (ou limpa) so' o contentor indicado no processo. E' o caso das
     * garrafas que foram registadas a mao na ficha do contentor: existem, mas
     * nao tem ficha de vinho engarrafado por tras.
     */
    private void marcarSoOContentor(ProcessoCertificacao p, boolean aprovado) {
        Long id = p.getContentorId();
        if (id == null) return;
        if (p.getTipoEmbalagem() == pt.acv.adega.fichas.TipoEmbalagem.BAG_IN_BOX) {
            bibRepo.findById(id).ifPresent(c -> {
                aplicar(aprovado, aprovado ? p : null,
                        c::setCertificado, c::setValidadeCertificacao, c::setCertificacaoCodigo,
                        c::setCertificadoPdf, c::setCertificadoPdfNome, c::setCertificadoPdfTipo);
                bibRepo.save(c);
            });
        } else {
            contentorRepo.findById(id).ifPresent(c -> {
                aplicar(aprovado, aprovado ? p : null,
                        c::setCertificado, c::setValidadeCertificacao, c::setCertificacaoCodigo,
                        c::setCertificadoPdf, c::setCertificadoPdfNome, c::setCertificadoPdfTipo);
                contentorRepo.save(c);
            });
        }
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
