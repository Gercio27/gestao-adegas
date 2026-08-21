package pt.acv.adega.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pagina de administracao para limpar dados de trabalho. O utilizador escolhe o
 * que quer apagar. So' admin (ver SecurityConfig).
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
        model.addAttribute("grupos", limpezaService.grupos());
        model.addAttribute("contagens", limpezaService.contagensPorGrupo());
        model.addAttribute("totalProcessos", limpezaService.contarProcessos());
        model.addAttribute("confirmacao", CONFIRMACAO);
        return "admin/limpar-processos";
    }

    @PostMapping
    public String limpar(@RequestParam(required = false) List<String> grupos,
                         @RequestParam(required = false) String confirmar,
                         RedirectAttributes ra) {
        if (!CONFIRMACAO.equals(confirmar)) {
            ra.addFlashAttribute("erro", "Para limpar, escreva " + CONFIRMACAO + " no campo de confirmação.");
            return "redirect:/admin/limpar-processos";
        }
        // So' passam ids que existem — o que vem do formulario nunca entra no SQL.
        Set<String> escolhidos = new LinkedHashSet<>();
        if (grupos != null) {
            for (String id : grupos) {
                if (limpezaService.grupo(id) != null) escolhidos.add(id);
            }
        }
        if (escolhidos.isEmpty()) {
            ra.addFlashAttribute("erro", "Escolha pelo menos um bloco de dados para apagar.");
            return "redirect:/admin/limpar-processos";
        }

        // Apagar o que esta a montante e deixar o que veio dele deixaria registos
        // orfaos (moagens sem vindima, mostos sem moagem). Recusa-se, em vez de
        // apagar por conta propria mais do que o utilizador mandou.
        List<GrupoLimpeza> falta = limpezaService.emFalta(escolhidos);
        if (!falta.isEmpty()) {
            ra.addFlashAttribute("erro", "Não é possível apagar só isso: "
                    + falta.stream().map(GrupoLimpeza::nome).collect(Collectors.joining(", "))
                    + " — o que veio a seguir nasceu dos dados que quer apagar e ficaria sem origem."
                    + " Marque também esses blocos, ou deixe os anteriores de fora.");
            return "redirect:/admin/limpar-processos";
        }

        long apagados = limpezaService.limpar(escolhidos);
        String nomes = escolhidos.stream()
                .map(id -> limpezaService.grupo(id).nome())
                .collect(Collectors.joining(", "));
        ra.addFlashAttribute("sucesso", apagados + " registos apagados (" + nomes + "). As fichas ficaram todas.");
        return "redirect:/admin/limpar-processos";
    }
}
