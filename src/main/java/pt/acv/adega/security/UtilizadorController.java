package pt.acv.adega.security;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestao de contas de acesso. Restrito a ADMIN (ver SecurityConfig: /utilizadores/**).
 */
@Controller
@RequestMapping("/utilizadores")
public class UtilizadorController {

    private final UtilizadorRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UtilizadorController(UtilizadorRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("utilizadores", repo.findAll());
        return "utilizadores/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new UtilizadorForm());
        model.addAttribute("perfis", Perfil.values());
        model.addAttribute("novo", true);
        return "utilizadores/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Utilizador u = repo.findById(id).orElse(null);
        if (u == null) { ra.addFlashAttribute("erro", "Utilizador não encontrado."); return "redirect:/utilizadores"; }
        model.addAttribute("form", UtilizadorForm.de(u));
        model.addAttribute("perfis", Perfil.values());
        model.addAttribute("novo", false);
        return "utilizadores/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("form") UtilizadorForm form, BindingResult result,
                          Model model, RedirectAttributes ra) {
        boolean novo = form.getId() == null;
        model.addAttribute("novo", novo);
        model.addAttribute("perfis", Perfil.values());

        if (novo && (form.getPassword() == null || form.getPassword().isBlank())) {
            result.rejectValue("password", "obrigatoria", "Defina uma palavra-passe.");
        }
        // Unicidade do username
        repo.findByUsername(form.getUsername()).ifPresent(existente -> {
            if (!existente.getId().equals(form.getId())) {
                result.rejectValue("username", "duplicado", "Já existe um utilizador com este nome.");
            }
        });
        if (result.hasErrors()) {
            return "utilizadores/form";
        }

        Utilizador u = novo ? new Utilizador() : repo.findById(form.getId()).orElseThrow();
        if (novo) {
            u.setUsername(form.getUsername());
        }
        u.setNome(form.getNome());
        u.setPerfil(form.getPerfil());
        u.setAtivo(form.isAtivo());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        repo.save(u);
        ra.addFlashAttribute("sucesso", "Utilizador guardado: " + u.getUsername());
        return "redirect:/utilizadores";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Utilizador u = repo.findById(id).orElse(null);
        if (u == null) { ra.addFlashAttribute("erro", "Utilizador não encontrado."); return "redirect:/utilizadores"; }
        if (u.getUsername().equals(auth.getName())) {
            ra.addFlashAttribute("erro", "Não pode eliminar a sua própria conta.");
            return "redirect:/utilizadores";
        }
        if (u.getPerfil() == Perfil.ADMIN && repo.countByPerfilAndAtivoTrue(Perfil.ADMIN) <= 1) {
            ra.addFlashAttribute("erro", "Tem de existir pelo menos um administrador ativo.");
            return "redirect:/utilizadores";
        }
        repo.delete(u);
        ra.addFlashAttribute("sucesso", "Utilizador eliminado.");
        return "redirect:/utilizadores";
    }
}
