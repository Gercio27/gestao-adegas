package pt.acv.adega.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Rede de seguranca global: em vez da pagina de erro 500 do Spring, devolve o
 * utilizador a pagina anterior com uma mensagem que diz o que se passou.
 *
 * <p>Distingue os dois casos, que nao tem nada a ver um com o outro: apagar
 * algo de que outros registos dependem, e gravar algo que choca com um valor
 * unico. Antes chamava "eliminacao" a tudo — criar uma casta dava o erro
 * "nao foi possivel eliminar", que nao ajuda ninguem.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String integridade(DataIntegrityViolationException ex, HttpServletRequest req, RedirectAttributes ra) {
        // Sem isto, a causa real fica sem rasto nenhum e nao ha' como investigar.
        log.warn("Violação de integridade em {} {}", req.getMethod(), req.getRequestURI(), ex);

        ra.addFlashAttribute("erro", duplicado(ex)
                ? "Não foi possível guardar: já existe um registo com esse código ou com um valor que tem de ser único. "
                        + "Tente outra vez — se voltar a acontecer, avise o administrador."
                : "Não foi possível eliminar: este registo ainda tem outros que dependem dele "
                        + "(ex.: moagens, vindimas, movimentos, engarrafamentos). Elimine primeiro esses registos dependentes "
                        + "(nos processos fechados, reabra-os antes de eliminar).");

        // Volta à página anterior, mas só se for do próprio site (evita redireção externa).
        String ref = req.getHeader("Referer");
        String base = req.getScheme() + "://" + req.getServerName();
        boolean seguro = ref != null && (ref.startsWith("/") || ref.startsWith(base));
        return "redirect:" + (seguro ? ref : "/");
    }

    /** Choque com um valor unico (codigo repetido), e nao uma dependencia. */
    private boolean duplicado(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = ex; t != null && sb.length() < 4000; t = t.getCause()) {
            if (t.getMessage() != null) sb.append(t.getMessage()).append(' ');
            if (t.getCause() == t) break;
        }
        String texto = sb.toString().toLowerCase();
        return texto.contains("unique") || texto.contains("duplicate") || texto.contains("duplicad");
    }
}
