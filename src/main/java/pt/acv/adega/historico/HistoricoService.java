package pt.acv.adega.historico;

import org.springframework.stereotype.Service;
import pt.acv.adega.planeamento.LinhaPlaneamentoParcela;
import pt.acv.adega.planeamento.PlaneamentoVinho;
import pt.acv.adega.planeamento.PlaneamentoVinhoRepository;
import pt.acv.adega.planeamento.RegistoVindima;
import pt.acv.adega.processos.EstadoProcesso;
import pt.acv.adega.processos.Processo;
import pt.acv.adega.processos.atesto.*;
import pt.acv.adega.processos.certificacao.*;
import pt.acv.adega.processos.comercial.*;
import pt.acv.adega.processos.engarrafamento.*;
import pt.acv.adega.processos.loteamento.*;
import pt.acv.adega.processos.moagem.*;
import pt.acv.adega.processos.movimento.*;
import pt.acv.adega.processos.movimentovinho.*;
import pt.acv.adega.processos.passagem.*;
import pt.acv.adega.processos.remontagem.*;
import pt.acv.adega.processos.rotulagem.*;
import pt.acv.adega.processos.saidacontentor.*;
import pt.acv.adega.processos.vindima.*;
import pt.acv.adega.tratamentos.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converte cada processo para linhas de historico. Cada metodo e' pequeno de
 * proposito: so' diz de onde vem a data, o estado, o responsavel e o que
 * preencher nos campos genericos. O filtrar e o desenhar sao feitos noutro
 * sitio, iguais para todos.
 */
@Service
public class HistoricoService {

    private final ProcessoMoagemRepository moagemRepo;
    private final ProcessoRemontagemRepository remontagemRepo;
    private final ProcessoAtestoRepository atestoRepo;
    private final ProcessoMovimentoMostoRepository movMostoRepo;
    private final ProcessoPassagemVinhoRepository passagemRepo;
    private final ProcessoMovimentoVinhoRepository movVinhoRepo;
    private final LoteamentoRepository loteamentoRepo;
    private final LoteConstrucaoRepository construcaoRepo;
    private final LoteLinhaRepository loteLinhaRepo;
    private final pt.acv.adega.fichas.ContentorGarrafasRepository contentorRepo;
    private final pt.acv.adega.fichas.ContentorBagInBoxRepository bibRepo;
    private final ProcessoCertificacaoRepository certificacaoRepo;
    private final ProcessoEngarrafamentoRepository engarrafamentoRepo;
    private final ProcessoRotulagemRepository rotulagemRepo;
    private final ProcessoPassagemComercialRepository comercialRepo;
    private final SaidaContentorRepository saidaRepo;
    private final TratamentoEnologicoRepository tratamentoRepo;
    private final AnaliseVinhoRepository analiseRepo;
    private final ProcessoVindimaRepository vindimaRepo;
    private final PlaneamentoVinhoRepository planeamentoRepo;

