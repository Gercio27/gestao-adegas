package pt.acv.adega.processos.movimentovinho;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.ContentorService;
import pt.acv.adega.fichas.TipoEmbalagem;
import pt.acv.adega.fichas.Deposito;
import pt.acv.adega.fichas.DepositoRepository;
import pt.acv.adega.fichas.Talha;
import pt.acv.adega.fichas.TalhaRepository;
import pt.acv.adega.movimentos.MovimentoStockService;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras da Fase 5 (entradas, saidas e transfegas de vinho a granel). Emite DA
 * nas entradas e saidas. A transfega move litros entre recipientes do mesmo
 * vinho, com controlo de capacidade. Reversivel. Transacional.
 */
@Service
public class MovimentoVinhoService {

    public static final String PREFIXO_DA = "DA";

    private final ProcessoMovimentoVinhoRepository repo;
    private final MostoRepository mostoRepo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final ContentorService contentorService;
    private final CodigoService codigoService;
    private final MovimentoStockService movimentos;

    public MovimentoVinhoService(ProcessoMovimentoVinhoRepository repo, MostoRepository mostoRepo,
                                 TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                                 ContentorService contentorService, CodigoService codigoService,
                                 MovimentoStockService movimentos) {
        this.movimentos = movimentos;
        this.repo = repo;
        this.mostoRepo = mostoRepo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.contentorService = contentorService;
        this.codigoService = codigoService;
    }

    @Transactional
    public void fechar(Long id) {
        ProcessoMovimentoVinho p = repo.findById(id)
                .orElseThrow(() -> new MovimentoVinhoException("Movimento não encontrado."));
        if (p.getEstado() == EstadoProcesso.FECHADO) throw new MovimentoVinhoException("O movimento já está fechado.");

        if (p.getTipo() == TipoMovimentoVinho.INTRA_EMP) {
            intraEmpresa(p);
        } else {
            BigDecimal litros = p.getLitros();
            if (litros == null || litros.signum() <= 0) throw new MovimentoVinhoException("Indique os litros (> 0).");
            switch (p.getTipo()) {
                case ENTRADA -> entrada(p, litros);
                case SAIDA -> saida(p, litros);
                case TRANSFEGA -> transfega(p, litros);
                default -> { }
            }
            // DA apenas nas entradas e saidas (a transfega e interna).
            if (p.getTipo() != TipoMovimentoVinho.TRANSFEGA && p.getNumeroDA() == null) {
                p.setNumeroDA(codigoService.proximoCodigo(PREFIXO_DA));
            }
        }

        p.setEstado(EstadoProcesso.FECHADO);
        if (p.getDataHoraFim() == null) p.setDataHoraFim(LocalDateTime.now());
        p.setDataFecho(LocalDateTime.now());
        repo.save(p);
    }

    private void entrada(ProcessoMovimentoVinho p, BigDecimal litros) {
        boolean talha = p.getTalhaDestino() != null;
        boolean dep = p.getDepositoDestino() != null;
        if (!talha && !dep) throw new MovimentoVinhoException("Indique o recipiente de destino.");
        Talha t = talha ? talhaRepo.findById(p.getTalhaDestino().getId()).orElseThrow() : null;
        Deposito d = dep ? depositoRepo.findById(p.getDepositoDestino().getId()).orElseThrow() : null;
        exigirCapacidade(t, d, litros);

        Mosto m = new Mosto();
        m.setCodigo(codigoService.proximoCodigo(Mosto.PREFIXO));
        m.setLitros(litros);
        m.setCasta(p.getCasta());
        m.setVinhoNome(p.getNomeVinho());
        m.setEstado(EstadoMosto.VINHO_GRANEL);
        m.setOrigemDescricao("Entrada externa " + p.getCodigo()
                + (p.getContraparte() != null ? " · " + p.getContraparte() : ""));
        m.setOrigemMovimentoId(p.getId());
        m.setDataProducao(LocalDateTime.now());
        String proc = "Entrada de vinho " + p.getCodigo();
        String desc = "Entrada externa" + (p.getContraparte() != null ? " de " + p.getContraparte() : "");
        if (talha) { m.setTalha(t); somarVolume(t, null, litros, proc, desc, p.getNomeVinho()); }
        else { m.setDeposito(d); somarVolume(null, d, litros, proc, desc, p.getNomeVinho()); }
        mostoRepo.save(m);
    }

