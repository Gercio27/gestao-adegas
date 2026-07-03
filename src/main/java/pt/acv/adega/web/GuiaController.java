package pt.acv.adega.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Guia de utilizador (ajuda) acessivel a partir do menu. */
@Controller
public class GuiaController {

    @GetMapping("/guia")
    public String guia() {
        return "guia";
    }
}
