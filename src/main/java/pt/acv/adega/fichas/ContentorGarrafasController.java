package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

@Controller
@RequestMapping("/fichas/contentores")
public class ContentorGarrafasController {

    private final ContentorGarrafasRepository repo;
    private final ArmazemRepository armazemRepo;
    private final AdegaRepository adegaRepo;
    private final CodigoService codigoService;

    public ContentorGarrafasController(ContentorGarrafasRepository repo, ArmazemRepository armazemRepo,
                                       AdegaRepository adegaRepo, CodigoService codigoService) {
        this.repo = repo;
        this.armazemRepo = armazemRepo;
        this.adegaRepo = adegaRepo;
        this.codigoService = codigoService;
    }


    /** Descarrega o PDF do certificado guardado nesta ficha. */
    @GetMapping("/{id}/certificado")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.ByteArrayResource> certificado(
            @PathVariable Long id) {
        ContentorGarrafas c = repo.findById(id).orElse(null);
        if (c == null || !c.isTemCertificadoPdf()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        String nome = c.getCertificadoPdfNome() != null ? c.getCertificadoPdfNome()
                : ("certificado-" + c.getCodigo() + ".pdf");
        org.springframework.http.MediaType tipo = c.getCertificadoPdfTipo() != null
                ? org.springframework.http.MediaType.parseMediaType(c.getCertificadoPdfTipo())
                : org.springframework.http.MediaType.APPLICATION_PDF;
        return org.springframework.http.ResponseEntity.ok().contentType(tipo)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.inline().filename(nome).build().toString())
                .body(new org.springframework.core.io.ByteArrayResource(c.getCertificadoPdf()));
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("contentores", repo.findAllByOrderByNomeAsc());
        return "fichas/contentores/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("contentor", new ContentorGarrafas());
        preencherOpcoes(model);
        return "fichas/contentores/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        ContentorGarrafas c = repo.findById(id).orElse(null);
        if (c == null) { ra.addFlashAttribute("erro", "Contentor nao encontrado."); return "redirect:/fichas/contentores"; }
        model.addAttribute("contentor", c);
        preencherOpcoes(model);
        return "fichas/contentores/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("contentor") ContentorGarrafas c, BindingResult result,
                          @RequestParam(value = "certificadoFicheiro", required = false) org.springframework.web.multipart.MultipartFile certificadoFicheiro,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "fichas/contentores/form";
        }
        if (c.getId() == null) {
            c.setCodigo(codigoService.proximoCodigo(ContentorGarrafas.PREFIXO));
        }
        // Sem capacidade indicada, assume o máximo fixo do formato.
        if (c.getCapacidadeGarrafas() <= 0 && c.getTipoGarrafa() != null) {
            c.setCapacidadeGarrafas(c.getTipoGarrafa().getMaximoGarrafas());
        }
        // Mantem o PDF ja guardado se nao vier ficheiro novo.
        if (c.getId() != null) {
            repo.findById(c.getId()).ifPresent(antigo -> {
                if (c.getCertificadoPdf() == null) {
                    c.setCertificadoPdf(antigo.getCertificadoPdf());
                    c.setCertificadoPdfNome(antigo.getCertificadoPdfNome());
                    c.setCertificadoPdfTipo(antigo.getCertificadoPdfTipo());
                }
                if (c.getCertificacaoCodigo() == null) c.setCertificacaoCodigo(antigo.getCertificacaoCodigo());
            });
        }
        if (certificadoFicheiro != null && !certificadoFicheiro.isEmpty()) {
            try {
                c.setCertificadoPdf(certificadoFicheiro.getBytes());
                c.setCertificadoPdfNome(certificadoFicheiro.getOriginalFilename());
                c.setCertificadoPdfTipo(certificadoFicheiro.getContentType());
            } catch (java.io.IOException e) {
                ra.addFlashAttribute("erro", "Nao foi possivel ler o PDF do certificado.");
                return "redirect:/fichas/contentores";
            }
        }
        // Sem certificacao, nao fica validade nem PDF pendurados.
        if (!c.isCertificado()) {
            c.setValidadeCertificacao(null);
            c.setCertificacaoCodigo(null);
        }
        repo.save(c);
        ra.addFlashAttribute("sucesso", "Contentor guardado: " + c.getCodigo());
        return "redirect:/fichas/contentores";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Contentor eliminado.");
        return "redirect:/fichas/contentores";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("tiposGarrafa", TipoGarrafa.values());
        model.addAttribute("armazens", armazemRepo.findAllByOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
    }
}
