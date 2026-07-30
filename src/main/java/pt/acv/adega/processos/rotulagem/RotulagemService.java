package pt.acv.adega.processos.rotulagem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.fichas.Consumivel;
import pt.acv.adega.fichas.ConsumivelRepository;
import pt.acv.adega.fichas.ContentorBagInBox;
import pt.acv.adega.fichas.ContentorBagInBoxRepository;
import pt.acv.adega.fichas.ContentorGarrafas;
import pt.acv.adega.fichas.ContentorGarrafasRepository;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.StockRotulado;
import pt.acv.adega.produtos.StockRotuladoRepository;
import pt.acv.adega.produtos.VinhoEngarrafado;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

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
    private final ContentorBagInBoxRepository bibRepo;
    private final StockRotuladoRepository stockRepo;
    private final pt.acv.adega.fichas.AdegaRepository adegaRepo;
    private final pt.acv.adega.fichas.ArmazemRepository armazemRepo;

    public RotulagemService(ProcessoRotulagemRepository repo, ConsumivelRepository consumivelRepo,
                            VinhoEngarrafadoRepository engarrafadoRepo, ContentorGarrafasRepository contentorRepo,
                            ContentorBagInBoxRepository bibRepo, StockRotuladoRepository stockRepo,
                            pt.acv.adega.fichas.AdegaRepository adegaRepo,
                            pt.acv.adega.fichas.ArmazemRepository armazemRepo) {
        this.repo = repo;
        this.consumivelRepo = consumivelRepo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.contentorRepo = contentorRepo;
        this.bibRepo = bibRepo;
        this.stockRepo = stockRepo;
        this.adegaRepo = adegaRepo;
        this.armazemRepo = armazemRepo;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoRotulagem p = repo.findById(id)
                .orElseThrow(() -> new RotulagemException("Rotulagem não encontrada."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new RotulagemException("A rotulagem já está fechada.");
        if (p.getEngarrafado() == null) throw new RotulagemException("Indique o vinho engarrafado a rotular.");
        if (p.getRotulo() == null || p.getNumeroRotulos() <= 0) throw new RotulagemException("Indique os rótulos e a quantidade.");
        if (p.getCaixasRotuladas() <= 0) throw new RotulagemException("Indique quantas caixas foram rotuladas (> 0).");
        if (p.getGarrafasPorCaixa() <= 0) throw new RotulagemException("Indique quantas garrafas leva cada caixa (> 0).");
        // As garrafas rotuladas sao as que cabem nas caixas cheias.
        int garrafas = p.getCaixasRotuladas() * p.getGarrafasPorCaixa();
        p.setNumeroGarrafas(garrafas);

        Consumivel rotulo = carregar(p.getRotulo().getId(), "Rótulo");
        exigirStock(rotulo, p.getNumeroRotulos());
        Consumivel capsula = p.getCapsula() != null ? carregar(p.getCapsula().getId(), "Cápsula") : null;
        if (capsula != null) exigirStock(capsula, p.getNumeroCapsulas());
        Consumivel caixa = p.getCaixa() != null ? carregar(p.getCaixa().getId(), "Caixa") : null;
        // Gasta-se uma caixa por cada caixa rotulada.
        if (caixa != null) { p.setNumeroCaixas(p.getCaixasRotuladas()); exigirStock(caixa, p.getNumeroCaixas()); }
        Consumivel etiqueta = p.getEtiqueta() != null ? carregar(p.getEtiqueta().getId(), "Etiqueta") : null;
        if (etiqueta != null) exigirStock(etiqueta, p.getNumeroEtiquetas());

        rotulo.setStock(rotulo.getStock() - p.getNumeroRotulos());
        consumivelRepo.save(rotulo);
        if (capsula != null) { capsula.setStock(capsula.getStock() - p.getNumeroCapsulas()); consumivelRepo.save(capsula); }
        if (caixa != null) { caixa.setStock(caixa.getStock() - p.getNumeroCaixas()); consumivelRepo.save(caixa); }
        if (etiqueta != null) { etiqueta.setStock(etiqueta.getStock() - p.getNumeroEtiquetas()); consumivelRepo.save(etiqueta); }

        VinhoEngarrafado veg = engarrafadoRepo.findById(p.getEngarrafado().getId())
                .orElseThrow(() -> new RotulagemException("Vinho engarrafado não encontrado."));
        veg.setRotulado(true);
        engarrafadoRepo.save(veg);

        // Ao rotular, as garrafas SAEM do contentor e ficam em caixas. Tira-se
        // do(s) contentor(es) desse vinho, no local escolhido.
        p.setSaidaContentores(retirarDosContentores(p, veg, garrafas));

        // As garrafas rotuladas ficam em caixas, no mesmo local. E' daqui que
        // o comercial as vai entregar.
        StockRotulado stock = stockDoVinhoNoLocal(veg, p.getLocalRef(), true);
        stock.setGarrafas(stock.getGarrafas() + garrafas);
        stock.setCaixas(stock.getCaixas() + p.getCaixasRotuladas());
        stock.setGarrafasPorCaixa(p.getGarrafasPorCaixa());
        stockRepo.save(stock);

        // O bag-in-box nao vai para caixas: fica onde esta, so' marcado.
        for (ContentorBagInBox c : bibRepo.findByVinhoEmbaladoIdOrderByNomeAsc(veg.getId())) {
            c.setRotulado(true);
            bibRepo.save(c);
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

        // Tira as garrafas do stock rotulado (voltam ao contentor).
        if (p.getEngarrafado() != null) {
            StockRotulado stock = stockDoVinhoNoLocal(p.getEngarrafado(), p.getLocalRef(), false);
            if (stock != null) {
                stock.setGarrafas(Math.max(0, stock.getGarrafas() - p.getNumeroGarrafas()));
                stock.setCaixas(Math.max(0, stock.getCaixas() - p.getCaixasRotuladas()));
                if (stock.getGarrafas() == 0) stockRepo.delete(stock); else stockRepo.save(stock);
            }
        }

        // Devolve as garrafas aos contentores de onde sairam.
        for (Map.Entry<Long, Integer> e : parseSaida(p.getSaidaContentores()).entrySet()) {
            ContentorGarrafas c = contentorRepo.findById(e.getKey()).orElse(null);
            if (c == null) continue;
            c.setGarrafasAtuais(c.getGarrafasAtuais() + e.getValue());
            if (p.getEngarrafado() != null) {
                c.setVinhoEngarrafadoId(p.getEngarrafado().getId());
                if (c.getVinhoNome() == null) c.setVinhoNome(p.getEngarrafado().getNome());
            }
            c.setRotulado(false);
            contentorRepo.save(c);
        }
        p.setSaidaContentores(null);

        repor(p.getRotulo(), p.getNumeroRotulos());
        repor(p.getCapsula(), p.getNumeroCapsulas());
        repor(p.getCaixa(), p.getNumeroCaixas());
        repor(p.getEtiqueta(), p.getNumeroEtiquetas());

        if (p.getEngarrafado() != null) {
            VinhoEngarrafado veg = engarrafadoRepo.findById(p.getEngarrafado().getId()).orElse(null);
            if (veg != null) {
                veg.setRotulado(false);
                engarrafadoRepo.save(veg);
                for (ContentorGarrafas c : contentorRepo.findByVinhoEngarrafadoId(veg.getId())) {
                    c.setRotulado(false);
                    contentorRepo.save(c);
                }
                for (ContentorBagInBox c : bibRepo.findByVinhoEmbaladoIdOrderByNomeAsc(veg.getId())) {
                    c.setRotulado(false);
                    bibRepo.save(c);
                }
            }
        }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }

    /**
     * Tira as garrafas rotuladas dos contentores desse vinho, no local
     * escolhido, e devolve o que saiu de cada um ("id:qtd;id:qtd").
     */
    private String retirarDosContentores(ProcessoRotulagem p, VinhoEngarrafado veg, int garrafas) {
        String local = p.getLocalRef();
        StringJoiner csv = new StringJoiner(";");
        int porTirar = garrafas;
        for (ContentorGarrafas c : contentorRepo.findByVinhoEngarrafadoIdOrderByNomeAsc(veg.getId())) {
            if (porTirar <= 0) break;
            if (c.getGarrafasAtuais() <= 0) continue;
            if (local != null && !local.isEmpty() && !local.equals(localDe(c))) continue;
            int tira = Math.min(porTirar, c.getGarrafasAtuais());
            c.setGarrafasAtuais(c.getGarrafasAtuais() - tira);
            if (c.getGarrafasAtuais() == 0) {
                c.setVinhoEngarrafadoId(null);
                c.setVinhoNome(null);
                c.setRotulado(false);
            }
            contentorRepo.save(c);
            csv.add(c.getId() + ":" + tira);
            porTirar -= tira;
        }
        if (porTirar > 0) {
            throw new RotulagemException(String.format(
                    "Só há %d garrafa(s) deste vinho %s — não dá para rotular %d (%d caixas de %d).",
                    garrafas - porTirar,
                    (local == null || local.isEmpty() ? "nos contentores" : "no local escolhido"),
                    garrafas, p.getCaixasRotuladas(), p.getGarrafasPorCaixa()));
        }
        return csv.toString();
    }

    /** Linha de stock rotulado desse vinho nesse local (cria se for preciso). */
    private StockRotulado stockDoVinhoNoLocal(VinhoEngarrafado veg, String localRef, boolean criar) {
        for (StockRotulado s : stockRepo.findByVinhoEngarrafadoId(veg.getId())) {
            if (s.getLocalRef().equals(localRef)) return s;
        }
        if (!criar) return null;
        StockRotulado s = new StockRotulado();
        s.setVinhoEngarrafadoId(veg.getId());
        s.setVinhoNome(veg.getNome());
        if (localRef != null && localRef.contains(":")) {
            String[] partes = localRef.split(":", 2);
            Long id;
            try { id = Long.valueOf(partes[1].trim()); } catch (Exception e) { id = null; }
            if (id != null) {
                if ("ADEGA".equals(partes[0])) adegaRepo.findById(id).ifPresent(s::setAdega);
                else if ("ARMAZEM".equals(partes[0])) armazemRepo.findById(id).ifPresent(s::setArmazem);
            }
        }
        return s;
    }

    private String localDe(ContentorGarrafas c) {
        if (c.getArmazem() != null) return "ARMAZEM:" + c.getArmazem().getId();
        if (c.getAdega() != null) return "ADEGA:" + c.getAdega().getId();
        return "";
    }

    /** Interpreta "id:qtd;id:qtd". */
    private Map<Long, Integer> parseSaida(String csv) {
        Map<Long, Integer> out = new LinkedHashMap<>();
        if (csv == null || csv.isBlank()) return out;
        for (String par : csv.split(";")) {
            String[] kv = par.split(":");
            if (kv.length != 2) continue;
            try { out.merge(Long.valueOf(kv[0].trim()), Integer.parseInt(kv[1].trim()), Integer::sum); }
            catch (Exception ignored) { }
        }
        return out;
    }

    private Consumivel carregar(Long id, String nome) {
        return consumivelRepo.findById(id)
                .orElseThrow(() -> new RotulagemException(nome + " não encontrado(a)."));
    }

    private void exigirStock(Consumivel c, int qtd) {
        if (c.getStock() <= 0) {
            throw new RotulagemException(String.format(
                    "Está sem stock de %s (%s · %s). Reponha o stock na ficha de consumíveis.",
                    c.getDescricao(), c.getCodigo(), c.getTipo().getDescricao()));
        }
        if (c.getStock() < qtd) {
            throw new RotulagemException(String.format(
                    "Não há %s (%s) que chegue: tem %d, precisa de %d — faltam %d.",
                    c.getDescricao(), c.getCodigo(), c.getStock(), qtd, qtd - c.getStock()));
        }
    }

    private void repor(Consumivel ref, int qtd) {
        if (ref == null) return;
        Consumivel c = consumivelRepo.findById(ref.getId()).orElse(null);
        if (c != null) { c.setStock(c.getStock() + qtd); consumivelRepo.save(c); }
    }
}
