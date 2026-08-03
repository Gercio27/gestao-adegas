package pt.acv.adega.produtos;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.CastaRepository;
import pt.acv.adega.fichas.ContentorGarrafas;
import pt.acv.adega.fichas.ContentorGarrafasRepository;
import pt.acv.adega.fichas.ContentorBagInBox;
import pt.acv.adega.fichas.ContentorBagInBoxRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Mapa de vinhos engarrafados (produtos acabados). No fluxo normal sao gerados
 * pelo Engarrafamento. Para uma adega "a meio", o administrador pode registar o
 * saldo inicial de vinho ja engarrafado e coloca-lo num contentor.
 */
@Controller
@RequestMapping("/produtos/engarrafados")
public class VinhoEngarrafadoController {

    private final VinhoEngarrafadoRepository repo;
    private final CastaRepository castaRepo;
    private final ContentorGarrafasRepository contentorRepo;
    private final ContentorBagInBoxRepository bibRepo;
    private final StockRotuladoRepository stockRotuladoRepo;
    private final CodigoService codigoService;

    public VinhoEngarrafadoController(VinhoEngarrafadoRepository repo, CastaRepository castaRepo,
                                      ContentorGarrafasRepository contentorRepo,
                                      ContentorBagInBoxRepository bibRepo,
                                      StockRotuladoRepository stockRotuladoRepo,
                                      CodigoService codigoService) {
        this.repo = repo;
        this.castaRepo = castaRepo;
        this.contentorRepo = contentorRepo;
        this.bibRepo = bibRepo;
        this.stockRotuladoRepo = stockRotuladoRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Integer ano,
                         @RequestParam(required = false) String local,
                         @RequestParam(required = false) String vinho,
                         Model model) {
        List<VinhoEngarrafado> todos = repo.findAllByOrderByDataProducaoDesc();
        // Onde estao as garrafas de cada vinho: sai dos contentores que o guardam.
        Map<Long, String> locaisPorVinho = locaisPorVinho();

        List<VinhoEngarrafado> lista = new ArrayList<>();
        for (VinhoEngarrafado v : todos) {
            Integer anoV = v.getDataProducao() != null ? v.getDataProducao().getYear() : null;
            if (ano != null && !ano.equals(anoV)) continue;
            if (vinho != null && !vinho.isBlank() && !vinho.equals(v.getNome())) continue;
            if (local != null && !local.isBlank()) {
                String onde = locaisPorVinho.getOrDefault(v.getId(), "");
                if (!onde.contains(local)) continue;
            }
            lista.add(v);
        }

        int totalGarrafas = lista.stream().mapToInt(VinhoEngarrafado::getNumeroGarrafas).sum();
        model.addAttribute("engarrafados", lista);
        model.addAttribute("totalGarrafas", totalGarrafas);
        model.addAttribute("locaisPorVinho", locaisPorVinho);

        // Opções dos seletores: sempre a partir da lista completa.
        TreeSet<Integer> anos = new TreeSet<>(Comparator.reverseOrder());
        TreeSet<String> vinhos = new TreeSet<>();
        for (VinhoEngarrafado v : todos) {
            if (v.getDataProducao() != null) anos.add(v.getDataProducao().getYear());
            if (v.getNome() != null && !v.getNome().isBlank()) vinhos.add(v.getNome());
        }
        model.addAttribute("anos", anos);
        model.addAttribute("nomesVinhos", vinhos);
        model.addAttribute("locais", new TreeSet<>(locaisConhecidos()));
        model.addAttribute("fAno", ano);
        model.addAttribute("fLocal", local);
        model.addAttribute("fVinho", vinho);
        model.addAttribute("totalGeral", todos.size());
        model.addAttribute("totalFiltrado", lista.size());
        return "produtos/engarrafados/lista";
    }