    private void saida(ProcessoMovimentoVinho p, BigDecimal litros) {
        if (p.getMostoOrigem() == null) throw new MovimentoVinhoException("Indique o vinho de origem a dar saída.");
        Mosto m = mostoRepo.findById(p.getMostoOrigem().getId())
                .orElseThrow(() -> new MovimentoVinhoException("Vinho de origem não encontrado."));
        if (v(m.getLitros()).compareTo(litros) < 0) {
            throw new MovimentoVinhoException(String.format("%s só tem %s L — não pode sair %s L.",
                    m.getCodigo(), v(m.getLitros()).toPlainString(), litros.toPlainString()));
        }
        m.setLitros(v(m.getLitros()).subtract(litros));
        mostoRepo.save(m);
        ajustarRecipiente(m, litros.negate(), "Saída de vinho " + p.getCodigo(),
                "Saída externa" + (p.getContraparte() != null ? " para " + p.getContraparte() : ""));
    }

    private void transfega(ProcessoMovimentoVinho p, BigDecimal litros) {
        if (p.getMostoOrigem() == null) throw new MovimentoVinhoException("Indique o vinho/depósito de origem.");
        boolean talha = p.getTalhaDestino() != null;
        boolean dep = p.getDepositoDestino() != null;
        if (!talha && !dep) throw new MovimentoVinhoException("Indique o recipiente de destino.");

        Mosto origem = mostoRepo.findById(p.getMostoOrigem().getId())
                .orElseThrow(() -> new MovimentoVinhoException("Vinho de origem não encontrado."));
        if (v(origem.getLitros()).compareTo(litros) < 0) {
            throw new MovimentoVinhoException(String.format("%s só tem %s L — não pode transfegar %s L.",
                    origem.getCodigo(), v(origem.getLitros()).toPlainString(), litros.toPlainString()));
        }
        Talha t = talha ? talhaRepo.findById(p.getTalhaDestino().getId()).orElseThrow() : null;
        Deposito d = dep ? depositoRepo.findById(p.getDepositoDestino().getId()).orElseThrow() : null;
        exigirCapacidade(t, d, litros);

        // Baixa na origem
        origem.setLitros(v(origem.getLitros()).subtract(litros));
        mostoRepo.save(origem);
        boolean interna = p.getTipo() == TipoMovimentoVinho.INTRA_EMP;
        String proc = (interna ? "Saída intra-empresa " : "Transfega ") + p.getCodigo();
        ajustarRecipiente(origem, litros.negate(), proc,
                "Saída para " + (talha ? "Talha " + t.getIdentificacao() : "Depósito " + d.getIdentificacao())
                        + (p.getMatriculaCamiao() != null ? " (camião " + p.getMatriculaCamiao() + ")" : ""));

        // Destino: junta a um mosto do mesmo vinho no recipiente, ou cria um novo.
        String nome = p.getNomeVinho() != null ? p.getNomeVinho() : origem.getVinhoNome();
        List<Mosto> noDestino = talha ? mostoRepo.findByTalhaId(t.getId()) : mostoRepo.findByDepositoId(d.getId());
        Mosto destino = null;
        for (Mosto m : noDestino) {
            if (m.getEstado() == EstadoMosto.VINHO_GRANEL && m.getId() != null
                    && !m.getId().equals(origem.getId())
                    && nome != null && nome.equals(m.getVinhoNome())) { destino = m; break; }
        }
        boolean criado = false;
        if (destino == null) {
            destino = new Mosto();
            destino.setCodigo(codigoService.proximoCodigo(Mosto.PREFIXO));
            destino.setLitros(BigDecimal.ZERO);
            destino.setCasta(origem.getCasta());
            destino.setVinhoNome(nome);
            destino.setLoteCodigo(origem.getLoteCodigo());
            destino.setEstado(EstadoMosto.VINHO_GRANEL);
            destino.setOrigemDescricao("Transfega " + p.getCodigo() + " de " + origem.getCodigo());
            destino.setDataProducao(LocalDateTime.now());
            if (talha) destino.setTalha(t); else destino.setDeposito(d);
            criado = true;
        }
        destino.setLitros(v(destino.getLitros()).add(litros));
        mostoRepo.save(destino);
        somarVolume(t, d, litros, proc, "Entrada vinda de " + origem.getCodigo(), nome);

        p.setMostoDestinoId(destino.getId());
        p.setDestinoCriado(criado);
    }

