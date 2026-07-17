package pt.acv.adega.processos.rotulagem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.fichas.Consumivel;
import pt.acv.adega.fichas.ConsumivelRepository;
import pt.acv.adega.fichas.ContentorGarrafas;
import pt.acv.adega.fichas.ContentorGarrafasRepository;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.VinhoEngarrafado;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;

/**
 * Regras da Rotulagem (Fase 7). Ao fechar da baixa de rotulos/capsulas/caixas e
 * marca o vinho engarrafado como rotulado. Reversivel. Transacional.
 */
@Service
public class RotulagemService {

    private final ProcessoRotulagemRepository repo;
    private final ConsumivelRepository consumivelRepo;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final ContentorGarrafasRepository contentorRepo;

    public RotulagemService(ProcessoRotulagemRepository repo, ConsumivelRepository consumivelRepo,
                            VinhoEngarrafadoRepository engarrafadoRepo, ContentorGarrafasRepository contentorRepo) {
        this.repo = repo;
        this.consumivelRepo = consumivelRepo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.contentorRepo = contentorRepo;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoRotulagem p = repo.findById(id)
                .orElseThrow(() -> new RotulagemException("Rotulagem não encontrada."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new RotulagemException("A rotulagem já está fechada.");
        if (p.getEngarrafado() == null) throw new RotulagemException("Indique o vinho engarrafado a rotular.");
        if (p.getRotulo() == null || p.getNumeroRotulos() <= 0) throw new RotulagemException("Indique os rótulos e a quantidade.");

        Consumivel rotulo = carregar(p.getRotulo().getId(), "Rótulo");
        exigirStock(rotulo, p.getNumeroRotulos());
        Consumivel capsula = p.getCapsula() != null ? carregar(p.getCapsula().getId(), "Cápsula") : null;
        if (capsula != null) exigirStock(capsula, p.getNumeroCapsulas());
        Consumivel caixa = p.getCaixa() != null ? carregar(p.getCaixa().getId(), "Caixa") : null;
        if (caixa != null) exigirStock(caixa, p.getNumeroCaixas());

        rotulo.setStock(rotulo.getStock() - p.getNumeroRotulos());
        consumivelRepo.save(rotulo);
        if (capsula != null) { capsula.setStock(capsula.getStock() - p.getNumeroCapsulas()); consumivelRepo.save(capsula); }
        if (caixa != null) { caixa.setStock(caixa.getStock() - p.getNumeroCaixas()); consumivelRepo.save(caixa); }

        VinhoEngarrafado veg = engarrafadoRepo.findById(p.getEngarrafado().getId())
                .orElseThrow(() -> new RotulagemException("Vinho engarrafado não encontrado."));
        veg.setRotulado(true);
        engarrafadoRepo.save(veg);

        // Marca os contentores desse vinho como rotulados (ficam prontos para o comercial).
        for (ContentorGarrafas c : contentorRepo.findByVinhoEngarrafadoId(veg.getId())) {
            c.setRotulado(true);
            contentorRepo.save(c);
        }

        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
    }

    @Transactional
    public void reabrir(Long id) {
        ProcessoRotulagem p = repo.findById(id)
                .orElseThrow(() -> new RotulagemException("Rotulagem não encontrada."));
        if (p.getEstado() == EstadoProcesso.ABERTO) return;

        repor(p.getRotulo(), p.getNumeroRotulos());
        repor(p.getCapsula(), p.getNumeroCapsulas());
        repor(p.getCaixa(), p.getNumeroCaixas());

        if (p.getEngarrafado() != null) {
            VinhoEngarrafado veg = engarrafadoRepo.findById(p.getEngarrafado().getId()).orElse(null);
            if (veg != null) {
                veg.setRotulado(false);
                engarrafadoRepo.save(veg);
                for (ContentorGarrafas c : contentorRepo.findByVinhoEngarrafadoId(veg.getId())) {
                    c.setRotulado(false);
                    contentorRepo.save(c);
                }
            }
        }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }

    private Consumivel carregar(Long id, String nome) {
        return consumivelRepo.findById(id)
                .orElseThrow(() -> new RotulagemException(nome + " não encontrado(a)."));
    }

    private void exigirStock(Consumivel c, int qtd) {
        if (c.getStock() < qtd) {
            throw new RotulagemException(String.format(
                    "Stock insuficiente de %s (%s): tem %d, precisa de %d.",
                    c.getTipo().getDescricao(), c.getCodigo(), c.getStock(), qtd));
        }
    }

    private void repor(Consumivel ref, int qtd) {
        if (ref == null) return;
        Consumivel c = consumivelRepo.findById(ref.getId()).orElse(null);
        if (c != null) { c.setStock(c.getStock() + qtd); consumivelRepo.save(c); }
    }
}
