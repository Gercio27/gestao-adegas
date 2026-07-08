package pt.acv.adega.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Regista, para efeitos de auditoria, todas as alteracoes feitas na aplicacao
 * (pedidos POST/PUT/DELETE/PATCH), identificando o utilizador autenticado, a
 * accao (metodo + caminho) e o resultado. As linhas sao escritas no logger
 * "AUDITORIA", encaminhado para o ficheiro logs/auditoria.log (ver
 * logback-spring.xml).
 */
@Component
public class AuditoriaInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger("AUDITORIA");

    /** Metodos HTTP que alteram dados e por isso interessa auditar. */
    private static final Set<String> METODOS_ALTERACAO = Set.of("POST", "PUT", "DELETE", "PATCH");

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!METODOS_ALTERACAO.contains(request.getMethod())) return;

        String uri = request.getRequestURI();
        // Ignora autenticacao/estaticos (nao sao alteracoes de dados de negocio).
        if (uri.startsWith("/login") || uri.startsWith("/logout")
                || uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/webjars/") || uri.startsWith("/img/")) {
            return;
        }

        String utilizador = request.getRemoteUser();
        if (utilizador == null) utilizador = "anonimo";

        String query = request.getQueryString();
        String caminho = query == null ? uri : uri + "?" + query;
        String resultado = ex != null ? "ERRO(" + ex.getClass().getSimpleName() + ")"
                                       : "estado=" + response.getStatus();

        LOG.info("utilizador={} acao={} {} {} ip={}",
                utilizador, request.getMethod(), caminho, resultado, request.getRemoteAddr());
    }
}
