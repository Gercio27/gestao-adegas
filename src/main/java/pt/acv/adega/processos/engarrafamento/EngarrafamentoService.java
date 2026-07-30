package pt.acv.adega.processos.engarrafamento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.*;
import pt.acv.adega.movimentos.MovimentoStockService;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras do Engarrafamento (Fase 6.3). Ao fechar: valida o vinho a granel e o
 * stock de garrafas/rolhas, da baixa de tudo (vinho, garrafas, rolhas e volume
 * do recipiente) e cria a ficha de vinho engarrafado. Reversivel. Transacional.
 */
@Service
public class EngarrafamentoService {

    private final ProcessoEngarrafamentoRepository repo;
    private final MostoRepository mostoRepo;
    private final ConsumivelRepository consumivelRepo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final ContentorGarrafasRepository contentorRepo;
    private final ContentorBagInBoxRepository bibRepo;
    private final CodigoService codigoService;
    private final MovimentoStockService movimentos;

    public EngarrafamentoService(ProcessoEngarrafamentoRepository repo, MostoRepository mostoRepo,
                                 ConsumivelRepository consumivelRepo, TalhaRepository talhaRepo,
                                 DepositoRepository depositoRepo, VinhoEngarrafadoRepository engarrafadoRepo,
                                 ContentorGarrafasRepository contentorRepo, ContentorBagInBoxRepository bibRepo,
                                 CodigoService codigoService, MovimentoStockService movimentos) {
        this.movimentos = movimentos;
        this.repo = repo;
        this.mostoRepo = mostoRepo;
        this.consumivelRepo = consumivelRepo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.contentorRepo = contentorRepo;
        this.bibRepo = bibRepo;
        this.codigoService = codigoService;
    }

    @Transactional
    public void fechar(Long id) {
        fechar(id, false);
    }

