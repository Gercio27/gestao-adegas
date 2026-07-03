package pt.acv.adega.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("p", dashboardService.carregar());
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