    /** Adegas/armazéns onde está cada vinho engarrafado, por id do vinho. */
    private Map<Long, String> locaisPorVinho() {
        Map<Long, Set<String>> acumulado = new LinkedHashMap<>();
        for (ContentorGarrafas c : contentorRepo.findAll()) {
            if (c.getVinhoEngarrafadoId() == null || c.getGarrafasAtuais() <= 0) continue;
            acumulado.computeIfAbsent(c.getVinhoEngarrafadoId(), k -> new TreeSet<>()).add(c.getLocalizacao());
        }
        for (ContentorBagInBox c : bibRepo.findAll()) {
            if (c.getVinhoEmbaladoId() == null || c.getUnidadesAtuais() <= 0) continue;
            acumulado.computeIfAbsent(c.getVinhoEmbaladoId(), k -> new TreeSet<>()).add(c.getLocalizacao());
        }
        // As garrafas já rotuladas saíram do contentor e estão em caixas.
        for (StockRotulado s : stockRotuladoRepo.findAll()) {
            if (s.getVinhoEngarrafadoId() == null || s.getGarrafas() <= 0) continue;
            acumulado.computeIfAbsent(s.getVinhoEngarrafadoId(), k -> new TreeSet<>()).add(s.getLocalNome());
        }
        Map<Long, String> out = new LinkedHashMap<>();
        acumulado.forEach((id, locais) -> out.put(id, String.join(", ", locais)));
        return out;
    }

    private Set<String> locaisConhecidos() {
        Set<String> out = new TreeSet<>();
        for (String junto : locaisPorVinho().values()) {
            for (String parte : junto.split(", ")) if (!parte.isBlank()) out.add(parte);
        }
        return out;
    }

    /** Etiqueta imprimivel para o contentor do vinho engarrafado. */
    @GetMapping("/{id}/etiqueta")
    public String etiqueta(@PathVariable Long id, Model model, RedirectAttributes ra) {
        VinhoEngarrafado v = repo.findById(id).orElse(null);
        if (v == null) { ra.addFlashAttribute("erro", "Vinho engarrafado nao encontrado."); return "redirect:/produtos/engarrafados"; }
        model.addAttribute("v", v);
        return "produtos/engarrafados/etiqueta";
    }

    // ----- Saldo inicial (adega a meio) — só administrador (protegido no SecurityConfig) -----

    @GetMapping("/saldo-inicial")
    public String novoSaldo(Model model) {
        VinhoEngarrafado v = new VinhoEngarrafado();
        v.setDataProducao(LocalDateTime.now());
        model.addAttribute("engarrafado", v);
        preencherOpcoes(model);
        return "produtos/engarrafados/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        VinhoEngarrafado v = repo.findById(id).orElse(null);
        if (v == null) { ra.addFlashAttribute("erro", "Vinho engarrafado nao encontrado."); return "redirect:/produtos/engarrafados"; }
        model.addAttribute("engarrafado", v);
        preencherOpcoes(model);
        return "produtos/engarrafados/form";
    }

    @PostMapping("/saldo-inicial")
    public String guardarSaldo(@Valid @ModelAttribute("engarrafado") VinhoEngarrafado v, BindingResult result,
                               @RequestParam(value = "contentorId", required = false) Long contentorId,
                               Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "produtos/engarrafados/form";
        }
        boolean novo = v.getId() == null;
        if (novo) {
            v.setCodigo(codigoService.proximoCodigo(VinhoEngarrafado.PREFIXO));
            if (v.getOrigemDescricao() == null || v.getOrigemDescricao().isBlank()) {
                v.setOrigemDescricao("Saldo inicial (adega a meio)");
            }
        }
        repo.save(v);

        // Coloca as garrafas no contentor escolhido (opcional).
        if (contentorId != null) {
            ContentorGarrafas c = contentorRepo.findById(contentorId).orElse(null);
            if (c != null) {
                c.setVinhoEngarrafadoId(v.getId());
                c.setVinhoNome(v.getNome());
                c.setGarrafasAtuais(v.getNumeroGarrafas());
                c.setRotulado(v.isRotulado());
                contentorRepo.save(c);
            }
        }
        ra.addFlashAttribute("sucesso", "Saldo de vinho engarrafado guardado: " + v.getCodigo());
        return "redirect:/produtos/engarrafados";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Vinho engarrafado eliminado.");
        return "redirect:/produtos/engarrafados";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("contentores", contentorRepo.findAllByOrderByNomeAsc());
    }
}
