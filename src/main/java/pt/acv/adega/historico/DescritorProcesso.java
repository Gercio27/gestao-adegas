package pt.acv.adega.historico;

import java.util.List;
import java.util.function.Supplier;

/**
 * O que o separador Historico precisa de saber sobre um processo: como se
 * chama, em que fase esta, como se carregam as linhas e que nome dar aos
 * campos genericos da LinhaHistorico.
 *
 * Os rotulos existem porque o mesmo campo quer dizer coisas diferentes de
 * processo para processo: "origem" e' a talha de origem na transfega mas o
 * contentor nas saidas de contentor.
 */
public record DescritorProcesso(
        String chave,
        String nome,
        String fase,
        String icone,
        String rotuloTipo,
        String rotuloAdega,
        String rotuloVinho,
        String rotuloOrigem,
        String rotuloDestino,
        String rotuloDetalhe,
        Supplier<List<LinhaHistorico>> carregar) {

    /** Descritor com os rotulos habituais; so' se indica o que foge a regra. */
    public static DescritorProcesso simples(String chave, String nome, String fase, String icone,
                                            Supplier<List<LinhaHistorico>> carregar) {
        return new DescritorProcesso(chave, nome, fase, icone,
                "Tipo", "Adega / armazém", "Vinho", "Origem", "Destino", "Detalhe", carregar);
    }

    public DescritorProcesso comRotulos(String tipo, String origem, String destino, String detalhe) {
        return new DescritorProcesso(chave, nome, fase, icone,
                tipo != null ? tipo : rotuloTipo,
                rotuloAdega,
                rotuloVinho,
                origem != null ? origem : rotuloOrigem,
                destino != null ? destino : rotuloDestino,
                detalhe != null ? detalhe : rotuloDetalhe,
                carregar);
    }
}
