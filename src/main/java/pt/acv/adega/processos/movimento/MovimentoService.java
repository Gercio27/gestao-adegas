package pt.acv.adega.processos.movimento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.*;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Regras das entradas (4.7) e saidas (4.8) de mosto. Emite o DA ao fechar.
 * ENTRADA cria ficha de mosto (com controlo de capacidade do recipiente).
 * SAIDA da baixa de um mosto existente. Reversivel. Transacional.
 */
@Service
public class MovimentoService {

    public static final String PREFIXO_DA = "DA";

    private final ProcessoMovimentoMostoRepository repo;
    private final MostoRepository mostoRepo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final CodigoService codigoService;

    public MovimentoService(ProcessoMovimentoMostoRepository repo, MostoRepository mostoRepo,
                            TalhaRepository talhaRepo, DepositoRepository depositoRepo, CodigoService codigoService) {
        this.repo = repo;
        this.mostoRepo = mostoRepo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.codigoService = codigoService;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoMovimentoMosto p = repo.findById(id)
                .orElseThrow(() -> new MovimentoException("Movimento não encontrado."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new MovimentoException("O movimento já está fechado.");
        BigDecimal litros = p.getLitros();
        if (litros == null || litros.signum() <= 0) throw new MovimentoException("Indique os litros (> 0).");

        if (p.getTipo() == TipoMovimento.ENTRADA) {
            boolean talha = p.getTalhaDestino() != null;
            boolean dep = p.getDepositoDestino() != null;
            if (!talha && !dep) throw new MovimentoException("Indique o recipiente de destino.");

            BigDecimal volAtual, cap; String nome;
            Talha t = null; Deposito d = null;
            if (talha) {
                t = talhaRepo.findById(p.getTalhaDestino().getId()).orElseThrow();
                volAtual = v(t.getVolumeAtualLitros()); cap = t.getCapacidadeLitros(); nome = "Talha " + t.getIdentificacao();
            } else {
                d = depositoRepo.findById(p.getDepositoDestino().getId()).orElseThrow();
                volAtual = v(d.getVolumeAtualLitros()); cap = d.getCapacidadeLitros(); nome = "Depósito " + d.getIdentificacao();
            }
            if (cap != null && volAtual.add(litros).compareTo(cap) > 0) {
                throw new MovimentoException(String.format(
                        "%s: capacidade excedida. Capacidade %s L, tem %s L, entrada de %s L.",
                        nome, cap.toPlainString(), volAtual.toPlainString(), litros.toPlainString()));
            }

            Mosto m = new Mosto();
            m.setCodigo(codigoService.proximoCodigo(Mosto.PREFIXO));
            m.setLitros(litros);
            m.setCasta(p.getCasta());
            m.setEstado(EstadoMosto.EM_FERMENTACAO);
            m.setOrigemDescricao("Entrada externa " + p.getCodigo()
                    + (p.getContraparte() != null ? " · " + p.getContraparte() : ""));
            m.setOrigemMovimentoId(p.getId());
            m.setDataProducao(LocalDateTime.now());
            if (talha) { m.setTalha(t); t.setVolumeAtualLitros(volAtual.add(litros)); talhaRepo.save(t); }
            else { m.setDeposito(d); d.setVolumeAtualLitros(volAtual.add(litros)); depositoRepo.save(d); }
            mostoRepo.save(m);

        } else { // SAIDA
            if (p.getMostoOrigem() == null) throw new MovimentoException("Indique o mosto de origem a dar saída.");
            Mosto m = mostoRepo.findById(p.getMostoOrigem().getId())
                    .orElseThrow(() -> new MovimentoException("Mosto de origem não encontrado."));
            if (v(m.getLitros()).compareTo(litros) < 0) {
                throw new MovimentoException(String.format(
                        "Mosto %s só tem %s L — não pode sair %s L.",
                        m.getCodigo(), v(m.getLitros()).toPlainString(), litros.toPlainString()));
            }
            m.setLitros(v(m.getLitros()).subtract(litros));
            mostoRepo.save(m);
            ajustarRecipiente(m, litros.negate());
        }

        if (p.getNumeroDA() == null) p.setNumeroDA(codigoService.proximoCodigo(PREFIXO_DA));
        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
    }

    @Transactional
    public void reabrir(Long id) {
        ProcessoMovimentoMosto p = repo.findById(id)
                .orElseThrow(() -> new MovimentoException("Movimento não encontrado."));
        if (p.getEstado() == EstadoProcesso.ABERTO) return;
        BigDecimal litros = v(p.getLitros());

        if (p.getTipo() == TipoMovimento.ENTRADA) {
            for (Mosto m : mostoRepo.findByOrigemMovimentoId(p.getId())) {
                ajustarRecipiente(m, v(m.getLitros()).negate()); // retira o volume que este mosto ocupa
                mostoRepo.delete(m);
            }
        } else if (p.getMostoOrigem() != null) {
            Mosto m = mostoRepo.findById(p.getMostoOrigem().getId()).orElse(null);
            if (m != null) {
                m.setLitros(v(m.getLitros()).add(litros));
                mostoRepo.save(m);
                ajustarRecipiente(m, litros);
            }
        }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }

    private void ajustarRecipiente(Mosto m, BigDecimal delta) {
        if (m.getTalha() != null) {
            Talha t = talhaRepo.findById(m.getTalha().getId()).orElse(null);
            if (t != null) { t.setVolumeAtualLitros(naoNeg(v(t.getVolumeAtualLitros()).add(delta))); talhaRepo.save(t); }
        } else if (m.getDeposito() != null) {
            Deposito d = depositoRepo.findById(m.getDeposito().getId()).orElse(null);
            if (d != null) { d.setVolumeAtualLitros(naoNeg(v(d.getVolumeAtualLitros()).add(delta))); depositoRepo.save(d); }
        }
    }

    private BigDecimal v(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    private BigDecimal naoNeg(BigDecimal x) { return x.signum() < 0 ? BigDecimal.ZERO : x; }
}