    public HistoricoService(ProcessoMoagemRepository moagemRepo, ProcessoRemontagemRepository remontagemRepo,
                            ProcessoAtestoRepository atestoRepo, ProcessoMovimentoMostoRepository movMostoRepo,
                            ProcessoPassagemVinhoRepository passagemRepo,
                            ProcessoMovimentoVinhoRepository movVinhoRepo,
                            LoteamentoRepository loteamentoRepo, LoteConstrucaoRepository construcaoRepo,
                            LoteLinhaRepository loteLinhaRepo,
                            pt.acv.adega.fichas.ContentorGarrafasRepository contentorRepo,
                            pt.acv.adega.fichas.ContentorBagInBoxRepository bibRepo,
                            ProcessoCertificacaoRepository certificacaoRepo,
                            ProcessoEngarrafamentoRepository engarrafamentoRepo,
                            ProcessoRotulagemRepository rotulagemRepo,
                            ProcessoPassagemComercialRepository comercialRepo,
                            SaidaContentorRepository saidaRepo, TratamentoEnologicoRepository tratamentoRepo,
                            AnaliseVinhoRepository analiseRepo, ProcessoVindimaRepository vindimaRepo,
                            PlaneamentoVinhoRepository planeamentoRepo) {
        this.moagemRepo = moagemRepo;
        this.remontagemRepo = remontagemRepo;
        this.atestoRepo = atestoRepo;
        this.movMostoRepo = movMostoRepo;
        this.passagemRepo = passagemRepo;
        this.movVinhoRepo = movVinhoRepo;
        this.loteamentoRepo = loteamentoRepo;
        this.construcaoRepo = construcaoRepo;
        this.loteLinhaRepo = loteLinhaRepo;
        this.contentorRepo = contentorRepo;
        this.bibRepo = bibRepo;
        this.certificacaoRepo = certificacaoRepo;
        this.engarrafamentoRepo = engarrafamentoRepo;
        this.rotulagemRepo = rotulagemRepo;
        this.comercialRepo = comercialRepo;
        this.saidaRepo = saidaRepo;
        this.tratamentoRepo = tratamentoRepo;
        this.analiseRepo = analiseRepo;
        this.vindimaRepo = vindimaRepo;
        this.planeamentoRepo = planeamentoRepo;
    }

    /** Todos os processos que o separador Historico conhece, pela ordem das fases. */
    public Map<String, DescritorProcesso> descritores() {
        Map<String, DescritorProcesso> m = new LinkedHashMap<>();
        add(m, DescritorProcesso.simples("planeamento", "Planeamento dos vinhos", "Fase 1", "bi-clipboard-data",
                this::planeamento).comRotulos("Tipo de vinho", "Parcelas", null, "Kg / litros previstos"));
        add(m, DescritorProcesso.simples("vindima", "Vindima", "Fase 2", "bi-basket",
                this::vindima).comRotulos("Origem da uva", "Vinha / parcela", null, "Kg colhidos"));
        add(m, DescritorProcesso.simples("moagem", "Moagem", "Fase 3", "bi-gear",
                this::moagem).comRotulos("Castas", "Vindimas", "Recipientes cheios", "Moído / mosto"));
        add(m, DescritorProcesso.simples("remontagem", "Remontagem", "Fase 4.1", "bi-arrow-repeat",
                this::remontagem).comRotulos(null, "Talha / depósito", null, "Talhas feitas"));
        add(m, DescritorProcesso.simples("atesto", "Atesto", "Fase 4.2", "bi-droplet-half",
                this::atesto).comRotulos(null, "Recipiente de origem", "Recipiente de destino", "Litros"));
        add(m, DescritorProcesso.simples("movimento-mosto", "Entradas e saídas de mosto", "Fase 4.3", "bi-arrow-left-right",
                this::movimentoMosto).comRotulos("Entrada / saída", "Contraparte", "Talha / depósito", "Litros"));
        add(m, DescritorProcesso.simples("passagem", "Passagem a limpo", "Fase 4.4", "bi-funnel",
                this::passagem).comRotulos(null, "Mostos passados", null, "Litros"));
        add(m, DescritorProcesso.simples("movimento-vinho", "Entradas / saídas / transfegas de vinho", "Fase 5", "bi-truck",
                this::movimentoVinho).comRotulos("Tipo de movimento", "Origem", "Destino", "Quantidade"));
        add(m, DescritorProcesso.simples("loteamento-plano", "Planeamento dos lotes", "Fase 6.1", "bi-diagram-3",
                this::loteamentoPlano).comRotulos("Estado do lote", null, null, "Vinhos do lote"));
        add(m, DescritorProcesso.simples("loteamento-construcao", "Construção dos lotes", "Fase 6.2", "bi-diagram-2",
                this::loteamentoConstrucao).comRotulos(null, "Origem", "Destino", "Litros"));
        add(m, DescritorProcesso.simples("certificacao", "Certificação", "Fase 7", "bi-patch-check",
                this::certificacao).comRotulos("Alvo", null, null, "Resultado"));
        add(m, DescritorProcesso.simples("engarrafamento", "Engarrafamento", "Fase 8", "bi-bottle",
                this::engarrafamento).comRotulos("Embalagem", "Vinho a granel", "Contentores", "Garrafas / litros"));
        add(m, DescritorProcesso.simples("rotulagem", "Rotulagem e embalamento", "Fase 9", "bi-tags",
                this::rotulagem).comRotulos(null, null, null, "Caixas / garrafas"));
        add(m, DescritorProcesso.simples("comercial", "Comercial / notas de entrega", "Fase 10", "bi-box-seam",
                this::comercial).comRotulos("Embalagem", "Cliente", null, "Quantidade"));
        add(m, DescritorProcesso.simples("saida-contentor", "Saídas de contentor", "Transversal", "bi-box-arrow-up",
                this::saidaContentor).comRotulos("Motivo", "Contentor (sai de)", null, "Quantidade"));
        add(m, DescritorProcesso.simples("tratamento", "Tratamentos enológicos", "Transversal", "bi-eyedropper",
                this::tratamento).comRotulos("Fase do vinho", "Recipientes", null, "Tratamento"));
        add(m, DescritorProcesso.simples("analise", "Análises aos vinhos", "Transversal", "bi-clipboard-pulse",
                this::analise).comRotulos("Fase do vinho", "Recipientes", null, "Análise"));
        return m;
    }

