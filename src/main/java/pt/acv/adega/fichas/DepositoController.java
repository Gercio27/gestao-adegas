package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/fichas/depositos")
public class DepositoController {

    private final DepositoRepository repo;
    private final AdegaRepository adegaRepo;
    private final ArmazemRepository armazemRepo;
    private final CodigoService codigoService;
    private final pt.acv.adega.produtos.MostoRepository mostoRepo;

    public DepositoController(DepositoRepository repo, AdegaRepository adegaRepo, ArmazemRepository armazemRepo,
                              CodigoService codigoService, pt.acv.adega.produtos.MostoRepository mostoRepo) {
        this.repo = repo;
        this.adegaRepo = adegaRepo;
        this.armazemRepo = armazemRepo;
        this.codigoService = codigoService;
        this.mostoRepo = mostoRepo;
    }

    @GetMapping("/{id}/etiqueta")
    public String etiqueta(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Deposito d = repo.findById(id).orElse(null);
        if (d == null) { ra.addFlashAttribute("erro", "Deposito nao encontrado."); return "redirect:/fichas/depositos"; }
        model.addAttribute("tipo", "Depósito");
        model.addAttribute("codigo", d.getCodigo());
        model.addAttribute("identificacao", d.getIdentificacao());
        model.addAttribute("adega", d.getAdega() != null ? d.getAdega().getNome()
                : (d.getArmazem() != null ? "Armazém " + d.getArmazem().getNome() : null));
        model.addAttribute("capacidade", d.getCapacidadeLitros());
        model.addAttribute("volume", d.getVolumeAtualLitros());
        model.addAttribute("propriedade", d.getPropriedade().getDescricao());
        model.addAttribute("conteudos", mostoRepo.findByDepositoId(id));
        return "fichas/etiqueta";
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("depositos", repo.findAllByOrderByIdentificacaoAsc());
        return "fichas/depositos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("deposito", new Deposito());
        preencherOpcoes(model);
        return "fichas/depositos/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Deposito d = repo.findById(id).orElse(null);
        if (d == null) {
            ra.addFlashAttribute("erro", "Deposito nao encontrado.");
            return "redirect:/fichas/depositos";
        }
        model.addAttribute("deposito", d);
        preencherOpcoes(model);
        // O que já lá está — para o formulário não pedir o vinho outra vez.
        model.addAttribute("conteudo", mostoRepo.findByDepositoId(id));
        return "fichas/depositos/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("deposito") Deposito d, BindingResult result,
                          @RequestParam(value = "localTipo", required = false) String localTipo,
                          @RequestParam(value = "vinhoNome", required = false) String vinhoNome,
                          @RequestParam(value = "estadoConteudo", required = false) EstadoMosto estadoConteudo,
                          Model model, RedirectAttributes ra) {
        // O depósito fica numa adega OU num armazém — limpa o lado não escolhido.
        if ("ARMAZEM".equals(localTipo)) d.setAdega(null); else d.setArmazem(null);
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "fichas/depositos/form";
        }
        if (d.getId() == null) {
            d.setCodigo(codigoService.proximoCodigo(Deposito.PREFIXO));
        }
        repo.save(d);
        String extra = registarConteudoInicial(d, vinhoNome, estadoConteudo);
        ra.addFlashAttribute("sucesso", "Deposito guardado: " + d.getCodigo() + extra);
        return "redirect:/fichas/depositos";
    }

    /**
     * Stock inicial: se o deposito ja vem com litros e ainda nao tem nenhum
     * mosto/vinho registado, cria a ficha do produto que la esta. E o mesmo
     * que a talha faz ao "inserir conteudo" - sem isto os litros existiam mas
     * o vinho nao aparecia em lado nenhum.
     */
    private String registarConteudoInicial(Deposito d, String vinhoNome, EstadoMosto estado) {
        BigDecimal litros = d.getVolumeAtualLitros();
        if (litros == null || litros.signum() <= 0) return "";
        if (!mostoRepo.findByDepositoId(d.getId()).isEmpty()) return "";
        if (vinhoNome == null || vinhoNome.isBlank()) return "";

        Mosto m = new Mosto();
        m.setCodigo(codigoService.proximoCodigo(Mosto.PREFIXO));
        m.setLitros(litros);
        m.setEstado(estado != null ? estado : EstadoMosto.VINHO_GRANEL);
        m.setVinhoNome(vinhoNome.trim());
        m.setDeposito(d);
        m.setDataProducao(LocalDateTime.now());
        m.setOrigemDescricao("Stock inicial do depósito " + d.getIdentificacao());
        mostoRepo.save(m);
        return " · conteúdo registado: " + m.getCodigo() + " (" + vinhoNome.trim() + ", " + litros.toPlainString() + " L)";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Deposito eliminado.");
        return "redirect:/fichas/depositos";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("armazens", armazemRepo.findAllByOrderByNomeAsc());
        model.addAttribute("propriedades", Propriedade.values());
        model.addAttribute("estados", EstadoMosto.values());
    }
}
