package pt.acv.adega.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pagina de administracao para limpar os processos e voltar a testar a
 * aplicacao do zero, sem perder as fichas. So' admin (ver SecurityConfig).
 */
@Controller
@RequestMapping("/admin/limpar-processos")
public class LimpezaController {

    /** Palavra que o utilizador tem de escrever para o botao funcionar. */
    private static final String CONFIRMACAO = "LIMPAR";

    private final LimpezaService limpezaService;

    public LimpezaController(LimpezaService limpezaService) {
        this.limpezaService = limpezaService;
    }

    @GetMapping
    public String pagina(Model model) {
        model.addAttribute("totalProcessos", limpezaService.contarProcessos());
        model.addAttribute("contagens", contagens());
        model.addAttribute("confirmacao", CONFIRMACAO);
        return "admin/limpar-processos";
    }

    @PostMapping
    public String limpar(@RequestParam(required = false) String confirmar, RedirectAttributes ra) {
        if (!CONFIRMACAO.equals(confirmar)) {
            ra.addFlashAttribute("erro", "Para limpar, escreva " + CONFIRMACAO + " no campo de confirmação.");
            return "redirect:/admin/limpar-processos";
        }
        long apagados = limpezaService.limparProcessos();
        ra.addFlashAttribute("sucesso", "Processos limpos: " + apagados
                + " registos apagados. As fichas ficaram todas; talhas, depósitos e contentores estão vazios.");
        return "redirect:/admin/limpar-processos";
    }

    /** O que existe hoje, para o utilizador ver antes de decidir. */
    private Map<String, Long> contagens() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("Planeamentos de vinho", limpezaService.contar("planeamento_vinho"));
        m.put("Vindimas", limpezaService.contar("processo_vindima"));
        m.put("Moagens", limpezaService.contar("processo_moagem"));
        m.put("Remontagens", limpezaService.contar("processo_remontagem"));
        m.put("Atestos", limpezaService.contar("processo_atesto"));
        m.put("Movimentos de mosto", limpezaService.contar("processo_movimento_mosto"));
        m.put("Passagens a limpo", limpezaService.contar("processo_passagem_vinho"));
        m.put("Movimentos de vinho a granel", limpezaService.contar("processo_movimento_vinho"));
        m.put("Certificações", limpezaService.contar("processo_certificacao"));
        m.put("Loteamentos", limpezaService.contar("loteamento"));
        m.put("Engarrafamentos", limpezaService.contar("processo_engarrafamento"));
        m.put("Rotulagens", limpezaService.contar("processo_rotulagem"));
        m.put("Entregas ao comercial", limpezaService.contar("processo_comercial"));
        m.put("Saídas de contentor", limpezaService.contar("saida_contentor"));
        m.put("Análises e tratamentos", limpezaService.contar("analise_vinho")
                + limpezaService.contar("tratamento_enologico")
                + limpezaService.contar("processo_analise_maturacao"));
        m.put("Mostos / vinhos a granel", limpezaService.contar("mosto"));
        m.put("Vinhos engarrafados", limpezaService.contar("vinho_engarrafado"));
        return m;
    }
}
