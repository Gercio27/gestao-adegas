package pt.acv.adega.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Rede de segurança global: em vez de mostrar a página de erro 500 do Spring
 * (Whitelabel) quando uma eliminação viola integridade referencial (há registos
 * dependentes), devolve o utilizador à página anterior com uma mensagem clara.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String integridade(DataIntegrityViolationException ex, HttpServletRequest req, RedirectAttributes ra) {
        ra.addFlashAttribute("erro", "Não foi possível eliminar: este registo ainda tem outros que dependem dele "
                + "(ex.: moagens, vindimas, movimentos, engarrafamentos). Elimine primeiro esses registos dependentes "
                + "(nos processos fechados, reabra-os antes de eliminar).");
        // Volta à página anterior, mas só se for do próprio site (evita redireção externa).
        String ref = req.getHeader("Referer");
        String base = req.getScheme() + "://" + req.getServerName();
        boolean seguro = ref != null && (ref.startsWith("/") || ref.startsWith(base));
        return "redirect:" + (seguro ? ref : "/");
    }
}
