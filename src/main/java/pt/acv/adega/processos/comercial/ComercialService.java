package pt.acv.adega.processos.comercial;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.ContentorService;
import pt.acv.adega.fichas.TipoEmbalagem;
import pt.acv.adega.movimentos.MovimentoStockService;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.StockRotulado;
import pt.acv.adega.produtos.StockRotuladoRepository;
import pt.acv.adega.produtos.VinhoEngarrafado;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;

/**
 * Regras da passagem ao comercial (Fase 8). Ao fechar valida o stock disponivel
 * do produto acabado, da baixa das garrafas entregues e emite o numero da nota
 * de entrega. Reversivel. Transacional.
 */
@Service
public class ComercialService {

    public static final String PREFIXO_NOTA = "NE";

    private final ProcessoPassagemComercialRepository repo;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final ContentorService contentorService;
    private final StockRotuladoRepository stockRepo;
    private final CodigoService codigoService;
    private final MovimentoStockService movimentos;

    public ComercialService(ProcessoPassagemComercialRepository repo,
                            VinhoEngarrafadoRepository engarrafadoRepo, ContentorService contentorService,
                            StockRotuladoRepository stockRepo,
                            CodigoService codigoService, MovimentoStockService movimentos) {
        this.movimentos = movimentos;
        this.repo = repo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.contentorService = contentorService;
        this.stockRepo = stockRepo;
        this.codigoService = codigoService;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoPassagemComercial p = repo.findById(id)
                .orElseThrow(() -> new ComercialException("Processo não encontrado."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new ComercialException("O processo já está fechado.");
        TipoEmbalagem tipo = p.getTipoEmbalagem() != null ? p.getTipoEmbalagem() : TipoEmbalagem.GARRAFA;
        if (p.getQuantidadeGarrafas() <= 0) {
            throw new ComercialException("Indique a quantidade de " + tipo.getUnidade() + " (> 0).");
        }

        if (tipo == TipoEmbalagem.GARRAFA) {
            // As garrafas rotuladas estao em caixas, no local — nao no contentor.
            StockRotulado s = p.getStockRotuladoId() != null
                    ? stockRepo.findById(p.getStockRotuladoId()).orElse(null) : null;
            if (s == null) throw new ComercialException("Indique de que stock rotulado saem as garrafas.");
            if (p.getQuantidadeGarrafas() > s.getGarrafas()) {
                throw new ComercialException(String.format(
                        "%s só tem %d garrafa(s) rotuladas — não pode entregar %d.",
                        s.getDescricao(), s.getGarrafas(), p.getQuantidadeGarrafas()));
            }
            s.setGarrafas(s.getGarrafas() - p.getQuantidadeGarrafas());
            s.setCaixas(s.getCaixasInteiras());
            if (s.getGarrafas() == 0) stockRepo.delete(s); else stockRepo.save(s);
            movimentos.rotulado(s, -p.getQuantidadeGarrafas(), "Entrega ao comercial " + p.getCodigo(),
                    "Entrega a " + (p.getDestinatario() != null ? p.getDestinatario() : "cliente"));
        } else {
            // Bag-in-box continua a sair da palete onde esta.
            ContentorService.Opcao c = contentorService.procurar(tipo, p.getContentorId());
            if (c == null) throw new ComercialException("Indique a palete de onde saem as unidades.");
            if (!c.rotulado()) throw new ComercialException("A palete " + c.nome() + " ainda não está rotulada (Fase 7).");
            if (p.getQuantidadeGarrafas() > c.stock()) {
                throw new ComercialException(String.format(
                        "%s só tem %d %s — não pode entregar %d.",
                        c.nome(), c.stock(), tipo.getUnidade(), p.getQuantidadeGarrafas()));
            }
            contentorService.ajustar(tipo, p.getContentorId(), -p.getQuantidadeGarrafas(),
                    "Entrega ao comercial " + p.getCodigo(),
                    "Entrega a " + (p.getDestinatario() != null ? p.getDestinatario() : "cliente"));
        }

        if (p.getEngarrafado() == null) throw new ComercialException("Indique o vinho a entregar.");
        VinhoEngarrafado veg = engarrafadoRepo.findById(p.getEngarrafado().getId())
                .orElseThrow(() -> new ComercialException("Vinho engarrafado não encontrado."));
        if (!veg.isRotulado()) {
            throw new ComercialException("O vinho " + veg.getCodigo() + " ainda não está rotulado (Fase 7).");
        }
        veg.setGarrafasEntregues(veg.getGarrafasEntregues() + p.getQuantidadeGarrafas());
        engarrafadoRepo.save(veg);

        if (p.getNumeroNota() == null) {
            p.setNumeroNota(codigoService.proximoCodigo(PREFIXO_NOTA));
        }
        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
    }

    @Transactional
    public void reabrir(Long id) {
        ProcessoPassagemComercial p = repo.findById(id)
                .orElseThrow(() -> new ComercialException("Processo não encontrado."));
        if (p.getEstado() == EstadoProcesso.ABERTO) return;
        if (p.getEngarrafado() != null) {
            VinhoEngarrafado veg = engarrafadoRepo.findById(p.getEngarrafado().getId()).orElse(null);
            if (veg != null) {
                int reposto = Math.max(0, veg.getGarrafasEntregues() - p.getQuantidadeGarrafas());
                veg.setGarrafasEntregues(reposto);
                engarrafadoRepo.save(veg);
            }
        }
        // Repor: nas garrafas volta ao stock rotulado, no bag-in-box a' palete.
        TipoEmbalagem tipo = p.getTipoEmbalagem() != null ? p.getTipoEmbalagem() : TipoEmbalagem.GARRAFA;
        if (tipo == TipoEmbalagem.GARRAFA) {
            if (p.getStockRotuladoId() != null) {
                StockRotulado s = stockRepo.findById(p.getStockRotuladoId()).orElse(null);
                if (s != null) {
                    s.setGarrafas(s.getGarrafas() + p.getQuantidadeGarrafas());
                    s.setCaixas(s.getCaixasInteiras());
                    stockRepo.save(s);
                    movimentos.rotulado(s, p.getQuantidadeGarrafas(), "Entrega ao comercial " + p.getCodigo(),
                            "Entrega reaberta — garrafas devolvidas ao stock");
                }
            }
        } else {
            contentorService.ajustar(tipo, p.getContentorId(), p.getQuantidadeGarrafas(),
                    "Entrega ao comercial " + p.getCodigo(), "Entrega reaberta — unidades devolvidas");
        }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }
}
