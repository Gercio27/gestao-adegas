package pt.acv.adega.historico;

import java.time.LocalDate;

/**
 * Uma linha do historico, seja de que processo for. Todos os processos sao
 * convertidos para esta forma comum, para o filtro e a tabela serem escritos
 * uma vez so' em vez de dezassete vezes.
 *
 * Os campos genericos (tipo, adega, vinho, origem, destino) sao usados de
 * maneira diferente conforme o processo — por exemplo, "tipo" e' Entrada/Saida
 * na Fase 4.3, o alvo na Certificacao e o motivo nas saidas de contentor. O
 * nome que aparece ao utilizador vem do descritor de cada processo.
 *
 * Um campo a null quer dizer "este processo nao tem isto": nem a coluna nem o
 * filtro correspondentes aparecem.
 */
public record LinhaHistorico(
        String codigo,
        LocalDate data,
        String estado,
        String tipo,
        String adega,
        String vinho,
        String responsavel,
        String origem,
        String destino,
        String detalhe,
        String url) {

    /** Construtor de conveniencia: so' o essencial, o resto a null. */
    public static Builder de(String codigo, LocalDate data) {
        return new Builder(codigo, data);
    }

    public boolean isFechado() { return "Fechado".equals(estado); }

    public static final class Builder {
        private final String codigo;
        private final LocalDate data;
        private String estado, tipo, adega, vinho, responsavel, origem, destino, detalhe, url;

        private Builder(String codigo, LocalDate data) {
            this.codigo = codigo;
            this.data = data;
        }

        public Builder estado(String v) { this.estado = vazioParaNulo(v); return this; }
        public Builder tipo(String v) { this.tipo = vazioParaNulo(v); return this; }
        public Builder adega(String v) { this.adega = vazioParaNulo(v); return this; }
        public Builder vinho(String v) { this.vinho = vazioParaNulo(v); return this; }
        public Builder responsavel(String v) { this.responsavel = vazioParaNulo(v); return this; }
        public Builder origem(String v) { this.origem = vazioParaNulo(v); return this; }
        public Builder destino(String v) { this.destino = vazioParaNulo(v); return this; }
        public Builder detalhe(String v) { this.detalhe = vazioParaNulo(v); return this; }
        public Builder url(String v) { this.url = v; return this; }

        public LinhaHistorico build() {
            return new LinhaHistorico(codigo, data, estado, tipo, adega, vinho,
                    responsavel, origem, destino, detalhe, url);
        }

        /** "—" e "" contam como vazio: nao devem virar opcao de filtro. */
        private static String vazioParaNulo(String v) {
            if (v == null) return null;
            String t = v.trim();
            return (t.isEmpty() || "—".equals(t) || "-".equals(t)) ? null : t;
        }
    }
}