    @Transactional
    public void fechar(Long id, boolean forcar) {
        ProcessoEngarrafamento p = repo.findById(id)
                .orElseThrow(() -> new EngarrafamentoException("Engarrafamento não encontrado."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new EngarrafamentoException("O engarrafamento já está fechado.");

        boolean bib = p.isBagInBox();
        if (p.getNumeroGarrafas() <= 0) {
            throw new EngarrafamentoException("Indique o número de " + (bib ? "unidades" : "garrafas") + " (> 0).");
        }
        if (p.getVinhoGranel() == null) throw new EngarrafamentoException("Indique o vinho a granel a utilizar.");
        if (bib && p.getTipoBag() == null) throw new EngarrafamentoException("Indique o formato do bag-in-box (3, 5, 10 ou 20 L).");
        if (p.getGarrafa() == null) {
            throw new EngarrafamentoException("Indique " + (bib ? "as bag-in-box" : "as garrafas") + " a utilizar.");
        }
        // No bag-in-box os litros são exatamente unidades × litros do formato.
        if (bib) p.setLitrosUtilizados(p.getLitrosDasUnidades());

        Mosto vinho = mostoRepo.findById(p.getVinhoGranel().getId())
                .orElseThrow(() -> new EngarrafamentoException("Vinho a granel não encontrado."));
        if (vinho.getEstado() != EstadoMosto.VINHO_GRANEL) {
            throw new EngarrafamentoException("O produto selecionado não é vinho pronto a granel.");
        }
        BigDecimal litros = p.getLitrosUtilizados();
        if (litros == null || litros.signum() <= 0) throw new EngarrafamentoException("Indique os litros de vinho a utilizar (> 0).");
        if (v(vinho.getLitros()).compareTo(litros) < 0) {
            throw new EngarrafamentoException(String.format(
                    "Vinho a granel %s só tem %s L — não chega para %s L.",
                    vinho.getCodigo(), v(vinho.getLitros()).toPlainString(), litros.toPlainString()));
        }

        Consumivel garrafa = consumivelRepo.findById(p.getGarrafa().getId())
                .orElseThrow(() -> new EngarrafamentoException("Garrafa não encontrada."));
        exigirStock(garrafa, p.getNumeroGarrafas());
        Consumivel rolha = null;
        if (p.getRolha() != null) {
            rolha = consumivelRepo.findById(p.getRolha().getId())
                    .orElseThrow(() -> new EngarrafamentoException("Rolha não encontrada."));
            exigirStock(rolha, p.getNumeroRolhas());
        }

        // Baixas
        vinho.setLitros(v(vinho.getLitros()).subtract(litros));
        mostoRepo.save(vinho);
        String proc = "Engarrafamento " + p.getCodigo();
        // liberta volume da talha/deposito
        ajustarRecipiente(vinho, litros.negate(), proc,
                (bib ? "Vinho usado no enchimento de bag-in-box" : "Vinho usado no engarrafamento"));
        garrafa.setStock(garrafa.getStock() - p.getNumeroGarrafas());
        consumivelRepo.save(garrafa);
        movimentos.consumivel(garrafa, -p.getNumeroGarrafas(), proc,
                (bib ? "Bag-in-box usadas no enchimento" : "Garrafas usadas no engarrafamento"));
        if (rolha != null) {
            rolha.setStock(rolha.getStock() - p.getNumeroRolhas());
            consumivelRepo.save(rolha);
            movimentos.consumivel(rolha, -p.getNumeroRolhas(), proc, "Rolhas usadas no engarrafamento");
        }

        // Criar produto acabado
        VinhoEngarrafado veg = new VinhoEngarrafado();
        veg.setCodigo(codigoService.proximoCodigo(VinhoEngarrafado.PREFIXO));
        veg.setNome(p.getNomeVinho() != null && !p.getNomeVinho().isBlank()
                ? p.getNomeVinho() : ("Vinho " + vinho.getCodigo()));
        veg.setNumeroGarrafas(p.getNumeroGarrafas());
        // Bag-in-box: a capacidade da unidade vem do formato, não da ficha da garrafa.
        veg.setCapacidadeMl(bib
                ? p.getTipoBag().getLitrosPorUnidade().multiply(BigDecimal.valueOf(1000)).intValue()
                : garrafa.getCapacidadeMl());
        veg.setLote(p.getLote());
        veg.setCasta(vinho.getCasta());
        veg.setOrigemDescricao("Engarrafamento " + p.getCodigo() + " · Vinho a granel " + vinho.getCodigo());
        veg.setOrigemEngarrafamentoId(p.getId());
        veg.setDataProducao(LocalDateTime.now());
        engarrafadoRepo.save(veg);

        // Colocar as garrafas nos contentores indicados (se houver distribuicao).
        // A capacidade PODE ser excedida — mas só depois de o utilizador confirmar
        // (forcar=true). Sem confirmação, avisa e não fecha.
        Map<Long, Integer> distribuicao = parseDistribuicao(p.getDistribuicaoContentores());
        if (!forcar) {
            List<String> avisos = new ArrayList<>();
            for (Map.Entry<Long, Integer> e : distribuicao.entrySet()) {
                if (bib) {
                    ContentorBagInBox c = bibRepo.findById(e.getKey()).orElse(null);
                    if (c == null) continue;
                    int novoTotal = c.getUnidadesAtuais() + e.getValue();
                    if (c.getCapacidadeUnidades() > 0 && novoTotal > c.getCapacidadeUnidades()) {
                        avisos.add(String.format("%s excede a capacidade em %d unidade(s) (máx. %d, ficaria com %d)",
                                c.getNome(), novoTotal - c.getCapacidadeUnidades(), c.getCapacidadeUnidades(), novoTotal));
                    }
                } else {
                    ContentorGarrafas c = contentorRepo.findById(e.getKey()).orElse(null);
                    if (c == null) continue;
                    int novoTotal = c.getGarrafasAtuais() + e.getValue();
                    if (c.getCapacidadeGarrafas() > 0 && novoTotal > c.getCapacidadeGarrafas()) {
                        avisos.add(String.format("%s excede o máximo em %d garrafa(s) (máx. %d, ficaria com %d)",
                                c.getNome(), novoTotal - c.getCapacidadeGarrafas(), c.getCapacidadeGarrafas(), novoTotal));
                    }
                }
            }
            if (!avisos.isEmpty()) {
                throw new EngarrafamentoException(
                        "Capacidade excedida: " + String.join("; ", avisos) + ". Pode fechar mesmo assim.", true);
            }
        }
        for (Map.Entry<Long, Integer> e : distribuicao.entrySet()) {
            if (bib) {
                ContentorBagInBox c = bibRepo.findById(e.getKey()).orElse(null);
                if (c == null) continue;
                c.setUnidadesAtuais(c.getUnidadesAtuais() + e.getValue());
                c.setVinhoEmbaladoId(veg.getId());
                c.setVinhoNome(veg.getNome());
                c.setRotulado(false);   // fica por rotular até à Fase 7
                bibRepo.save(c);
                movimentos.palete(c, e.getValue(), "Engarrafamento " + p.getCodigo(),
                        "Bag-in-box cheias arrumadas na palete", veg.getNome());
            } else {
                ContentorGarrafas c = contentorRepo.findById(e.getKey()).orElse(null);
                if (c == null) continue;
                c.setGarrafasAtuais(c.getGarrafasAtuais() + e.getValue());
                c.setVinhoEngarrafadoId(veg.getId());
                c.setVinhoNome(veg.getNome());
                c.setRotulado(false);
                contentorRepo.save(c);
                movimentos.contentor(c, e.getValue(), "Engarrafamento " + p.getCodigo(),
                        "Garrafas cheias arrumadas no contentor", veg.getNome());
            }
        }

        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
    }

    @Transactional
    public void reabrir(Long id) {
        ProcessoEngarrafamento p = repo.findById(id)
                .orElseThrow(() -> new EngarrafamentoException("Engarrafamento não encontrado."));
        if (p.getEstado() == EstadoProcesso.ABERTO) return;

        // Retirar as unidades dos contentores onde foram colocadas
        for (Map.Entry<Long, Integer> e : parseDistribuicao(p.getDistribuicaoContentores()).entrySet()) {
            if (p.isBagInBox()) {
                ContentorBagInBox c = bibRepo.findById(e.getKey()).orElse(null);
                if (c == null) continue;
                c.setUnidadesAtuais(Math.max(0, c.getUnidadesAtuais() - e.getValue()));
                if (c.getUnidadesAtuais() == 0) {
                    c.setVinhoEmbaladoId(null);
                    c.setVinhoNome(null);
                    c.setRotulado(false);
                }
                bibRepo.save(c);
                movimentos.palete(c, -e.getValue(), "Engarrafamento " + p.getCodigo(),
                        "Engarrafamento reaberto — unidades retiradas", p.getNomeVinho());
            } else {
                ContentorGarrafas c = contentorRepo.findById(e.getKey()).orElse(null);
                if (c == null) continue;
                String vinhoNome = c.getVinhoNome();
                c.setGarrafasAtuais(Math.max(0, c.getGarrafasAtuais() - e.getValue()));
                if (c.getGarrafasAtuais() == 0) {
                    c.setVinhoEngarrafadoId(null);
                    c.setVinhoNome(null);
                    c.setRotulado(false);
                }
                contentorRepo.save(c);
                movimentos.contentor(c, -e.getValue(), "Engarrafamento " + p.getCodigo(),
                        "Engarrafamento reaberto — garrafas retiradas", vinhoNome);
            }
        }

        // Apagar produtos acabados gerados
        engarrafadoRepo.findByOrigemEngarrafamentoId(p.getId()).forEach(engarrafadoRepo::delete);

        // Repor vinho a granel e volume do recipiente
        BigDecimal litros = v(p.getLitrosUtilizados());
        if (p.getVinhoGranel() != null) {
            Mosto vinho = mostoRepo.findById(p.getVinhoGranel().getId()).orElse(null);
            if (vinho != null) {
                vinho.setLitros(v(vinho.getLitros()).add(litros));
                mostoRepo.save(vinho);
                ajustarRecipiente(vinho, litros, "Engarrafamento " + p.getCodigo(),
                        "Engarrafamento reaberto — vinho devolvido");
            }
        }
        // Repor stocks
        if (p.getGarrafa() != null) {
            Consumivel g = consumivelRepo.findById(p.getGarrafa().getId()).orElse(null);
            if (g != null) {
                g.setStock(g.getStock() + p.getNumeroGarrafas()); consumivelRepo.save(g);
                movimentos.consumivel(g, p.getNumeroGarrafas(), "Engarrafamento " + p.getCodigo(),
                        "Engarrafamento reaberto — devolvido ao stock");
            }
        }
        if (p.getRolha() != null) {
            Consumivel r = consumivelRepo.findById(p.getRolha().getId()).orElse(null);
            if (r != null) {
                r.setStock(r.getStock() + p.getNumeroRolhas()); consumivelRepo.save(r);
                movimentos.consumivel(r, p.getNumeroRolhas(), "Engarrafamento " + p.getCodigo(),
                        "Engarrafamento reaberto — devolvido ao stock");
            }
        }

        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }

    /** Soma 'delta' litros ao recipiente (talha/deposito) onde o vinho esta. */
    private void ajustarRecipiente(Mosto vinho, BigDecimal delta, String origem, String descricao) {
        if (vinho.getTalha() != null) {
            Talha t = talhaRepo.findById(vinho.getTalha().getId()).orElse(null);
            if (t != null) {
                t.setVolumeAtualLitros(naoNegativo(v(t.getVolumeAtualLitros()).add(delta))); talhaRepo.save(t);
                movimentos.talha(t, delta, origem, descricao, vinho.getVinhoNome());
            }
        } else if (vinho.getDeposito() != null) {
            Deposito d = depositoRepo.findById(vinho.getDeposito().getId()).orElse(null);
            if (d != null) {
                d.setVolumeAtualLitros(naoNegativo(v(d.getVolumeAtualLitros()).add(delta))); depositoRepo.save(d);
                movimentos.deposito(d, delta, origem, descricao, vinho.getVinhoNome());
            }
        }
    }

    /** Interpreta a distribuicao "contentorId:qtd;contentorId:qtd" num mapa id->qtd. */
    private Map<Long, Integer> parseDistribuicao(String csv) {
        Map<Long, Integer> out = new LinkedHashMap<>();
        if (csv == null || csv.isBlank()) return out;
        for (String par : csv.split(";")) {
            String[] kv = par.split(":");
            if (kv.length != 2) continue;
            try {
                Long id = Long.valueOf(kv[0].trim());
                int qtd = Integer.parseInt(kv[1].trim());
                if (qtd > 0) out.merge(id, qtd, Integer::sum);
            } catch (Exception ignored) { }
        }
        return out;
    }

    /** Diz claramente se e' falta total de stock ou so' insuficiente. */
    private void exigirStock(Consumivel c, int qtd) {
        if (qtd <= 0) return;
        if (c.getStock() <= 0) {
            throw new EngarrafamentoException(String.format(
                    "Está sem stock de %s (%s · %s). Reponha o stock na ficha de consumíveis.",
                    c.getDescricao(), c.getCodigo(), c.getTipo().getDescricao()));
        }
        if (c.getStock() < qtd) {
            throw new EngarrafamentoException(String.format(
                    "Não há %s (%s) que chegue: tem %d, precisa de %d — faltam %d.",
                    c.getDescricao(), c.getCodigo(), c.getStock(), qtd, qtd - c.getStock()));
        }
    }

    private BigDecimal v(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    private BigDecimal naoNegativo(BigDecimal x) { return x.signum() < 0 ? BigDecimal.ZERO : x; }
}
