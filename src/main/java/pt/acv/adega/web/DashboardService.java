package pt.acv.adega.web;

import org.springframework.stereotype.Service;
import pt.acv.adega.fichas.*;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.processos.maturacao.ProcessoAnaliseMaturacaoRepository;
import pt.acv.adega.processos.atesto.ProcessoAtestoRepository;
import pt.acv.adega.processos.comercial.ProcessoPassagemComercialRepository;
import pt.acv.adega.processos.engarrafamento.ProcessoEngarrafamentoRepository;
import pt.acv.adega.processos.moagem.ProcessoMoagemRepository;
import pt.acv.adega.processos.passagem.ProcessoPassagemVinhoRepository;
import pt.acv.adega.processos.remontagem.ProcessoRemontagemRepository;
import pt.acv.adega.processos.rotulagem.ProcessoRotulagemRepository;
import pt.acv.adega.processos.vindima.ProcessoVindimaRepository;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;
import pt.acv.adega.produtos.VinhoEngarrafado;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.math.BigDecimal;
import java.util.List;

/** Reune os indicadores e alertas mostrados no painel inicial. */
@Service
public class DashboardService {

    private final CastaRepository castaRepo;
    private final VinhaRepository vinhaRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final AdegaRepository adegaRepo;
    private final TalhaRepository talhaRepo;
    private final DepositoRepository depositoRepo;
    private final ConsumivelRepository consumivelRepo;
    private final MostoRepository mostoRepo;
    private final VinhoEngarrafadoRepository engarrafadoRepo;

    private final ProcessoAnaliseMaturacaoRepository maturacaoRepo;
    private final ProcessoVindimaRepository vindimaRepo;
    private final ProcessoMoagemRepository moagemRepo;
    private final ProcessoRemontagemRepository remontagemRepo;
    private final ProcessoAtestoRepository atestoRepo;
    private final ProcessoPassagemVinhoRepository passagemRepo;
    private final ProcessoEngarrafamentoRepository engRepo;
    private final ProcessoRotulagemRepository rotRepo;
    private final ProcessoPassagemComercialRepository comercialRepo;

    public DashboardService(CastaRepository castaRepo, VinhaRepository vinhaRepo,
                            TrabalhadorRepository trabalhadorRepo, AdegaRepository adegaRepo,
                            TalhaRepository talhaRepo, DepositoRepository depositoRepo,
                            ConsumivelRepository consumivelRepo, MostoRepository mostoRepo,
                            VinhoEngarrafadoRepository engarrafadoRepo,
                            ProcessoAnaliseMaturacaoRepository maturacaoRepo,
                            ProcessoVindimaRepository vindimaRepo, ProcessoMoagemRepository moagemRepo,
                            ProcessoRemontagemRepository remontagemRepo, ProcessoAtestoRepository atestoRepo,
                            ProcessoPassagemVinhoRepository passagemRepo, ProcessoEngarrafamentoRepository engRepo,
                            ProcessoRotulagemRepository rotRepo, ProcessoPassagemComercialRepository comercialRepo) {
        this.castaRepo = castaRepo;
        this.vinhaRepo = vinhaRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.adegaRepo = adegaRepo;
        this.talhaRepo = talhaRepo;
        this.depositoRepo = depositoRepo;
        this.consumivelRepo = consumivelRepo;
        this.mostoRepo = mostoRepo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.maturacaoRepo = maturacaoRepo;
        this.vindimaRepo = vindimaRepo;
        this.moagemRepo = moagemRepo;
        this.remontagemRepo = remontagemRepo;
        this.atestoRepo = atestoRepo;
        this.passagemRepo = passagemRepo;
        this.engRepo = engRepo;
        this.rotRepo = rotRepo;
        this.comercialRepo = comercialRepo;
    }

    public PainelDados carregar() {
        List<Talha> talhas = talhaRepo.findAll();
        long talhasOcupadas = talhas.stream().filter(t -> !t.isVazia()).count();
        List<Deposito> depositos = depositoRepo.findAll();
        long depositosOcupados = depositos.stream().filter(d -> !d.isVazia()).count();

        BigDecimal litrosMosto = mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.EM_FERMENTACAO).stream()
                .map(Mosto::getLitros).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal litrosGranel = mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL).stream()
                .map(Mosto::getLitros).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<VinhoEngarrafado> engarrafados = engarrafadoRepo.findAll();
        long garrafas = engarrafados.stream().mapToLong(VinhoEngarrafado::getNumeroGarrafas).sum();
        long disponiveis = engarrafados.stream().mapToLong(VinhoEngarrafado::getDisponiveis).sum();

        List<Consumivel> alertas = consumivelRepo.findAllByOrderByTipoAscDescricaoAsc().stream()
                .filter(Consumivel::isAbaixoMinimo).toList();

        long abertos = maturacaoRepo.countByEstado(EstadoProcesso.ABERTO)
                + vindimaRepo.countByEstado(EstadoProcesso.ABERTO)
                + moagemRepo.countByEstado(EstadoProcesso.ABERTO)
                + remontagemRepo.countByEstado(EstadoProcesso.ABERTO)
                + atestoRepo.countByEstado(EstadoProcesso.ABERTO)
                + passagemRepo.countByEstado(EstadoProcesso.ABERTO)
                + engRepo.countByEstado(EstadoProcesso.ABERTO)
                + rotRepo.countByEstado(EstadoProcesso.ABERTO)
                + comercialRepo.countByEstado(EstadoProcesso.ABERTO);

        return new PainelDados(
                castaRepo.count(), vinhaRepo.count(), trabalhadorRepo.count(), adegaRepo.count(),
                talhas.size(), depositos.size(), consumivelRepo.count(),
                talhasOcupadas, talhas.size() - talhasOcupadas,
                depositosOcupados, depositos.size() - depositosOcupados,
                litrosMosto, litrosGranel,
                garrafas, disponiveis,
                abertos, alertas);
    }
}
