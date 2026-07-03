package pt.acv.adega.web;

import java.time.LocalDateTime;

/** Linha generica para a listagem de fichas por datas. */
public record LinhaListagem(String codigo, String descricao, String detalhe, LocalDateTime dataCriacao) {
}