    /** Saída intra-empresa: transferência interna de granel/mosto ou de produto acabado. */
    private void intraEmpresa(ProcessoMovimentoVinho p) {
        exigirResponsaveis(p);
        if (p.isEngarrafado()) {
            intraEngarrafado(p);
        } else {
            // Granel/mosto: sai em deposito de camiao, com matricula obrigatoria.
            if (vazio(p.getMatriculaCamiao())) {
                throw new MovimentoVinhoException("Indique a matrícula do camião que transporta o granel.");
            }
            // Mover litros entre recipientes (pode ser entre adegas).
            BigDecimal litros = p.getLitros();
            if (litros == null || litros.signum() <= 0) throw new MovimentoVinhoException("Indique os litros a transferir (> 0).");
            transfega(p, litros);
        }
    }

    /** A entrega e a receção têm de estar identificadas (exigência do D.A.). */
    private void exigirResponsaveis(ProcessoMovimentoVinho p) {
        if (vazio(p.getResponsavelEntrega())) throw new MovimentoVinhoException("Indique o responsável da entrega.");
        if (vazio(p.getResponsavelRececao())) throw new MovimentoVinhoException("Indique o responsável da receção.");
    }

    private boolean vazio(String s) { return s == null || s.isBlank(); }

    /** Transferência de produto acabado entre contentores — garrafas ou bag-in-box. */
    private void intraEngarrafado(ProcessoMovimentoVinho p) {
        TipoEmbalagem tipo = p.getTipoEmbalagem();
        String unidade = tipo.getUnidade();
        if (p.getContentorOrigemId() == null) throw new MovimentoVinhoException("Indique o contentor de origem.");
        ContentorService.Opcao orig = contentorService.procurar(tipo, p.getContentorOrigemId());
        if (orig == null) throw new MovimentoVinhoException("Contentor de origem não encontrado.");
        if (orig.stock() <= 0) throw new MovimentoVinhoException("O contentor de origem está vazio.");
        if (p.getContentorDestinoId() == null) throw new MovimentoVinhoException("Indique o contentor de destino.");
        if (p.getContentorDestinoId().equals(p.getContentorOrigemId())) throw new MovimentoVinhoException("O destino tem de ser diferente da origem.");
        ContentorService.Opcao dest = contentorService.procurar(tipo, p.getContentorDestinoId());
        if (dest == null) throw new MovimentoVinhoException("Contentor de destino não encontrado.");

        int qtd = p.isContentorCompleto() ? orig.stock() : (p.getGarrafas() == null ? 0 : p.getGarrafas());
        if (qtd <= 0) throw new MovimentoVinhoException(
                "Indique quantas " + unidade + " transferir (> 0) ou marque 'contentor completo'.");
        if (qtd > orig.stock()) throw new MovimentoVinhoException(
                String.format("O contentor de origem só tem %d %s.", orig.stock(), unidade));

        // O destino tem de estar vazio ou já ter o mesmo vinho.
        Long vinhoOrigem = contentorService.vinhoDe(tipo, p.getContentorOrigemId());
        Long vinhoDestino = contentorService.vinhoDe(tipo, p.getContentorDestinoId());
        if (dest.stock() > 0 && vinhoDestino != null && vinhoOrigem != null && !vinhoDestino.equals(vinhoOrigem)) {
            throw new MovimentoVinhoException("O contentor de destino já tem outro vinho. Escolha um vazio ou com o mesmo vinho.");
        }

        contentorService.transferir(tipo, p.getContentorOrigemId(), p.getContentorDestinoId(), qtd,
                "Saída intra-empresa " + p.getCodigo(), "Transferência interna entre locais");

        p.setGarrafas(qtd);
        if (p.getNomeVinho() == null) p.setNomeVinho(orig.vinhoNome());
    }

    private void reverterIntraEngarrafado(ProcessoMovimentoVinho p) {
        int qtd = p.getGarrafas() == null ? 0 : p.getGarrafas();
        if (qtd <= 0) return;
        // Devolve ao contentor de origem o que tinha ido para o destino.
        contentorService.transferir(p.getTipoEmbalagem(), p.getContentorDestinoId(), p.getContentorOrigemId(), qtd,
                "Saída intra-empresa " + p.getCodigo(), "Movimento reaberto — devolvido");
    }

