package pt.acv.adega.processos.engarrafamento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.*;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private final CodigoService codigoService;

    public EngarrafamentoService(ProcessoEngarrafamentoRepository repo, MostoRepository mostoRepo,
                                 ConsumivelRepository consumivelRepo, TalhaRepository talhaRepo,
                                 DepositoRepository depositoRepo, VinhoEngarrafadoRepository engarrafadoRepo,
                                 CodigoService codigoService) {
        this.repo = repo;
        this.mostoRepo = mostoRepo;
        this.consumivelRepo = consumivelRepo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.codigoService = codigoService;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoEngarrafamento p = repo.findById(id)
                .orElseThrow(() -> new EngarrafamentoException("Engarrafamento não encontrado."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new EngarrafamentoException("O engarrafamento já está fechado.");

        if (p.getNumeroGarrafas() <= 0) throw new EngarrafamentoException("Indique o número de garrafas (> 0).");
        if (p.getVinhoGranel() == null) throw new EngarrafamentoException("Indique o vinho a granel a utilizar.");
        if (p.getGarrafa() == null) throw new EngarrafamentoException("Indique as garrafas a utilizar.");

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
        if (garrafa.getStock() < p.getNumeroGarrafas()) {
            throw new EngarrafamentoException(String.format(
                    "Stock de garrafas insuficiente: %s tem %d, precisa de %d.",
                    garrafa.getCodigo(), garrafa.getStock(), p.getNumeroGarrafas()));
        }
        Consumivel rolha = null;
        if (p.getRolha() != null) {
            rolha = consumivelRepo.findById(p.getRolha().getId())
                    .orElseThrow(() -> new EngarrafamentoException("Rolha não encontrada."));
            if (rolha.getStock() < p.getNumeroRolhas()) {
                throw new EngarrafamentoException(String.format(
                        "Stock de rolhas insuficiente: %s tem %d, precisa de %d.",
                        rolha.getCodigo(), rolha.getStock(), p.getNumeroRolhas()));
            }
        }

        // Baixas
        vinho.setLitros(v(vinho.getLitros()).subtract(litros));
        mostoRepo.save(vinho);
        ajustarRecipiente(vinho, litros.negate());          // liberta volume da talha/deposito
        garrafa.setStock(garrafa.getStock() - p.getNumeroGarrafas());
        consumivelRepo.save(garrafa);
        if (rolha != null) {
            rolha.setStock(rolha.getStock() - p.getNumeroRolhas());
            consumivelRepo.save(rolha);
        }

        // Criar produto acabado
        VinhoEngarrafado veg = new VinhoEngarrafado();
        veg.setCodigo(codigoService.proximoCodigo(VinhoEngarrafado.PREFIXO));
        veg.setNome(p.getNomeVinho() != null && !p.getNomeVinho().isBlank()
                ? p.getNomeVinho() : ("Vinho " + vinho.getCodigo()));
        veg.setNumeroGarrafas(p.getNumeroGarrafas());
        veg.setCapacidadeMl(garrafa.getCapacidadeMl());
        veg.setLote(p.getLote());
        veg.setCasta(vinho.getCasta());
        veg.setOrigemDescricao("Engarrafamento " + p.getCodigo() + " · Vinho a granel " + vinho.getCodigo());
        veg.setOrigemEngarrafamentoId(p.getId());
        veg.setDataProducao(LocalDateTime.now());
        engarrafadoRepo.save(veg);

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

        // Apagar produtos acabados gerados
        engarrafadoRepo.findByOrigemEngarrafamentoId(p.getId()).forEach(engarrafadoRepo::delete);

        // Repor vinho a granel e volume do recipiente
        BigDecimal litros = v(p.getLitrosUtilizados());
        if (p.getVinhoGranel() != null) {
            Mosto vinho = mostoRepo.findById(p.getVinhoGranel().getId()).orElse(null);
            if (vinho != null) {
                vinho.setLitros(v(vinho.getLitros()).add(litros));
                mostoRepo.save(vinho);
                ajustarRecipiente(vinho, litros);
            }
        }
        // Repor stocks
        if (p.getGarrafa() != null) {
            Consumivel g = consumivelRepo.findById(p.getGarrafa().getId()).orElse(null);
            if (g != null) { g.setStock(g.getStock() + p.getNumeroGarrafas()); consumivelRepo.save(g); }
        }
        if (p.getRolha() != null) {
            Consumivel r = consumivelRepo.findById(p.getRolha().getId()).orElse(null);
            if (r != null) { r.setStock(r.getStock() + p.getNumeroRolhas()); consumivelRepo.save(r); }
        }

        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }

    /** Soma 'delta' litros ao recipiente (talha/deposito) onde o vinho esta. */
    private void ajustarRecipiente(Mosto vinho, BigDecimal delta) {
        if (vinho.getTalha() != null) {
            Talha t = talhaRepo.findById(vinho.getTalha().getId()).orElse(null);
            if (t != null) { t.setVolumeAtualLitros(naoNegativo(v(t.getVolumeAtualLitros()).add(delta))); talhaRepo.save(t); }
        } else if (vinho.getDeposito() != null) {
            Deposito d = depositoRepo.findById(vinho.getDeposito().getId()).orElse(null);
            if (d != null) { d.setVolumeAtualLitros(naoNegativo(v(d.getVolumeAtualLitros()).add(delta))); depositoRepo.save(d); }
        }
    }

    private BigDecimal v(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    private BigDecimal naoNegativo(BigDecimal x) { return x.signum() < 0 ? BigDecimal.ZERO : x; }
}
