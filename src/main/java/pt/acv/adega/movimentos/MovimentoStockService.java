package pt.acv.adega.movimentos;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.acv.adega.fichas.*;
import pt.acv.adega.produtos.StockRotulado;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ponto unico por onde os processos registam o que entrou e saiu de cada
 * ficha. Os metodos recebem ja a entidade, para o historico guardar o nome e o
 * local sem quem chama ter de os montar.
 */
@Service
public class MovimentoStockService {

    private final MovimentoStockRepository repo;

    public MovimentoStockService(MovimentoStockRepository repo) {
        this.repo = repo;
    }

    // ----- recipientes de granel -----

    public void talha(Talha t, BigDecimal litros, String origem, String descricao, String vinho) {
        if (t == null || nulo(litros)) return;
        registar(TipoAlvo.TALHA, t.getId(), "Talha " + t.getIdentificacao(),
                t.getAdega() != null ? "Adega " + t.getAdega().getNome() : null,
                litros, "L", t.getVolumeAtualLitros(), origem, descricao, vinho);
    }

    public void deposito(Deposito d, BigDecimal litros, String origem, String descricao, String vinho) {
        if (d == null || nulo(litros)) return;
        registar(TipoAlvo.DEPOSITO, d.getId(), "Depósito " + d.getIdentificacao(), d.getLocalizacao(),
                litros, "L", d.getVolumeAtualLitros(), origem, descricao, vinho);
    }

    /** Atalho: escreve na talha ou no deposito, conforme o que vier preenchido. */
    public void recipiente(Talha t, Deposito d, BigDecimal litros, String origem, String descricao, String vinho) {
        if (t != null) talha(t, litros, origem, descricao, vinho);
        else deposito(d, litros, origem, descricao, vinho);
    }

    // ----- produto acabado -----

    public void contentor(ContentorGarrafas c, int garrafas, String origem, String descricao, String vinho) {
        if (c == null || garrafas == 0) return;
        registar(TipoAlvo.CONTENTOR_GARRAFAS, c.getId(), c.getNome(), c.getLocalizacao(),
                BigDecimal.valueOf(garrafas), "garrafas", BigDecimal.valueOf(c.getGarrafasAtuais()),
                origem, descricao, vinho != null ? vinho : c.getVinhoNome());
    }

    public void palete(ContentorBagInBox c, int unidades, String origem, String descricao, String vinho) {
        if (c == null || unidades == 0) return;
        registar(TipoAlvo.PALETE_BIB, c.getId(), c.getNome(), c.getLocalizacao(),
                BigDecimal.valueOf(unidades), "unidades", BigDecimal.valueOf(c.getUnidadesAtuais()),
                origem, descricao, vinho != null ? vinho : c.getVinhoNome());
    }

    public void consumivel(Consumivel c, int quantidade, String origem, String descricao) {
        if (c == null || quantidade == 0) return;
        registar(TipoAlvo.CONSUMIVEL, c.getId(), c.getDescricao(), c.getTipo().getDescricao(),
                BigDecimal.valueOf(quantidade), c.getUnidade() != null ? c.getUnidade() : "un",
                BigDecimal.valueOf(c.getStock()), origem, descricao, null);
    }

    public void rotulado(StockRotulado s, int garrafas, String origem, String descricao) {
        if (s == null || garrafas == 0) return;
        registar(TipoAlvo.STOCK_ROTULADO, s.getId(), s.getVinhoNome(), s.getLocalNome(),
                BigDecimal.valueOf(garrafas), "garrafas", BigDecimal.valueOf(s.getGarrafas()),
                origem, descricao, s.getVinhoNome());
    }

    // ----- base -----

    private void registar(TipoAlvo tipo, Long alvoId, String alvoNome, String local,
                          BigDecimal quantidade, String unidade, BigDecimal saldo,
                          String origem, String descricao, String vinho) {
        if (alvoId == null) return;
        MovimentoStock m = new MovimentoStock();
        m.setTipoAlvo(tipo);
        m.setAlvoId(alvoId);
        m.setAlvoNome(alvoNome);
        m.setLocal(local);
        m.setQuantidade(quantidade);
        m.setUnidade(unidade);
        m.setSaldo(saldo);
        m.setOrigem(origem);
        m.setDescricao(descricao);
        m.setVinhoNome(vinho);
        m.setDataHora(LocalDateTime.now());
        m.setUtilizador(utilizadorAtual());
        repo.save(m);
    }

    private boolean nulo(BigDecimal v) { return v == null || v.signum() == 0; }

    private String utilizadorAtual() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
