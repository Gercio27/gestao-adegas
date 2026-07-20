package pt.acv.adega.processos.moagem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.DepositoRepository;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.fichas.TalhaRepository;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Logica de negocio do fecho/reabertura da Moagem. Isolada num servico
 * transacional para garantir que a geracao de mostos e a atualizacao dos
 * volumes acontecem tudo-ou-nada.
 */
@Service
public class MoagemService {

    private final ProcessoMoagemRepository moagemRepo;
    private final MostoRepository mostoRepo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final CodigoService codigoService;

    public MoagemService(ProcessoMoagemRepository moagemRepo, MostoRepository mostoRepo,
                         TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                         CodigoService codigoService) {
        this.moagemRepo = moagemRepo;
        this.mostoRepo = mostoRepo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.codigoService = codigoService;
    }

    /**
     * Fecha a moagem: valida a capacidade de cada recipiente, cria uma ficha de
     * mosto por enchimento e soma o volume aos recipientes. Se algo falhar,
     * lanca MoagemException e a transacao e revertida (nada e gravado).
     */
    @Transactional
    public void fechar(Long moagemId) {
        ProcessoMoagem m = moagemRepo.findById(moagemId)
                .orElseThrow(() -> new MoagemException("Moagem não encontrada."));
        if (m.getEstado() == EstadoProcesso.FECHADO) {
            throw new MoagemException("A moagem já está fechada.");
        }
        List<Enchimento> linhas = m.getEnchimentos();
        if (linhas.isEmpty()) {
            throw new MoagemException("Adicione pelo menos um enchimento antes de fechar.");
        }

        // 1) Validacao previa da capacidade (soma por recipiente).
        Map<String, BigDecimal> acrescimoPorRecipiente = new HashMap<>();
        for (Enchimento e : linhas) {
            if (e.getLitros() == null || e.getLitros().signum() <= 0) {
                throw new MoagemException("Cada enchimento tem de ter litros > 0.");
            }
            if (e.getTalha() == null && e.getDeposito() == null) {
                throw new MoagemException("Cada enchimento tem de indicar uma talha ou depósito.");
            }
            String chave = e.getTalha() != null ? "T" + e.getTalha().getId() : "D" + e.getDeposito().getId();
            acrescimoPorRecipiente.merge(chave, e.getLitros(), BigDecimal::add);
        }
        for (Enchimento e : linhas) {
            if (e.getTalha() != null) {
                Talha t = talhaRepo.findById(e.getTalha().getId()).orElseThrow();
                validarCapacidade("Talha " + t.getIdentificacao(), t.getCapacidadeLitros(),
                        t.getVolumeAtualLitros(), acrescimoPorRecipiente.get("T" + t.getId()));
            } else {
                Deposito d = depositoRepo.findById(e.getDeposito().getId()).orElseThrow();
                validarCapacidade("Depósito " + d.getIdentificacao(), d.getCapacidadeLitros(),
                        d.getVolumeAtualLitros(), acrescimoPorRecipiente.get("D" + d.getId()));
            }
        }

        // 2) Efetivar: criar mostos e somar volumes.
        String origem = descreverOrigem(m);
        String nomeVinho = m.getPlano() != null ? m.getPlano().getNomeVinho() : null;
        for (Enchimento e : linhas) {
            Mosto mosto = new Mosto();
            mosto.setCodigo(codigoService.proximoCodigo(Mosto.PREFIXO));
            mosto.setLitros(e.getLitros());
            mosto.setCasta(e.getCasta());
            mosto.setCastas(new java.util.ArrayList<>(e.getCastas()));
            mosto.setEstado(EstadoMosto.EM_FERMENTACAO);
            mosto.setOrigemDescricao(origem);
            mosto.setOrigemMoagemId(m.getId());
            // Grava o nome do vinho na ficha do mosto para que as fases seguintes
            // o mostrem sem terem de o ir buscar ao planeamento da moagem.
            mosto.setVinhoNome(nomeVinho);
            mosto.setAlcoolProvavel(e.getAlcoolProvavel());
            mosto.setDataProducao(LocalDateTime.now());

            if (e.getTalha() != null) {
                Talha t = talhaRepo.findById(e.getTalha().getId()).orElseThrow();
                t.setVolumeAtualLitros(somar(t.getVolumeAtualLitros(), e.getLitros()));
                talhaRepo.save(t);
                mosto.setTalha(t);
            } else {
                Deposito d = depositoRepo.findById(e.getDeposito().getId()).orElseThrow();
                d.setVolumeAtualLitros(somar(d.getVolumeAtualLitros(), e.getLitros()));
                depositoRepo.save(d);
                mosto.setDeposito(d);
            }
            mostoRepo.save(mosto);
        }

        m.setEstado(EstadoProcesso.FECHADO);
        if (m.getDataHoraFim() == null) m.setDataHoraFim(LocalDateTime.now());
        m.setDataFecho(LocalDateTime.now());
        moagemRepo.save(m);
    }

    /**
     * Reabre a moagem (apenas admin): elimina os mostos gerados e devolve os
     * volumes aos recipientes, para permitir corrigir e voltar a fechar.
     */
    @Transactional
    public void reabrir(Long moagemId) {
        ProcessoMoagem m = moagemRepo.findById(moagemId)
                .orElseThrow(() -> new MoagemException("Moagem não encontrada."));
        List<Mosto> gerados = mostoRepo.findByOrigemMoagemId(m.getId());
        for (Mosto mosto : gerados) {
            if (mosto.getTalha() != null) {
                Talha t = mosto.getTalha();
                t.setVolumeAtualLitros(subtrair(t.getVolumeAtualLitros(), mosto.getLitros()));
                talhaRepo.save(t);
            } else if (mosto.getDeposito() != null) {
                Deposito d = mosto.getDeposito();
                d.setVolumeAtualLitros(subtrair(d.getVolumeAtualLitros(), mosto.getLitros()));
                depositoRepo.save(d);
            }
            mostoRepo.delete(mosto);
        }
        m.setEstado(EstadoProcesso.ABERTO);
        m.setDataFecho(null);
        moagemRepo.save(m);
    }

    // ----- auxiliares -----

    private void validarCapacidade(String nome, BigDecimal capacidade, BigDecimal volumeAtual, BigDecimal acrescimo) {
        if (capacidade == null) return; // sem capacidade definida -> nao e possivel validar
        BigDecimal atual = volumeAtual == null ? BigDecimal.ZERO : volumeAtual;
        BigDecimal total = atual.add(acrescimo);
        if (total.compareTo(capacidade) > 0) {
            throw new MoagemException(String.format(
                    "%s: capacidade excedida. Capacidade %s L, já tem %s L, tentou juntar %s L (total %s L).",
                    nome, capacidade.toPlainString(), atual.toPlainString(),
                    acrescimo.toPlainString(), total.toPlainString()));
        }
    }

    private BigDecimal somar(BigDecimal a, BigDecimal b) {
        return (a == null ? BigDecimal.ZERO : a).add(b);
    }

    private BigDecimal subtrair(BigDecimal a, BigDecimal b) {
        BigDecimal r = (a == null ? BigDecimal.ZERO : a).subtract(b);
        return r.signum() < 0 ? BigDecimal.ZERO : r;
    }

    private String descreverOrigem(ProcessoMoagem m) {
        StringBuilder sb = new StringBuilder("Moagem ").append(m.getCodigo());
        if (m.getPlano() != null) sb.append(" · Vinho ").append(m.getPlano().getNomeVinho());
        if (m.getAdega() != null) sb.append(" · Adega ").append(m.getAdega().getNome());
        return sb.toString();
    }
}
