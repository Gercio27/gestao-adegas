package pt.acv.adega.processos.loteamento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.*;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras da Fase 6 (loteamento). A construcao e uma transfega que da baixa num
 * deposito de origem e junta no vinho do lote (deposito de destino), com
 * controlo de capacidade. Reversivel. Transacional.
 */
@Service
public class LoteamentoService {

    private final LoteamentoRepository loteRepo;
    private final LoteConstrucaoRepository construcaoRepo;
    private final MostoRepository mostoRepo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final RecipienteService recipienteService;
    private final CodigoService codigoService;

    public LoteamentoService(LoteamentoRepository loteRepo, LoteConstrucaoRepository construcaoRepo,
                             MostoRepository mostoRepo, TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                             RecipienteService recipienteService, CodigoService codigoService) {
        this.loteRepo = loteRepo;
        this.construcaoRepo = construcaoRepo;
        this.mostoRepo = mostoRepo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.recipienteService = recipienteService;
        this.codigoService = codigoService;
    }

    @Transactional
    public void executarConstrucao(Long loteId, Long mostoOrigemId, String destinoRef, BigDecimal litros) {
        Loteamento lote = loteRepo.findById(loteId)
                .orElseThrow(() -> new LoteamentoException("Lote não encontrado."));
        if (lote.isConcluido()) throw new LoteamentoException("O lote já está concluído.");
        if (litros == null || litros.signum() <= 0) throw new LoteamentoException("Indique os litros (> 0).");
        if (mostoOrigemId == null) throw new LoteamentoException("Indique o depósito de origem.");

        Mosto origem = mostoRepo.findById(mostoOrigemId)
                .orElseThrow(() -> new LoteamentoException("Depósito de origem não encontrado."));
        if (v(origem.getLitros()).compareTo(litros) < 0) {
            throw new LoteamentoException(String.format("%s só tem %s L — não pode sair %s L.",
                    origem.getCodigo(), v(origem.getLitros()).toPlainString(), litros.toPlainString()));
        }

        RecipienteService.Recipiente r = recipienteService.resolver(destinoRef);
        Talha t = r.talha() != null ? talhaRepo.findById(r.talha().getId()).orElseThrow() : null;
        Deposito d = r.deposito() != null ? depositoRepo.findById(r.deposito().getId()).orElseThrow() : null;
        if (t == null && d == null) throw new LoteamentoException("Indique o depósito de destino do lote.");
        exigirCapacidade(t, d, litros);

        // Baixa na origem
        origem.setLitros(v(origem.getLitros()).subtract(litros));
        mostoRepo.save(origem);
        ajustarRecipiente(origem, litros.negate());

        // Destino: o vinho do lote (por codigo do lote) neste recipiente, ou cria.
        List<Mosto> noDestino = t != null ? mostoRepo.findByTalhaId(t.getId()) : mostoRepo.findByDepositoId(d.getId());
        Mosto destino = null;
        for (Mosto m : noDestino) {
            if (lote.getCodigo().equals(m.getLoteCodigo())) { destino = m; break; }
        }
        boolean criado = false;
        if (destino == null) {
            destino = new Mosto();
            destino.setCodigo(codigoService.proximoCodigo(Mosto.PREFIXO));
            destino.setLitros(BigDecimal.ZERO);
            destino.setCasta(origem.getCasta());
            destino.setVinhoNome(lote.getNome());
            destino.setLoteCodigo(lote.getCodigo());
            destino.setEstado(EstadoMosto.VINHO_GRANEL);
            destino.setOrigemDescricao("Lote " + lote.getCodigo() + " · " + lote.getNome());
            destino.setDataProducao(LocalDateTime.now());
            if (t != null) destino.setTalha(t); else destino.setDeposito(d);
            criado = true;
        }
        destino.setLitros(v(destino.getLitros()).add(litros));
        mostoRepo.save(destino);
        if (t != null) { t.setVolumeAtualLitros(naoNeg(v(t.getVolumeAtualLitros()).add(litros))); talhaRepo.save(t); }
        else { d.setVolumeAtualLitros(naoNeg(v(d.getVolumeAtualLitros()).add(litros))); depositoRepo.save(d); }

        LoteConstrucao c = new LoteConstrucao();
        c.setLoteamentoId(loteId);
        c.setNumero((int) construcaoRepo.countByLoteamentoId(loteId) + 1);
        c.setMostoOrigemId(origem.getId());
        c.setOrigemDescricao(origem.getCodigo() + " · " + origem.getLocalizacao());
        c.setDestinoRef(destinoRef);
        c.setDestinoDescricao(t != null ? "Talha " + t.getIdentificacao() : "Depósito " + d.getIdentificacao());
        c.setLitros(litros);
        c.setMostoDestinoId(destino.getId());
        c.setDestinoCriado(criado);
        c.setData(LocalDateTime.now());
        construcaoRepo.save(c);
    }

    @Transactional
    public void reverterConstrucao(Long construcaoId) {
        LoteConstrucao c = construcaoRepo.findById(construcaoId)
                .orElseThrow(() -> new LoteamentoException("Construção não encontrada."));
        BigDecimal litros = v(c.getLitros());
        // Repor na origem
        if (c.getMostoOrigemId() != null) {
            Mosto origem = mostoRepo.findById(c.getMostoOrigemId()).orElse(null);
            if (origem != null) { origem.setLitros(v(origem.getLitros()).add(litros)); mostoRepo.save(origem); ajustarRecipiente(origem, litros); }
        }
        // Retirar do destino
        if (c.getMostoDestinoId() != null) {
            Mosto destino = mostoRepo.findById(c.getMostoDestinoId()).orElse(null);
            if (destino != null) {
                destino.setLitros(naoNeg(v(destino.getLitros()).subtract(litros)));
                ajustarRecipiente(destino, litros.negate());
                if (c.isDestinoCriado() && v(destino.getLitros()).signum() <= 0) mostoRepo.delete(destino);
                else mostoRepo.save(destino);
            }
        }
        Loteamento lote = loteRepo.findById(c.getLoteamentoId()).orElse(null);
        if (lote != null && lote.isConcluido()) { lote.setConcluido(false); loteRepo.save(lote); }
        construcaoRepo.delete(c);
    }

    private void exigirCapacidade(Talha t, Deposito d, BigDecimal litros) {
        BigDecimal vol, cap; String nome;
        if (t != null) { vol = v(t.getVolumeAtualLitros()); cap = t.getCapacidadeLitros(); nome = "Talha " + t.getIdentificacao(); }
        else { vol = v(d.getVolumeAtualLitros()); cap = d.getCapacidadeLitros(); nome = "Depósito " + d.getIdentificacao(); }
        if (cap != null && vol.add(litros).compareTo(cap) > 0) {
            throw new LoteamentoException(String.format(
                    "%s: capacidade excedida. Capacidade %s L, tem %s L, a entrar %s L.",
                    nome, cap.toPlainString(), vol.toPlainString(), litros.toPlainString()));
        }
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
