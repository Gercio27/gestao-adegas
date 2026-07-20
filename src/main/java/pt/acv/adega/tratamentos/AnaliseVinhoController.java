package pt.acv.adega.tratamentos;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Análises aos vinhos — funciona como os tratamentos enológicos (adega, vinho,
 * onde está), mas em vez da data do tratamento guarda o PDF da análise na base
 * de dados. Repetível, para o histórico de análises do vinho.
 */
@Controller
@RequestMapping("/analises")
public class AnaliseVinhoController {

    private final AnaliseVinhoRepository repo;
    private final LocalizacaoVinhoService localizacaoService;
    private final CodigoService codigoService;

    public AnaliseVinhoController(AnaliseVinhoRepository repo,
                                  LocalizacaoVinhoService localizacaoService, CodigoService codigoService) {
        this.repo = repo;
        this.localizacaoService = localizacaoService;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("analises", repo.findAllByOrderByVinhoNomeAscDataAnaliseAsc());
        return "analises/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        AnaliseVinho a = new AnaliseVinho();
        a.setDataAnalise(LocalDate.now());
        model.addAttribute("analise", a);
        preencherOpcoes(model);
        return "analises/form";
    }

    @PostMapping
    public String guardar(@ModelAttribute("analise") AnaliseVinho analise,
                          @RequestParam(value = "analiseFicheiro", required = false) MultipartFile analiseFicheiro,
                          Authentication auth, RedirectAttributes ra) {
        if (analise.getId() == null) {
            analise.setCodigo(codigoService.proximoCodigo(AnaliseVinho.PREFIXO));
            analise.setCriadoPor(auth.getName());
        } else {
            AnaliseVinho existente = repo.findById(analise.getId()).orElse(null);
            if (existente != null) {
                analise.setCriadoPor(existente.getCriadoPor());
                // Mantém o PDF já guardado se não vier ficheiro novo.
                analise.setAnalisePdf(existente.getAnalisePdf());
                analise.setAnalisePdfNome(existente.getAnalisePdfNome());
                analise.setAnalisePdfTipo(existente.getAnalisePdfTipo());
            }
        }
        if (analiseFicheiro != null && !analiseFicheiro.isEmpty()) {
            try {
                analise.setAnalisePdf(analiseFicheiro.getBytes());
                analise.setAnalisePdfNome(analiseFicheiro.getOriginalFilename());
                analise.setAnalisePdfTipo(analiseFicheiro.getContentType());
            } catch (IOException e) {
                ra.addFlashAttribute("erro", "Não foi possível ler o ficheiro da análise.");
                return "redirect:/analises/nova";
            }
        }
        repo.save(analise);
        ra.addFlashAttribute("sucesso", "Análise registada: " + analise.getCodigo());
        return "redirect:/analises";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<ByteArrayResource> descarregar(@PathVariable Long id) {
        AnaliseVinho a = repo.findById(id).orElse(null);
        if (a == null || !a.isTemPdf()) return ResponseEntity.notFound().build();
        String nome = a.getAnalisePdfNome() != null ? a.getAnalisePdfNome() : ("analise-" + a.getCodigo() + ".pdf");
        MediaType tipo = a.getAnalisePdfTipo() != null
                ? MediaType.parseMediaType(a.getAnalisePdfTipo()) : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(nome).build().toString())
                .body(new ByteArrayResource(a.getAnalisePdf()));
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Análise eliminada.");
        return "redirect:/analises";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("categorias", CategoriaVinho.values());
        model.addAttribute("dadosPorCategoria", localizacaoService.dadosPorCategoria());
    }
}