    @Transactional
    public void reabrir(Long id) {
        ProcessoMovimentoVinho p = repo.findById(id)
                .orElseThrow(() -> new MovimentoVinhoException("Movimento não encontrado."));
        if (p.getEstado() == EstadoProcesso.ABERTO) return;
        BigDecimal litros = v(p.getLitros());

        if (p.isEngarrafado()) {
            reverterIntraEngarrafado(p);
        } else if (p.getTipo() == TipoMovimentoVinho.ENTRADA) {
            for (Mosto m : mostoRepo.findByOrigemMovimentoId(p.getId())) {
                ajustarRecipiente(m, v(m.getLitros()).negate(), "Entrada de vinho " + p.getCodigo(),
                        "Entrada reaberta — vinho anulado");
                mostoRepo.delete(m);
            }
        } else if (p.getTipo() == TipoMovimentoVinho.SAIDA) {
            if (p.getMostoOrigem() != null) {
                Mosto m = mostoRepo.findById(p.getMostoOrigem().getId()).orElse(null);
                if (m != null) { m.setLitros(v(m.getLitros()).add(litros)); mostoRepo.save(m);
                    ajustarRecipiente(m, litros, "Saída de vinho " + p.getCodigo(),
                            "Saída reaberta — vinho devolvido"); }
            }
        } else { // TRANSFEGA
            if (p.getMostoOrigem() != null) {
                Mosto origem = mostoRepo.findById(p.getMostoOrigem().getId()).orElse(null);
                if (origem != null) { origem.setLitros(v(origem.getLitros()).add(litros)); mostoRepo.save(origem);
                    ajustarRecipiente(origem, litros, "Transfega " + p.getCodigo(),
                            "Transfega reaberta — vinho devolvido"); }
            }
            if (p.getMostoDestinoId() != null) {
                Mosto destino = mostoRepo.findById(p.getMostoDestinoId()).orElse(null);
                if (destino != null) {
                    destino.setLitros(naoNeg(v(destino.getLitros()).subtract(litros)));
                    ajustarRecipiente(destino, litros.negate(), "Transfega " + p.getCodigo(),
                            "Transfega reaberta — vinho retirado do destino");
                    if (p.isDestinoCriado() && v(destino.getLitros()).signum() <= 0) mostoRepo.delete(destino);
                    else mostoRepo.save(destino);
                }
            }
        }
        p.setEstado(EstadoProcesso.ABERTO);
        p.setDataFecho(null);
        repo.save(p);
    }

    // ----- auxiliares -----

    private void exigirCapacidade(Talha t, Deposito d, BigDecimal litros) {
        BigDecimal vol, cap; String nome;
        if (t != null) { vol = v(t.getVolumeAtualLitros()); cap = t.getCapacidadeLitros(); nome = "Talha " + t.getIdentificacao(); }
        else { vol = v(d.getVolumeAtualLitros()); cap = d.getCapacidadeLitros(); nome = "Depósito " + d.getIdentificacao(); }
        if (cap != null && vol.add(litros).compareTo(cap) > 0) {
            throw new MovimentoVinhoException(String.format(
                    "%s: capacidade excedida. Capacidade %s L, tem %s L, a entrar %s L.",
                    nome, cap.toPlainString(), vol.toPlainString(), litros.toPlainString()));
        }
    }

    private void somarVolume(Talha t, Deposito d, BigDecimal delta, String origem, String descricao, String vinho) {
        if (t != null) {
            t.setVolumeAtualLitros(naoNeg(v(t.getVolumeAtualLitros()).add(delta))); talhaRepo.save(t);
            movimentos.talha(t, delta, origem, descricao, vinho);
        } else if (d != null) {
            d.setVolumeAtualLitros(naoNeg(v(d.getVolumeAtualLitros()).add(delta))); depositoRepo.save(d);
            movimentos.deposito(d, delta, origem, descricao, vinho);
        }
    }

    private void ajustarRecipiente(Mosto m, BigDecimal delta, String origem, String descricao) {
        if (m.getTalha() != null) {
            Talha t = talhaRepo.findById(m.getTalha().getId()).orElse(null);
            if (t != null) {
                t.setVolumeAtualLitros(naoNeg(v(t.getVolumeAtualLitros()).add(delta))); talhaRepo.save(t);
                movimentos.talha(t, delta, origem, descricao, m.getVinhoNome());
            }
        } else if (m.getDeposito() != null) {
            Deposito d = depositoRepo.findById(m.getDeposito().getId()).orElse(null);
            if (d != null) {
                d.setVolumeAtualLitros(naoNeg(v(d.getVolumeAtualLitros()).add(delta))); depositoRepo.save(d);
                movimentos.deposito(d, delta, origem, descricao, m.getVinhoNome());
            }
        }
    }

    private BigDecimal v(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    private BigDecimal naoNeg(BigDecimal x) { return x.signum() < 0 ? BigDecimal.ZERO : x; }
}
