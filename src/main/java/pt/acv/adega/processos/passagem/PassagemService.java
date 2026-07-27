package pt.acv.adega.processos.passagem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.DepositoRepository;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.fichas.TalhaRepository;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras da passagem de mosto a vinho pronto a granel (Fase 4.4). Ao fechar, por
 * cada linha: o mosto passa a VINHO_GRANEL, fica com os litros efetivos (a
 * diferenca para os originais e perda/borras que sai do stock) e, se indicada
 * uma talha de destino, o vinho e movido para la (com controlo de capacidade).
 * Reversivel (reabrir) atraves dos snapshots guardados em cada linha.
 */
@Service
public class PassagemService {

    private final ProcessoPassagemVinhoRepository repo;
    private final MostoRepository mostoRepo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final CodigoService codigoService;

    public PassagemService(ProcessoPassagemVinhoRepository repo, MostoRepository mostoRepo,
                           TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                           CodigoService codigoService) {
        this.repo = repo;
        this.mostoRepo = mostoRepo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.codigoService = codigoService;
    }

    /**
     * Fecha o processo e devolve os avisos que houver. Passar mais litros do
     * que os registados no mosto e' permitido - a medicao real manda - e fica
     * so' o aviso.
     */
    @Transactional
    public String fechar(Long id) {
        ProcessoPassagemVinho p = repo.findById(id)
                .orElseThrow(() -> new PassagemException("Processo não encontrado."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new PassagemException("O processo já está fechado.");

        List<PassagemItem> itens = p.getItens();
        if (itens.isEmpty()) throw new PassagemException("Selecione pelo menos um mosto para passar a vinho a granel.");

        int convertidos = 0;
        List<String> avisos = new java.util.ArrayList<>();
        for (PassagemItem it : itens) {
            Mosto m = mostoRepo.findById(it.getMostoId()).orElse(null);
            if (m == null || m.getEstado() == EstadoMosto.VINHO_GRANEL) continue;

            BigDecimal orig = v(m.getLitros());
            BigDecimal efet = it.getLitrosEfetivos() != null ? it.getLitrosEfetivos() : orig;
            if (efet.signum() <= 0) throw new PassagemException("Os litros efetivos têm de ser > 0 (mosto " + m.getCodigo() + ").");
            if (efet.compareTo(orig) > 0) {
                avisos.add(String.format("Mosto %s: passou %s L, mais %s L do que os %s L registados.",
                        m.getCodigo(), efet.toPlainString(),
                        efet.subtract(orig).toPlainString(), orig.toPlainString()));
            }

            // Snapshot da origem, para reverter.
            it.setLitrosOriginais(orig);
            it.setTalhaOrigemId(m.getTalha() != null ? m.getTalha().getId() : null);
            it.setDepositoOrigemId(m.getDeposito() != null ? m.getDeposito().getId() : null);

            // O que não foi passado. Só é perda se o utilizador disser que o
            // recipiente ficou vazio; senão fica lá como mosto por passar.
            BigDecimal sobra = orig.subtract(efet);
            boolean guardarSobra = sobra.signum() > 0 && !it.isOrigemVazia();

            Long destinoId = it.getTalhaDestinoId();
            boolean fica = destinoId == null
                    || (m.getTalha() != null && destinoId.equals(m.getTalha().getId()));

            if (fica) {
                // Fica no mesmo recipiente: só desce o volume se a sobra for perda.
                if (!guardarSobra) ajustarRecipiente(m, efet.subtract(orig));
                it.setMovido(false);
            } else {
                Talha destino = talhaRepo.findById(destinoId)
                        .orElseThrow(() -> new PassagemException("Talha de destino não encontrada."));
                String excesso = avisoCapacidade(destino, efet);
                if (excesso != null) avisos.add(excesso);
                // Tira da origem os litros que saem (tudo, se a sobra for perda).
                ajustarRecipiente(m, guardarSobra ? efet.negate() : orig.negate());
                // ...e poe os litros efetivos na talha de destino.
                destino.setVolumeAtualLitros(naoNeg(v(destino.getVolumeAtualLitros()).add(efet)));
                talhaRepo.save(destino);
            }

            // A sobra fica no recipiente de origem, como mosto ainda por passar.
            if (guardarSobra) {
                Mosto resto = new Mosto();
                resto.setCodigo(codigoService.proximoCodigo(Mosto.PREFIXO));
                resto.setLitros(sobra);
                resto.setEstado(EstadoMosto.EM_FERMENTACAO);
                resto.setCasta(m.getCasta());
                resto.setCastas(new java.util.ArrayList<>(m.getCastas()));
                resto.setVinhoNome(m.getVinhoNome());
                resto.setAlcoolProvavel(m.getAlcoolProvavel());
                resto.setOrigemMoagemId(m.getOrigemMoagemId());
                resto.setOrigemDescricao("Sobra da passagem a limpo " + p.getCodigo() + " de " + m.getCodigo());
                resto.setDataProducao(LocalDateTime.now());
                resto.setTalha(m.getTalha());
                resto.setDeposito(m.getDeposito());
                mostoRepo.save(resto);
                it.setMostoRestanteId(resto.getId());
            }

            // Só agora se move o mosto passado, para a sobra ficar no sítio certo.
            if (!fica) {
                Talha destino = talhaRepo.findById(destinoId).orElseThrow();
                m.setTalha(destino);
                m.setDeposito(null);
                it.setMovido(true);
            }

            m.setLitros(efet);
            m.setEstado(EstadoMosto.VINHO_GRANEL);
            mostoRepo.save(m);
            convertidos++;
        }
        if (convertidos == 0) throw new PassagemException("Nenhum dos mostos selecionados está em fermentação.");

        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
        return avisos.isEmpty() ? null : String.join(" ", avisos);
    }

    @Transactional
    public void reabrir(Long id) {
        ProcessoPassagemVinho p = repo.findById(id)
                .orElseThrow(() -> new PassagemException("Processo não encontrado."));
        if (p.getEstado() == EstadoProcesso.ABERTO) return;

        for (PassagemItem it : p.getItens()) {
            Mosto m = mostoRepo.findById(it.getMostoId()).orElse(null);
            if (m == null) continue;
            BigDecimal orig = v(it.getLitrosOriginais());
            BigDecimal efet = it.getLitrosEfetivos() != null ? it.getLitrosEfetivos() : orig;

            // A sobra guardada volta a fazer parte do mosto original.
            BigDecimal sobraGuardada = BigDecimal.ZERO;
            if (it.getMostoRestanteId() != null) {
                Mosto resto = mostoRepo.findById(it.getMostoRestanteId()).orElse(null);
                if (resto != null) { sobraGuardada = v(resto.getLitros()); mostoRepo.delete(resto); }
                it.setMostoRestanteId(null);
            }
            boolean sobraFicou = sobraGuardada.signum() > 0;

            if (it.isMovido()) {
                // Tira os litros efetivos da talha de destino (onde o mosto esta agora).
                ajustarRecipiente(m, efet.negate());
                // Devolve ao recipiente de origem exatamente o que de la' saiu:
                // so' os litros passados, se a sobra tiver ficado la'.
                BigDecimal devolver = sobraFicou ? efet : orig;
                if (it.getTalhaOrigemId() != null) {
                    Talha to = talhaRepo.findById(it.getTalhaOrigemId()).orElse(null);
                    m.setTalha(to); m.setDeposito(null);
                    if (to != null) { to.setVolumeAtualLitros(naoNeg(v(to.getVolumeAtualLitros()).add(devolver))); talhaRepo.save(to); }
                } else if (it.getDepositoOrigemId() != null) {
                    Deposito dep = depositoRepo.findById(it.getDepositoOrigemId()).orElse(null);
                    m.setDeposito(dep); m.setTalha(null);
                    if (dep != null) { dep.setVolumeAtualLitros(naoNeg(v(dep.getVolumeAtualLitros()).add(devolver))); depositoRepo.save(dep); }
                }
            } else {
                // Ficou no mesmo recipiente: se a sobra ficou la', o volume nunca
                // desceu e nao ha nada a devolver; senao devolve-se a perda.
                if (!sobraFicou) ajustarRecipiente(m, orig.subtract(efet));
            }

            m.setLitros(orig);
            m.setEstado(EstadoMosto.EM_FERMENTACAO);
            mostoRepo.save(m);

            it.setLitrosOriginais(null);
            it.setTalhaOrigemId(null);
            it.setDepositoOrigemId(null);
            it.setMovido(false);
        }

        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }

    /** Aviso de capacidade do destino (null se couber). Nao bloqueia. */
    private String avisoCapacidade(Talha t, BigDecimal litros) {
        BigDecimal vol = v(t.getVolumeAtualLitros());
        BigDecimal cap = t.getCapacidadeLitros();
        if (cap == null || vol.add(litros).compareTo(cap) <= 0) return null;
        return String.format("Talha %s: fica com %s L, acima da capacidade de %s L (%s L a mais).",
                t.getIdentificacao(), vol.add(litros).toPlainString(), cap.toPlainString(),
                vol.add(litros).subtract(cap).toPlainString());
    }

    /** Soma delta ao volume do recipiente onde o mosto se encontra (talha ou deposito). */
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
