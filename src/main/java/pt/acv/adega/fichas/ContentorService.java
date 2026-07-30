package pt.acv.adega.fichas;

import org.springframework.stereotype.Service;
import pt.acv.adega.movimentos.MovimentoStockService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Um sitio so' para lidar com contentores de produto acabado, sejam de
 * garrafas ou de bag-in-box. As fases (certificacao, comercial, saidas,
 * movimentos) passam por aqui em vez de repetirem o mesmo "if garrafas senao
 * bag-in-box" cada uma a sua maneira.
 */
@Service
public class ContentorService {

    private final ContentorGarrafasRepository garrafasRepo;
    private final ContentorBagInBoxRepository bibRepo;
    private final MovimentoStockService movimentos;

    public ContentorService(ContentorGarrafasRepository garrafasRepo, ContentorBagInBoxRepository bibRepo,
                            MovimentoStockService movimentos) {
        this.garrafasRepo = garrafasRepo;
        this.bibRepo = bibRepo;
        this.movimentos = movimentos;
    }

    /** Um contentor visto de fora, sem interessar de que tipo e'. */
    public record Opcao(Long id, String nome, String label, int stock, String vinhoNome, boolean rotulado) { }

    public List<Opcao> todos(TipoEmbalagem tipo) {
        List<Opcao> out = new ArrayList<>();
        if (tipo == TipoEmbalagem.BAG_IN_BOX) {
            for (ContentorBagInBox c : bibRepo.findAllByOrderByNomeAsc()) out.add(paraOpcao(c));
        } else {
            for (ContentorGarrafas c : garrafasRepo.findAllByOrderByNomeAsc()) out.add(paraOpcao(c));
        }
        return out;
    }

    /** So' os que tem alguma coisa la dentro. */
    public List<Opcao> comStock(TipoEmbalagem tipo) {
        return todos(tipo).stream().filter(o -> o.stock() > 0).toList();
    }

    /** So' os que ja passaram pela rotulagem e tem stock (prontos para o comercial). */
    public List<Opcao> rotuladosComStock(TipoEmbalagem tipo) {
        return todos(tipo).stream().filter(o -> o.stock() > 0 && o.rotulado()).toList();
    }

    public Opcao procurar(TipoEmbalagem tipo, Long id) {
        if (id == null) return null;
        if (tipo == TipoEmbalagem.BAG_IN_BOX) {
            return bibRepo.findById(id).map(this::paraOpcao).orElse(null);
        }
        return garrafasRepo.findById(id).map(this::paraOpcao).orElse(null);
    }

    public int stock(TipoEmbalagem tipo, Long id) {
        Opcao o = procurar(tipo, id);
        return o == null ? 0 : o.stock();
    }

    /** Id do produto acabado que ocupa o contentor, se algum. */
    public Long vinhoDe(TipoEmbalagem tipo, Long id) {
        if (id == null) return null;
        if (tipo == TipoEmbalagem.BAG_IN_BOX) {
            return bibRepo.findById(id).map(ContentorBagInBox::getVinhoEmbaladoId).orElse(null);
        }
        return garrafasRepo.findById(id).map(ContentorGarrafas::getVinhoEngarrafadoId).orElse(null);
    }

    /**
     * Soma 'delta' unidades ao contentor (negativo para dar baixa). Nunca fica
     * negativo e, quando chega a zero, esquece o vinho que la estava.
     */
    public void ajustar(TipoEmbalagem tipo, Long id, int delta) {
        if (id == null || delta == 0) return;
        if (tipo == TipoEmbalagem.BAG_IN_BOX) {
            ContentorBagInBox c = bibRepo.findById(id).orElse(null);
            if (c == null) return;
            c.setUnidadesAtuais(Math.max(0, c.getUnidadesAtuais() + delta));
            if (c.getUnidadesAtuais() == 0) { c.setVinhoEmbaladoId(null); c.setVinhoNome(null); c.setRotulado(false); }
            bibRepo.save(c);
        } else {
            ContentorGarrafas c = garrafasRepo.findById(id).orElse(null);
            if (c == null) return;
            c.setGarrafasAtuais(Math.max(0, c.getGarrafasAtuais() + delta));
            if (c.getGarrafasAtuais() == 0) { c.setVinhoEngarrafadoId(null); c.setVinhoNome(null); c.setRotulado(false); }
            garrafasRepo.save(c);
        }
    }

