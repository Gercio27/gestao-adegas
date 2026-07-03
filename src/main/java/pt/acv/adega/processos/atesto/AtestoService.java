package pt.acv.adega.processos.atesto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.DepositoRepository;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.fichas.TalhaRepository;
import pt.acv.adega.processos.EstadoProcesso;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Regras de negocio do Atesto (Fase 4.2): ao fechar, valida e move o volume de
 * um recipiente para outro, com controlo de capacidade nos dois lados. Tudo
 * transacional (tudo-ou-nada).
 */
@Service
public class AtestoService {

    private final ProcessoAtestoRepository repo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;

    public AtestoService(ProcessoAtestoRepository repo, TalhaRepository talhaRepo, DepositoRepository depositoRepo) {
        this.repo = repo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoAtesto a = repo.findById(id).orElseThrow(() -> new AtestoException("Atesto não encontrado."));
        if (a.getEstado() == EstadoProcesso.FECHADO) throw new AtestoException("O atesto já está fechado.");

        BigDecimal litros = a.getLitros();
        if (litros == null || litros.signum() <= 0) throw new AtestoException("Indique os litros a atestar (> 0).");

        boolean origemTalha = a.getTalhaOrigem() != null;
        boolean origemDeposito = a.getDepositoOrigem() != null;
        boolean destinoTalha = a.getTalhaDestino() != null;
        boolean destinoDeposito = a.getDepositoDestino() != null;
        if (!origemTalha && !origemDeposito) throw new AtestoException("Indique o recipiente de origem.");
        if (!destinoTalha && !destinoDeposito) throw new AtestoException("Indique o recipiente de destino.");

        // Impedir origem == destino
        if (origemTalha && destinoTalha && a.getTalhaOrigem().getId().equals(a.getTalhaDestino().getId()))
            throw new AtestoException("A origem e o destino não podem ser o mesmo recipiente.");
        if (origemDeposito && destinoDeposito && a.getDepositoOrigem().getId().equals(a.getDepositoDestino().getId()))
            throw new AtestoException("A origem e o destino não podem ser o mesmo recipiente.");

        // Carregar entidades geridas
        Talha tOrig = origemTalha ? talhaRepo.findById(a.getTalhaOrigem().getId()).orElseThrow() : null;
        Deposito dOrig = origemDeposito ? depositoRepo.findById(a.getDepositoOrigem().getId()).orElseThrow() : null;
        Talha tDest = destinoTalha ? talhaRepo.findById(a.getTalhaDestino().getId()).orElseThrow() : null;
        Deposito dDest = destinoDeposito ? depositoRepo.findById(a.getDepositoDestino().getId()).orElseThrow() : null;

        BigDecimal volOrigem = origemTalha ? vol(tOrig.getVolumeAtualLitros()) : vol(dOrig.getVolumeAtualLitros());
        BigDecimal volDestino = destinoTalha ? vol(tDest.getVolumeAtualLitros()) : vol(dDest.getVolumeAtualLitros());
        BigDecimal capDestino = destinoTalha ? tDest.getCapacidadeLitros() : dDest.getCapacidadeLitros();
        String nomeOrigem = origemTalha ? "Talha " + tOrig.getIdentificacao() : "Depósito " + dOrig.getIdentificacao();
        String nomeDestino = destinoTalha ? "Talha " + tDest.getIdentificacao() : "Depósito " + dDest.getIdentificacao();

        // Controlo da ORIGEM: nao pode dar mais do que tem
        if (volOrigem.compareTo(litros) < 0) {
            throw new AtestoException(String.format(
                    "%s só tem %s L — não pode ceder %s L.", nomeOrigem, volOrigem.toPlainString(), litros.toPlainString()));
        }
        // Controlo do DESTINO: nao pode passar a capacidade
        if (capDestino != null && volDestino.add(litros).compareTo(capDestino) > 0) {
            throw new AtestoException(String.format(
                    "%s: capacidade excedida. Capacidade %s L, tem %s L, tentou juntar %s L (total %s L).",
                    nomeDestino, capDestino.toPlainString(), volDestino.toPlainString(),
                    litros.toPlainString(), volDestino.add(litros).toPlainString()));
        }

        // Aplicar movimento
        if (origemTalha) { tOrig.setVolumeAtualLitros(volOrigem.subtract(litros)); talhaRepo.save(tOrig); }
        else { dOrig.setVolumeAtualLitros(volOrigem.subtract(litros)); depositoRepo.save(dOrig); }
        if (destinoTalha) { tDest.setVolumeAtualLitros(volDestino.add(litros)); talhaRepo.save(tDest); }
        else { dDest.setVolumeAtualLitros(volDestino.add(litros)); depositoRepo.save(dDest); }

        a.setEstado(EstadoProcesso.FECHADO);
        if (a.getDataHoraFim() == null) a.setDataHoraFim(LocalDateTime.now());
        a.setDataFecho(LocalDateTime.now());
        repo.save(a);
    }

    @Transactional
    public void reabrir(Long id) {
        ProcessoAtesto a = repo.findById(id).orElseThrow(() -> new AtestoException("Atesto não encontrado."));
        if (a.getEstado() == EstadoProcesso.ABERTO) return;
        BigDecimal litros = vol(a.getLitros());

        // Reverter: devolver a origem, retirar do destino
        if (a.getTalhaOrigem() != null) {
            Talha t = talhaRepo.findById(a.getTalhaOrigem().getId()).orElseThrow();
            t.setVolumeAtualLitros(vol(t.getVolumeAtualLitros()).add(litros)); talhaRepo.save(t);
        } else if (a.getDepositoOrigem() != null) {
            Deposito d = depositoRepo.findById(a.getDepositoOrigem().getId()).orElseThrow();
            d.setVolumeAtualLitros(vol(d.getVolumeAtualLitros()).add(litros)); depositoRepo.save(d);
        }
        if (a.getTalhaDestino() != null) {
            Talha t = talhaRepo.findById(a.getTalhaDestino().getId()).orElseThrow();
            t.setVolumeAtualLitros(naoNegativo(vol(t.getVolumeAtualLitros()).subtract(litros))); talhaRepo.save(t);
        } else if (a.getDepositoDestino() != null) {
            Deposito d = depositoRepo.findById(a.getDepositoDestino().getId()).orElseThrow();
            d.setVolumeAtualLitros(naoNegativo(vol(d.getVolumeAtualLitros()).subtract(litros))); depositoRepo.save(d);
        }
        a.setEstado(EstadoProcesso.ABERTO);
        a.setDataFecho(null);
        repo.save(a);
    }

    private BigDecimal vol(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private BigDecimal naoNegativo(BigDecimal v) { return v.signum() < 0 ? BigDecimal.ZERO : v; }
}