    private void add(Map<String, DescritorProcesso> m, DescritorProcesso d) { m.put(d.chave(), d); }

    // ===================== conversores, um por processo =====================

    private List<LinhaHistorico> planeamento() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (PlaneamentoVinho p : planeamentoRepo.findAll()) {
            List<String> parcelas = new ArrayList<>();
            List<String> adegas = new ArrayList<>();
            for (LinhaPlaneamentoParcela l : p.getLinhas()) {
                if (l.getParcela() != null) parcelas.add(l.getParcela().getIdentificacao());
                if (l.getAdegaEntrega() != null && !adegas.contains(l.getAdegaEntrega().getNome())) {
                    adegas.add(l.getAdegaEntrega().getNome());
                }
            }
            out.add(LinhaHistorico.de(p.getCodigo(), p.getDataPlaneamento())
                    .tipo(p.getTipoVinho() != null ? p.getTipoVinho().getDescricao() : null)
                    .vinho(p.getNomeVinho())
                    .adega(String.join(", ", adegas))
                    .origem(String.join(", ", parcelas))
                    .detalhe(texto(p.getTotalKgAplicar()) + " kg previstos")
                    .url("/planeamento")
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> vindima() {
        List<LinhaHistorico> out = new ArrayList<>();
        // As colheitas ficam registadas nas linhas do planeamento (RegistoVindima).
        for (PlaneamentoVinho p : planeamentoRepo.findAll()) {
            for (LinhaPlaneamentoParcela l : p.getLinhas()) {
                for (RegistoVindima r : l.getVindimas()) {
                    out.add(LinhaHistorico.de(r.getCodigo(), r.getDataInicio())
                            .vinho(p.getNomeVinho())
                            .adega(l.getAdegaEntrega() != null ? "Adega " + l.getAdegaEntrega().getNome() : null)
                            .origem(l.getParcela() != null ? l.getParcela().getIdentificacao() : null)
                            .responsavel(nome(r.getResponsavel()))
                            .detalhe(texto(r.getQuantidadeKg()) + " kg")
                            .url("/processos/vindima")
                            .build());
                }
            }
        }
        for (ProcessoVindima v : vindimaRepo.findAll()) {
            out.add(base(v, v.getCodigo(), dia(v))
                    .tipo(v.getOrigem() != null ? v.getOrigem().getDescricao() : null)
                    .vinho(v.getCastaPrincipal() != null ? v.getCastaPrincipal().getNome() : null)
                    .adega(v.getAdegaDestino() != null ? "Adega " + v.getAdegaDestino().getNome() : null)
                    .origem(v.getVinha() != null ? v.getVinha().getNome() : null)
                    .detalhe(texto(v.getQuantidadeKg()) + " kg")
                    .url("/processos/vindima")
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> moagem() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (ProcessoMoagem m : moagemRepo.findAll()) {
            List<String> vindimas = new ArrayList<>();
            m.getVindimas().forEach(l -> vindimas.add(l.getEtiqueta()));
            out.add(base(m, m.getCodigo(), m.getDataDaMoagem())
                    .tipo(m.getCastasDescricao())
                    .vinho(m.getPlano() != null ? m.getPlano().getNomeVinho() : null)
                    .adega(m.getAdega() != null ? "Adega " + m.getAdega().getNome() : null)
                    .origem(String.join(", ", vindimas))
                    .destino(m.getRecipientesDescricao())
                    .detalhe(texto(m.getTotalMoidoKg()) + " kg · " + texto(m.getTotalLitrosMosto()) + " L")
                    .url("/processos/moagem")
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> remontagem() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (ProcessoRemontagem r : remontagemRepo.findAll()) {
            out.add(base(r, r.getCodigo(), dia(r))
                    .adega(r.getAdega() != null ? "Adega " + r.getAdega().getNome() : null)
                    .origem(r.getRecipientes())
                    .detalhe(r.getConcluidas() + " de " + r.getTalhas().size() + " talhas feitas")
                    .url("/processos/remontagem")
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> atesto() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (ProcessoAtesto a : atestoRepo.findAll()) {
            out.add(base(a, a.getCodigo(), dia(a))
                    .adega(a.getAdega() != null ? "Adega " + a.getAdega().getNome() : null)
                    .vinho(a.getVinhoNome())
                    .origem(recipiente(a.getTalhaOrigem(), a.getDepositoOrigem()))
                    .destino(recipiente(a.getTalhaDestino(), a.getDepositoDestino()))
                    .detalhe(texto(a.getLitros()) + " L")
                    .url("/processos/atesto/" + a.getId())
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> movimentoMosto() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (ProcessoMovimentoMosto p : movMostoRepo.findAll()) {
            String destino = p.getTipo() == TipoMovimento.ENTRADA
                    ? recipiente(p.getTalhaDestino(), p.getDepositoDestino())
                    : (p.getMostoOrigem() != null ? p.getMostoOrigem().getLocalizacao() : null);
            out.add(base(p, p.getCodigo(), dia(p))
                    .tipo(p.getTipo() != null ? p.getTipo().getDescricao() : null)
                    .vinho(p.getNomeVinho() != null ? p.getNomeVinho()
                            : (p.getMostoOrigem() != null ? p.getMostoOrigem().getVinhoNome() : null))
                    .adega(adegaDoRecipiente(p.getTalhaDestino(), p.getDepositoDestino(), p.getMostoOrigem()))
                    .origem(p.getContraparte())
                    .destino(destino)
                    .detalhe(texto(p.getLitros()) + " L"
                            + (p.getNumeroDA() != null ? " · DA " + p.getNumeroDA() : ""))
                    .url("/processos/movimento-mosto/" + p.getId())
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> passagem() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (ProcessoPassagemVinho p : passagemRepo.findAll()) {
            java.math.BigDecimal litros = java.math.BigDecimal.ZERO;
            List<String> adegas = new ArrayList<>();
            for (PassagemItem it : p.getItens()) {
                if (it.getLitrosEfetivos() != null) litros = litros.add(it.getLitrosEfetivos());
            }
            out.add(base(p, p.getCodigo(), dia(p))
                    .adega(String.join(", ", adegas))
                    .origem(p.getMostosDescricao())
                    .detalhe(texto(litros) + " L passados")
                    .url("/processos/passagem-vinho/" + p.getId())
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> movimentoVinho() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (ProcessoMovimentoVinho p : movVinhoRepo.findAll()) {
            String quantidade = p.isEngarrafado()
                    ? (p.getGarrafas() == null ? "0" : p.getGarrafas().toString()) + " " + p.getUnidadeNome()
                    : texto(p.getLitros()) + " L";
            // Na Fase 5 quem entrega e quem recebe contam tanto como o responsavel.
            String quem = nome(p.getResponsavel());
            if (quem == null) quem = p.getResponsavelEntrega();
            out.add(base(p, p.getCodigo(), dia(p))
                    .tipo(p.getTipo() != null ? p.getTipo().getDescricao() : null)
                    .vinho(p.getNomeVinho())
                    .adega(p.getDestinoLocalDescricao() != null
                            ? p.getDestinoLocalDescricao() : p.getOrigemLocalDescricao())
                    .responsavel(quem)
                    .origem(p.getOrigemLocalDescricao() != null ? p.getOrigemLocalDescricao()
                            : (p.getMostoOrigem() != null ? p.getMostoOrigem().getLocalizacao() : null))
                    .destino(p.getDestinoDescricao() != null ? p.getDestinoDescricao()
                            : recipiente(p.getTalhaDestino(), p.getDepositoDestino()))
                    .detalhe(quantidade + (p.getNumeroDA() != null ? " · DA " + p.getNumeroDA() : ""))
                    .url("/processos/movimento-vinho/" + p.getId())
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> loteamentoPlano() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (Loteamento l : loteamentoRepo.findAll()) {
            List<String> origens = new ArrayList<>();
            for (LoteLinha ln : loteLinhaRepo.findByLoteamentoId(l.getId())) {
                if (ln.getOrigemDescricao() != null && !origens.contains(ln.getOrigemDescricao())) {
                    origens.add(ln.getOrigemDescricao());
                }
            }
            out.add(LinhaHistorico.de(l.getCodigo(), l.getDataPlaneamento())
                    .estado(l.isConcluido() ? "Fechado" : "Aberto")
                    .tipo(l.isConcluido() ? "Concluído" : "Em construção")
                    .vinho(l.getNome())
                    .adega(l.getLocalNome())
                    .responsavel(l.getCriadoPor())
                    .detalhe(origens.isEmpty() ? null : String.join(" · ", origens))
                    .url("/processos/loteamento")
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> loteamentoConstrucao() {
        Map<Long, Loteamento> lotes = new LinkedHashMap<>();
        loteamentoRepo.findAll().forEach(l -> lotes.put(l.getId(), l));
        List<LinhaHistorico> out = new ArrayList<>();
        for (LoteConstrucao c : construcaoRepo.findAll()) {
            Loteamento l = lotes.get(c.getLoteamentoId());
            out.add(LinhaHistorico.de(l != null ? l.getCodigo() + " · nº " + c.getNumero() : "nº " + c.getNumero(),
                            c.getData() != null ? c.getData().toLocalDate() : null)
                    .estado(l != null && l.isConcluido() ? "Fechado" : "Aberto")
                    .vinho(l != null ? l.getNome() : null)
                    .adega(l != null ? l.getLocalNome() : null)
                    .responsavel(l != null ? l.getCriadoPor() : null)
                    .origem(c.getOrigemDescricao())
                    .destino(c.getDestinoDescricao())
                    .detalhe(texto(c.getLitros()) + " L")
                    .url("/processos/loteamento")
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> certificacao() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (ProcessoCertificacao c : certificacaoRepo.findAll()) {
            String vinho = c.getEngarrafado() != null ? c.getEngarrafado().getNome()
                    : (c.getVinhoGranel() != null ? c.getVinhoGranel().getVinhoNome() : null);
            out.add(base(c, c.getCodigo(), dia(c))
                    .tipo(c.getAlvo() != null ? c.getAlvo().getDescricao() : null)
                    .vinho(vinho)
                    .origem(c.getAlvoDescricao())
                    .detalhe((c.getResultado() != null ? c.getResultado().getDescricao() : "Sem resultado")
                            + (c.getNumeroCertificado() != null ? " · " + c.getNumeroCertificado() : ""))
                    .url("/processos/certificacao/" + c.getId())
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> engarrafamento() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (ProcessoEngarrafamento e : engarrafamentoRepo.findAll()) {
            out.add(base(e, e.getCodigo(), dia(e))
                    .tipo(e.getTipoEmbalagem() != null ? e.getTipoEmbalagem().getDescricao() : null)
                    .vinho(e.getNomeVinho())
                    .adega(localDoEngarrafamento(e))
                    .origem(e.getVinhoGranel() != null ? e.getVinhoGranel().getCodigo() : null)
                    .destino(e.getContentoresDescricao())
                    .detalhe(e.getNumeroGarrafas() + " " + e.getUnidadeNome()
                            + " · " + texto(e.getLitrosUtilizados()) + " L")
                    .url("/processos/engarrafamento/" + e.getId())
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> rotulagem() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (ProcessoRotulagem r : rotulagemRepo.findAll()) {
            out.add(base(r, r.getCodigo(), dia(r))
                    .vinho(r.getEngarrafado() != null ? r.getEngarrafado().getNome() : null)
                    .adega(r.getLocalNome())
                    .detalhe(r.getCaixasRotuladas() + " caixa(s) · " + r.getNumeroGarrafas() + " garrafas")
                    .url("/processos/rotulagem/" + r.getId())
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> comercial() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (ProcessoPassagemComercial p : comercialRepo.findAll()) {
            // O expedidor e' o responsavel do processo; a rececao e' do lado do cliente.
            out.add(base(p, p.getCodigo(), dia(p))
                    .tipo(p.getTipoEmbalagem() != null ? p.getTipoEmbalagem().getDescricao() : null)
                    .vinho(p.getEngarrafado() != null ? p.getEngarrafado().getNome() : null)
                    .adega(p.getOrigemDescricao())
                    .origem(p.getDestinatario())
                    .detalhe(p.getQuantidadeGarrafas() + " " + p.getUnidadeNome()
                            + (p.getNumeroNota() != null ? " · Nota " + p.getNumeroNota() : ""))
                    .url("/processos/comercial/" + p.getId())
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> saidaContentor() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (SaidaContentor s : saidaRepo.findAll()) {
            out.add(LinhaHistorico.de(s.getCodigo(),
                            s.getDataSaida() != null ? s.getDataSaida().toLocalDate() : null)
                    .tipo(s.getMotivo() != null ? s.getMotivo().getDescricao() : null)
                    .vinho(s.getVinhoNome())
                    .responsavel(s.getCriadoPor())
                    .origem(s.getContentorNome())
                    .detalhe(s.getQuantidade() + " " + s.getUnidadeNome()
                            + (s.getObservacao() != null ? " · " + s.getObservacao() : ""))
                    .url("/processos/saida-contentor")
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> tratamento() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (TratamentoEnologico t : tratamentoRepo.findAll()) {
            out.add(LinhaHistorico.de(t.getCodigo(), t.getDataTratamento())
                    .tipo(t.getCategoria() != null ? t.getCategoria().getDescricao() : null)
                    .vinho(t.getVinhoNome())
                    .adega(t.getLocalNome())
                    .responsavel(t.getCriadoPor())
                    .origem(t.getRecipientesDescricao())
                    .detalhe(t.getDescricao())
                    .url("/tratamentos")
                    .build());
        }
        return ordenar(out);
    }

    private List<LinhaHistorico> analise() {
        List<LinhaHistorico> out = new ArrayList<>();
        for (AnaliseVinho a : analiseRepo.findAll()) {
            out.add(LinhaHistorico.de(a.getCodigo(), a.getDataAnalise())
                    .tipo(a.getCategoria() != null ? a.getCategoria().getDescricao() : null)
                    .vinho(a.getVinhoNome())
                    .adega(a.getLocalNome())
                    .responsavel(a.getCriadoPor())
                    .origem(a.getRecipientesDescricao())
                    .detalhe(a.getDescricao())
                    .url("/analises")
                    .build());
        }
        return ordenar(out);
    }

    // ===================== auxiliares =====================

    /** Parte comum a todos os processos: estado, responsavel e quem o criou. */
    private LinhaHistorico.Builder base(Processo p, String codigo, LocalDate data) {
        String quem = nome(p.getResponsavel());
        return LinhaHistorico.de(codigo, data)
                .estado(p.getEstado() == EstadoProcesso.FECHADO ? "Fechado" : "Aberto")
                .responsavel(quem != null ? quem : p.getCriadoPor());
    }

    /** Data que conta para o historico: a de inicio, senao a de criacao. */
    private LocalDate dia(Processo p) {
        if (p.getDataHoraInicio() != null) return p.getDataHoraInicio().toLocalDate();
        LocalDateTime c = ((pt.acv.adega.common.BaseEntity) p).getDataCriacao();
        return c != null ? c.toLocalDate() : null;
    }

    private String nome(pt.acv.adega.fichas.Trabalhador t) { return t == null ? null : t.getNome(); }

    private String recipiente(pt.acv.adega.fichas.Talha t, pt.acv.adega.fichas.Deposito d) {
        if (t != null) return "Talha " + t.getIdentificacao();
        if (d != null) return "Depósito " + d.getIdentificacao();
        return null;
    }

    /** Adega onde a coisa aconteceu, seja pelo recipiente de destino ou de origem. */
    private String adegaDoRecipiente(pt.acv.adega.fichas.Talha t, pt.acv.adega.fichas.Deposito d,
                                     pt.acv.adega.produtos.Mosto origem) {
        if (t != null && t.getAdega() != null) return "Adega " + t.getAdega().getNome();
        if (d != null) return d.getLocalizacao();
        if (origem != null) {
            if (origem.getTalha() != null && origem.getTalha().getAdega() != null) {
                return "Adega " + origem.getTalha().getAdega().getNome();
            }
            if (origem.getDeposito() != null) return origem.getDeposito().getLocalizacao();
        }
        return null;
    }

    /**
     * Adega/armazem de um engarrafamento. O processo nao guarda o local: ele
     * esta' nos contentores que foram cheios, indicados na distribuicao
     * "contentorId:qtd;contentorId:qtd".
     */
    private String localDoEngarrafamento(ProcessoEngarrafamento e) {
        String csv = e.getDistribuicaoContentores();
        if (csv == null || csv.isBlank()) return null;
        java.util.TreeSet<String> locais = new java.util.TreeSet<>();
        for (String par : csv.split(";")) {
            String[] kv = par.split(":");
            if (kv.length != 2) continue;
            Long id;
            try { id = Long.valueOf(kv[0].trim()); } catch (Exception ex) { continue; }
            if (e.isBagInBox()) {
                bibRepo.findById(id).ifPresent(c -> locais.add(c.getLocalizacao()));
            } else {
                contentorRepo.findById(id).ifPresent(c -> locais.add(c.getLocalizacao()));
            }
        }
        return locais.isEmpty() ? null : String.join(", ", locais);
    }

    private String texto(java.math.BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    /** Mais recentes primeiro; as linhas sem data vao para o fim. */
    private List<LinhaHistorico> ordenar(List<LinhaHistorico> linhas) {
        linhas.sort(Comparator.comparing(LinhaHistorico::data,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return linhas;
    }
}
