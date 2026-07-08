package pt.acv.adega.auditoria;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pt.acv.adega.security.UtilizadorRepository;

/**
 * Consulta do registo de auditoria. Restrito a ADMIN (ver SecurityConfig:
 * /auditoria/**). Mostra quem alterou o que, com nome, data e hora.
 */
@Controller
@RequestMapping("/auditoria")
public class AuditoriaController {

    private final RegistoAuditoriaRepository repo;
    private final UtilizadorRepository utilizadorRepo;

    public AuditoriaController(RegistoAuditoriaRepository repo, UtilizadorRepository utilizadorRepo) {
        this.repo = repo;
        this.utilizadorRepo = utilizadorRepo;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String utilizador, Model model) {
        boolean filtrar = utilizador != null && !utilizador.isBlank();
        model.addAttribute("registos", filtrar
                ? repo.findTop500ByUsernameOrderByDataHoraDesc(utilizador)
                : repo.findTop500ByOrderByDataHoraDesc());
        model.addAttribute("utilizadores", utilizadorRepo.findAll());
        model.addAttribute("filtroUtilizador", filtrar ? utilizador : "");
        return "auditoria/lista";
    }
}
