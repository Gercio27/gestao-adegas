package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;

/**
 * Ficha dos contentores / paletes de bag-in-box. Espelha a ficha dos
 * contentores de garrafas, mas conta unidades de bag-in-box em vez de garrafas.
 */
@Controller
@RequestMapping("/fichas/bag-in-box")
public class ContentorBagInBoxController {

    private final ContentorBagInBoxRepository repo;
    private final ArmazemRepository armazemRepo;
    private final AdegaRepository adegaRepo;
    private final CodigoService codigoService;

    public ContentorBagInBoxController(ContentorBagInBoxRepository repo, ArmazemRepository armazemRepo,
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
        ContentorBagInBox c = repo.findById(id).orElse(null);
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
        return "fichas/baginbox/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("contentor", new ContentorBagInBox());
        preencherOpcoes(model);
        return "fichas/baginbox/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        ContentorBagInBox c = repo.findById(id).orElse(null);
        if (c == null) { ra.addFlashAttribute("erro", "Contentor bag-in-box nao encontrado."); return "redirect:/fichas/bag-in-box"; }
        model.addAttribute("contentor", c);
        preencherOpcoes(model);
        return "fichas/baginbox/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("contentor") ContentorBagInBox c, BindingResult result,
                          @RequestParam(value = "certificadoFicheiro", required = false) org.springframework.web.multipart.MultipartFile certificadoFicheiro,
                          @RequestParam(value = "localTipo", required = false) String localTipo,
                          Model model, RedirectAttributes ra) {
        // Fica num armazém OU numa adega — limpa o lado não escolhido.
        if ("ADEGA".equals(localTipo)) c.setArmazem(null); else c.setAdega(null);
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "fichas/baginbox/form";
        }
        if (c.getId() == null) {
            c.setCodigo(codigoService.proximoCodigo(ContentorBagInBox.PREFIXO));
        }
        if (c.getUnidadesAtuais() < 0) c.setUnidadesAtuais(0);
        if (c.getCapacidadeUnidades() < 0) c.setCapacidadeUnidades(0);
        // Contentor vazio não fica com vinho pendurado.
        if (c.getUnidadesAtuais() == 0) { c.setVinhoNome(null); c.setVinhoEmbaladoId(null); c.setRotulado(false); }
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
                return "redirect:/fichas/bag-in-box";
            }
        }
        // Sem certificacao, nao fica validade nem PDF pendurados.
        if (!c.isCertificado()) {
            c.setValidadeCertificacao(null);
            c.setCertificacaoCodigo(null);
        }
        repo.save(c);
        ra.addFlashAttribute("sucesso", "Contentor bag-in-box guardado: " + c.getCodigo());
        return "redirect:/fichas/bag-in-box";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        ContentorBagInBox c = repo.findById(id).orElse(null);
        if (c == null) { ra.addFlashAttribute("erro", "Contentor bag-in-box nao encontrado."); return "redirect:/fichas/bag-in-box"; }
        if (c.getUnidadesAtuais() > 0) {
            ra.addFlashAttribute("erro", "Não pode eliminar: ainda tem " + c.getUnidadesAtuais() + " unidade(s) lá dentro.");
            return "redirect:/fichas/bag-in-box";
        }
        repo.delete(c);
        ra.addFlashAttribute("sucesso", "Contentor bag-in-box eliminado.");
        return "redirect:/fichas/bag-in-box";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("tiposBag", TipoBagInBox.values());
        model.addAttribute("armazens", armazemRepo.findAllByOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
    }
}
