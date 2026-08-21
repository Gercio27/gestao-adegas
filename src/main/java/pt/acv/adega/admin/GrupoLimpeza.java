package pt.acv.adega.admin;

import java.util.List;

/**
 * Um bloco de dados que se pode mandar apagar na pagina de limpeza.
 *
 * @param id         identificador usado no formulario (nunca vem do utilizador para dentro do SQL)
 * @param nome       o que aparece ao utilizador
 * @param descricao  uma linha a dizer o que leva atras
 * @param tabelas    tabelas a apagar, <b>filhas primeiro</b>
 * @param prefixos   prefixos de codigo cujo contador volta ao 1 (ex.: "MOA")
 * @param encadeado  true quando faz parte da cadeia do processo produtivo: apagar
 *                   um destes obriga a apagar todos os que vem depois, porque o
 *                   que vem depois nasceu deste. Os que estao a false (analises,
 *                   historico de stock) podem ser apagados sozinhos.
 */
public record GrupoLimpeza(String id, String nome, String descricao,
                           List<String> tabelas, List<String> prefixos, boolean encadeado) {
}