    /**
     * Move unidades de um contentor para outro do mesmo tipo, levando o vinho
     * e o estado de rotulagem consigo. O destino fica com o vinho da origem.
     */
    public void transferir(TipoEmbalagem tipo, Long origemId, Long destinoId, int quantidade) {
        if (origemId == null || destinoId == null || quantidade <= 0) return;
        if (tipo == TipoEmbalagem.BAG_IN_BOX) {
            ContentorBagInBox o = bibRepo.findById(origemId).orElse(null);
            ContentorBagInBox d = bibRepo.findById(destinoId).orElse(null);
            if (o == null || d == null) return;
            o.setUnidadesAtuais(Math.max(0, o.getUnidadesAtuais() - quantidade));
            d.setUnidadesAtuais(d.getUnidadesAtuais() + quantidade);
            d.setVinhoEmbaladoId(o.getVinhoEmbaladoId());
            d.setVinhoNome(o.getVinhoNome());
            d.setRotulado(o.isRotulado());
            // A origem só esquece o vinho depois de ficar mesmo vazia.
            if (o.getUnidadesAtuais() == 0) { o.setVinhoEmbaladoId(null); o.setVinhoNome(null); o.setRotulado(false); }
            bibRepo.save(o);
            bibRepo.save(d);
        } else {
            ContentorGarrafas o = garrafasRepo.findById(origemId).orElse(null);
            ContentorGarrafas d = garrafasRepo.findById(destinoId).orElse(null);
            if (o == null || d == null) return;
            Long vinho = o.getVinhoEngarrafadoId();
            String nome = o.getVinhoNome();
            boolean rotulado = o.isRotulado();
            o.setGarrafasAtuais(Math.max(0, o.getGarrafasAtuais() - quantidade));
            d.setGarrafasAtuais(d.getGarrafasAtuais() + quantidade);
            d.setVinhoEngarrafadoId(vinho);
            d.setVinhoNome(nome);
            d.setRotulado(rotulado);
            if (o.getGarrafasAtuais() == 0) { o.setVinhoEngarrafadoId(null); o.setVinhoNome(null); o.setRotulado(false); }
            garrafasRepo.save(o);
            garrafasRepo.save(d);
        }
    }

    /**
     * O mesmo que ajustar(), mas deixando rasto no historico da ficha. E' esta a
     * versao que as fases devem usar, dizendo de onde vem o movimento.
     */
    public void ajustar(TipoEmbalagem tipo, Long id, int delta, String origem, String descricao) {
        String vinho = nomeDoVinho(tipo, id);
        ajustar(tipo, id, delta);
        registar(tipo, id, delta, origem, descricao, vinho);
    }

    /** O mesmo que transferir(), deixando rasto nos dois contentores. */
    public void transferir(TipoEmbalagem tipo, Long origemId, Long destinoId, int quantidade,
                           String origem, String descricao) {
        String vinho = nomeDoVinho(tipo, origemId);
        String nomeOrigem = nomeDoContentor(tipo, origemId);
        String nomeDestino = nomeDoContentor(tipo, destinoId);
        transferir(tipo, origemId, destinoId, quantidade);
        registar(tipo, origemId, -quantidade, origem, descricao + " — saída para " + nomeDestino, vinho);
        registar(tipo, destinoId, quantidade, origem, descricao + " — entrada vinda de " + nomeOrigem, vinho);
    }

    /** Escreve a linha no historico, ja com o contentor no estado final. */
    private void registar(TipoEmbalagem tipo, Long id, int delta, String origem, String descricao, String vinho) {
        if (id == null || delta == 0) return;
        if (tipo == TipoEmbalagem.BAG_IN_BOX) {
            bibRepo.findById(id).ifPresent(c -> movimentos.palete(c, delta, origem, descricao, vinho));
        } else {
            garrafasRepo.findById(id).ifPresent(c -> movimentos.contentor(c, delta, origem, descricao, vinho));
        }
    }

    /** Nome do vinho antes do movimento (depois de esvaziar, o contentor esquece-o). */
    private String nomeDoVinho(TipoEmbalagem tipo, Long id) {
        if (id == null) return null;
        if (tipo == TipoEmbalagem.BAG_IN_BOX) {
            return bibRepo.findById(id).map(ContentorBagInBox::getVinhoNome).orElse(null);
        }
        return garrafasRepo.findById(id).map(ContentorGarrafas::getVinhoNome).orElse(null);
    }

