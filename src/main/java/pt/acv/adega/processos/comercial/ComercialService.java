package pt.acv.adega.processos.comercial;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.ContentorService;
import pt.acv.adega.fichas.TipoEmbalagem;
import pt.acv.adega.processos.EstadoProcesso;
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
    private final CodigoService codigoService;

    public ComercialService(ProcessoPassagemComercialRepository repo,
                            VinhoEngarrafadoRepository engarrafadoRepo, ContentorService contentorService,
                            CodigoService codigoService) {
        this.repo = repo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.contentorService = contentorService;
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

        // Preferencialmente a entrega sai de um contentor rotulado (define vinho + local).
        ContentorService.Opcao c = contentorService.procurar(tipo, p.getContentorId());
        if (c != null) {
            if (!c.rotulado()) throw new ComercialException("O contentor " + c.nome() + " ainda não está rotulado (Fase 7).");
            if (p.getQuantidadeGarrafas() > c.stock()) {
                throw new ComercialException(String.format(
                        "%s só tem %d %s — não pode entregar %d.",
                        c.nome(), c.stock(), tipo.getUnidade(), p.getQuantidadeGarrafas()));
            }
            contentorService.ajustar(tipo, p.getContentorId(), -p.getQuantidadeGarrafas());
        }

        if (p.getEngarrafado() == null) throw new ComercialException("Indique o vinho a entregar.");
        VinhoEngarrafado veg = engarrafadoRepo.findById(p.getEngarrafado().getId())
                .orElseThrow(() -> new ComercialException("Vinho engarrafado não encontrado."));
        if (!veg.isRotulado()) {
            throw new ComercialException("O vinho " + veg.getCodigo() + " ainda não está rotulado (Fase 7).");
        }
        // Sem contentor (registos antigos): valida contra o disponivel do produto acabado.
        if (c == null && p.getQuantidadeGarrafas() > veg.getDisponiveis()) {
            throw new ComercialException(String.format(
                    "%s só tem %d garrafas disponíveis — não pode entregar %d.",
                    veg.getCodigo(), veg.getDisponiveis(), p.getQuantidadeGarrafas()));
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
        // Repor as unidades no contentor de origem.
        TipoEmbalagem tipo = p.getTipoEmbalagem() != null ? p.getTipoEmbalagem() : TipoEmbalagem.GARRAFA;
        contentorService.ajustar(tipo, p.getContentorId(), p.getQuantidadeGarrafas());
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }
}
