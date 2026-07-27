package pt.acv.adega.processos.atesto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.DepositoRepository;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.fichas.TalhaRepository;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private final MostoRepository mostoRepo;
    private final CodigoService codigoService;

    public AtestoService(ProcessoAtestoRepository repo, TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                         MostoRepository mostoRepo, CodigoService codigoService) {
        this.repo = repo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.mostoRepo = mostoRepo;
        this.codigoService = codigoService;
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

        // Aplicar movimento aos volumes dos recipientes
        if (origemTalha) { tOrig.setVolumeAtualLitros(volOrigem.subtract(litros)); talhaRepo.save(tOrig); }
        else { dOrig.setVolumeAtualLitros(volOrigem.subtract(litros)); depositoRepo.save(dOrig); }
        if (destinoTalha) { tDest.setVolumeAtualLitros(volDestino.add(litros)); talhaRepo.save(tDest); }
        else { dDest.setVolumeAtualLitros(volDestino.add(litros)); depositoRepo.save(dDest); }

        // ... e ao vinho em si, senão o recipiente de destino ficava com litros
        // mas sem se saber o que lá está dentro.
        moverVinho(a, tOrig, dOrig, tDest, dDest, litros);

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
        reverterVinho(a, litros);
        a.setEstado(EstadoProcesso.ABERTO);
        a.setDataFecho(null);
        repo.save(a);
    }

    /**
     * Passa os litros de vinho do recipiente de origem para o de destino: tira
     * de uma ficha de mosto que la' esteja e junta a uma do mesmo vinho no
     * destino (ou cria uma nova). E' isto que faz o destino deixar de estar so'
     * "ocupado" e passar a dizer o que tem la' dentro.
     */
    private void moverVinho(ProcessoAtesto a, Talha tOrig, Deposito dOrig, Talha tDest, Deposito dDest, BigDecimal litros) {
        List<Mosto> naOrigem = tOrig != null ? mostoRepo.findByTalhaId(tOrig.getId()) : mostoRepo.findByDepositoId(dOrig.getId());
        // Prefere a ficha que tenha litros que cheguem; senao, a que tiver mais.
        Mosto origem = null;
        for (Mosto m : naOrigem) {
            if (vol(m.getLitros()).compareTo(litros) >= 0) { origem = m; break; }
            if (origem == null || vol(m.getLitros()).compareTo(vol(origem.getLitros())) > 0) origem = m;
        }
        if (origem == null) return;   // recipiente sem ficha de mosto (ex.: dados antigos)

        BigDecimal aMover = vol(origem.getLitros()).min(litros);
        origem.setLitros(naoNegativo(vol(origem.getLitros()).subtract(aMover)));
        mostoRepo.save(origem);

        String nome = origem.getVinhoNome();
        List<Mosto> noDestino = tDest != null ? mostoRepo.findByTalhaId(tDest.getId()) : mostoRepo.findByDepositoId(dDest.getId());
        Mosto destino = null;
        for (Mosto m : noDestino) {
            if (m.getId() != null && m.getId().equals(origem.getId())) continue;
            boolean mesmoVinho = nome != null ? nome.equals(m.getVinhoNome())
                    : (m.getVinhoNome() == null && m.getEstado() == origem.getEstado());
            if (mesmoVinho) { destino = m; break; }
        }
        boolean criado = false;
        if (destino == null) {
            destino = new Mosto();
            destino.setCodigo(codigoService.proximoCodigo(Mosto.PREFIXO));
            destino.setLitros(BigDecimal.ZERO);
            destino.setEstado(origem.getEstado());
            destino.setVinhoNome(nome);
            destino.setCasta(origem.getCasta());
            destino.setCastas(new java.util.ArrayList<>(origem.getCastas()));
            destino.setAlcoolProvavel(origem.getAlcoolProvavel());
            destino.setOrigemMoagemId(origem.getOrigemMoagemId());
            destino.setOrigemDescricao("Atesto " + a.getCodigo() + " de " + origem.getCodigo());
            destino.setDataProducao(LocalDateTime.now());
            if (tDest != null) destino.setTalha(tDest); else destino.setDeposito(dDest);
            criado = true;
        }
        destino.setLitros(vol(destino.getLitros()).add(aMover));
        mostoRepo.save(destino);

        a.setMostoOrigemId(origem.getId());
        a.setMostoDestinoId(destino.getId());
        a.setDestinoCriado(criado);
    }

    /** Desfaz o que o moverVinho fez. */
    private void reverterVinho(ProcessoAtesto a, BigDecimal litros) {
        Mosto destino = a.getMostoDestinoId() != null ? mostoRepo.findById(a.getMostoDestinoId()).orElse(null) : null;
        BigDecimal devolvido = BigDecimal.ZERO;
        if (destino != null) {
            devolvido = vol(destino.getLitros()).min(litros);
            destino.setLitros(naoNegativo(vol(destino.getLitros()).subtract(devolvido)));
            if (a.isDestinoCriado() && vol(destino.getLitros()).signum() <= 0) mostoRepo.delete(destino);
            else mostoRepo.save(destino);
        }
        Mosto origem = a.getMostoOrigemId() != null ? mostoRepo.findById(a.getMostoOrigemId()).orElse(null) : null;
        if (origem != null) {
            origem.setLitros(vol(origem.getLitros()).add(devolvido));
            mostoRepo.save(origem);
        }
        a.setMostoOrigemId(null);
        a.setMostoDestinoId(null);
        a.setDestinoCriado(false);
    }

    private BigDecimal vol(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private BigDecimal naoNegativo(BigDecimal v) { return v.signum() < 0 ? BigDecimal.ZERO : v; }
}