    private String nomeDoContentor(TipoEmbalagem tipo, Long id) {
        Opcao o = procurar(tipo, id);
        return o == null ? "?" : o.nome();
    }

    /** Opções agrupadas pelo local (armazém/adega) onde o contentor está. */
    public Map<String, List<Opcao>> porLocal(TipoEmbalagem tipo, boolean apenasComStock) {
        Map<String, List<Opcao>> out = new LinkedHashMap<>();
        if (tipo == TipoEmbalagem.BAG_IN_BOX) {
            for (ContentorBagInBox c : bibRepo.findAllByOrderByNomeAsc()) {
                if (apenasComStock && c.getUnidadesAtuais() <= 0) continue;
                String ref = localRef(c.getArmazem(), c.getAdega());
                if (ref != null) out.computeIfAbsent(ref, k -> new ArrayList<>()).add(paraOpcao(c));
            }
        } else {
            for (ContentorGarrafas c : garrafasRepo.findAllByOrderByNomeAsc()) {
                if (apenasComStock && c.getGarrafasAtuais() <= 0) continue;
                String ref = localRef(c.getArmazem(), c.getAdega());
                if (ref != null) out.computeIfAbsent(ref, k -> new ArrayList<>()).add(paraOpcao(c));
            }
        }
        return out;
    }

    /** Nome legível do local de cada contentor, por referência. */
    public Map<String, String> nomesDosLocais(TipoEmbalagem tipo) {
        Map<String, String> out = new LinkedHashMap<>();
        if (tipo == TipoEmbalagem.BAG_IN_BOX) {
            for (ContentorBagInBox c : bibRepo.findAllByOrderByNomeAsc()) {
                String ref = localRef(c.getArmazem(), c.getAdega());
                if (ref != null) out.putIfAbsent(ref, c.getLocalizacao());
            }
        } else {
            for (ContentorGarrafas c : garrafasRepo.findAllByOrderByNomeAsc()) {
                String ref = localRef(c.getArmazem(), c.getAdega());
                if (ref != null) out.putIfAbsent(ref, c.getLocalizacao());
            }
        }
        return out;
    }

    /** Local onde está um contentor concreto ("ARMAZEM:id" / "ADEGA:id"). */
    public String localDe(TipoEmbalagem tipo, Long id) {
        if (id == null) return "";
        if (tipo == TipoEmbalagem.BAG_IN_BOX) {
            ContentorBagInBox c = bibRepo.findById(id).orElse(null);
            return c == null ? "" : nuloParaVazio(localRef(c.getArmazem(), c.getAdega()));
        }
        ContentorGarrafas c = garrafasRepo.findById(id).orElse(null);
        return c == null ? "" : nuloParaVazio(localRef(c.getArmazem(), c.getAdega()));
    }

    // ----- auxiliares -----

    private String localRef(Armazem armazem, Adega adega) {
        if (armazem != null) return "ARMAZEM:" + armazem.getId();
        if (adega != null) return "ADEGA:" + adega.getId();
        return null;
    }

    private String nuloParaVazio(String s) { return s == null ? "" : s; }

    private Opcao paraOpcao(ContentorGarrafas c) {
        String vinho = c.getGarrafasAtuais() > 0 ? c.getVinhoNome() : null;
        String label = c.getNome() + " · " + c.getLocalizacao()
                + (c.getGarrafasAtuais() > 0
                    ? " · " + (vinho != null ? vinho : "vinho") + " · " + c.getGarrafasAtuais() + " garrafas"
                    : " · vazio (cabem " + c.getCapacidadeGarrafas() + ")");
        return new Opcao(c.getId(), c.getNome(), label, c.getGarrafasAtuais(), vinho, c.isRotulado());
    }

    private Opcao paraOpcao(ContentorBagInBox c) {
        String vinho = c.getUnidadesAtuais() > 0 ? c.getVinhoNome() : null;
        String label = c.getNome() + " · " + c.getTipoBag().getDescricao() + " · " + c.getLocalizacao()
                + (c.getUnidadesAtuais() > 0
                    ? " · " + (vinho != null ? vinho : "vinho") + " · " + c.getUnidadesAtuais() + " unidades ("
                        + c.getLitrosAtuais().toPlainString() + " L)"
                    : " · vazio (cabem " + c.getCapacidadeUnidades() + ")");
        return new Opcao(c.getId(), c.getNome(), label, c.getUnidadesAtuais(), vinho, c.isRotulado());
    }
}
