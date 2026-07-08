package pt.acv.adega.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pt.acv.adega.auditoria.AuditoriaService;

import java.util.Map;
import java.util.Set;

/**
 * Regista, para efeitos de auditoria, todas as alteracoes feitas na aplicacao
 * (pedidos POST/PUT/DELETE/PATCH), identificando o utilizador autenticado, a
 * accao e o resultado. Grava na base de dados (via AuditoriaService, consultavel
 * em /auditoria) e tambem no logger "AUDITORIA" (ficheiro logs/auditoria.log).
 */
@Component
public class AuditoriaInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger("AUDITORIA");

    /** Metodos HTTP que alteram dados e por isso interessa auditar. */
    private static final Set<String> METODOS_ALTERACAO = Set.of("POST", "PUT", "DELETE", "PATCH");

    /** Nomes legiveis das areas da aplicacao, a partir de um segmento do caminho. */
    private static final Map<String, String> AREAS = Map.ofEntries(
            Map.entry("maturacao", "Análise à maturação"),
            Map.entry("planeamento", "Planeamento dos vinhos"),
            Map.entry("vindima", "Vindima"),
            Map.entry("moagem", "Moagem"),
            Map.entry("remontagem", "Remontagem"),
            Map.entry("atesto", "Atesto"),
            Map.entry("movimento-mosto", "Movimento de mosto"),
            Map.entry("passagem-vinho", "Passagem a vinho"),
            Map.entry("certificacao", "Certificação"),
            Map.entry("engarrafamento", "Engarrafamento"),
            Map.entry("rotulagem", "Rotulagem"),
            Map.entry("comercial", "Comercial / Nota de entrega"),
            Map.entry("castas", "Castas"),
            Map.entry("vinhas", "Vinhas"),
            Map.entry("trabalhadores", "Trabalhadores"),
            Map.entry("adegas", "Adegas"),
            Map.entry("talhas", "Talhas"),
            Map.entry("depositos", "Depósitos / Cubas"),
            Map.entry("consumiveis", "Consumíveis"),
            Map.entry("mostos", "Mostos"),
            Map.entry("engarrafados", "Vinhos engarrafados"),
            Map.entry("lotes", "Lotes"),
            Map.entry("utilizadores", "Utilizadores")
    );

    private final AuditoriaService auditoriaService;

    public AuditoriaInterceptor(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

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

        String metodo = request.getMethod();
        String query = request.getQueryString();
        String caminho = query == null ? uri : uri + "?" + query;
        String descricao = descrever(metodo, uri);
        int estado = response.getStatus();
        String ip = request.getRemoteAddr();

        // Persistir na BD nunca deve quebrar o pedido; regista o erro no ficheiro.
        try {
            auditoriaService.registar(utilizador, metodo, caminho, descricao, estado, ip);
        } catch (Exception e) {
            LOG.warn("Falha ao gravar registo de auditoria: {}", e.getMessage());
        }

        LOG.info("utilizador={} acao=\"{}\" {} {} estado={} ip={}",
                utilizador, descricao, metodo, caminho, estado, ip);
    }

    /** Constroi uma descricao legivel a partir do metodo e do caminho. */
    private String descrever(String metodo, String uri) {
        String[] seg = uri.split("/");
        String ultimo = seg.length > 0 ? seg[seg.length - 1] : "";

        String acao;
        switch (ultimo) {
            case "fechar":   acao = "Fechou"; break;
            case "reabrir":  acao = "Reabriu"; break;
            case "eliminar": acao = "Eliminou"; break;
            default:         acao = "DELETE".equals(metodo) ? "Eliminou" : "Guardou";
        }
        return acao + " · " + area(seg);
    }

    /** Area legivel + id do registo (se existir) a partir dos segmentos do caminho. */
    private String area(String[] seg) {
        String label = null;
        String id = null;
        for (String s : seg) {
            if (s.isEmpty()) continue;
            if (label == null && AREAS.containsKey(s)) label = AREAS.get(s);
            if (s.matches("\\d+")) id = s;
        }
        if (label == null) label = "(outro)";
        return id != null ? label + " (#" + id + ")" : label;
    }
}
