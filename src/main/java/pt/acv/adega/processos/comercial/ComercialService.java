package pt.acv.adega.processos.comercial;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.common.CodigoService;
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
    private final CodigoService codigoService;

    public ComercialService(ProcessoPassagemComercialRepository repo,
                            VinhoEngarrafadoRepository engarrafadoRepo, CodigoService codigoService) {
        this.repo = repo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.codigoService = codigoService;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoPassagemComercial p = repo.findById(id)
                .orElseThrow(() -> new ComercialException("Processo não encontrado."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new ComercialException("O processo já está fechado.");
        if (p.getEngarrafado() == null) throw new ComercialException("Indique o vinho engarrafado a entregar.");
        if (p.getQuantidadeGarrafas() <= 0) throw new ComercialException("Indique a quantidade de garrafas (> 0).");

        VinhoEngarrafado veg = engarrafadoRepo.findById(p.getEngarrafado().getId())
                .orElseThrow(() -> new ComercialException("Vinho engarrafado não encontrado."));
        if (!veg.isRotulado()) {
            throw new ComercialException("O vinho " + veg.getCodigo() + " ainda não está rotulado (Fase 7).");
        }
        if (p.getQuantidadeGarrafas() > veg.getDisponiveis()) {
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
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }
}
