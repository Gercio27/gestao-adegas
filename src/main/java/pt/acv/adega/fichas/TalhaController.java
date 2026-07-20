package pt.acv.adega.fichas;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/fichas/talhas")
public class TalhaController {

    private final TalhaRepository repo;
    private final AdegaRepository adegaRepo;
    private final CastaRepository castaRepo;
    private final CodigoService codigoService;
    private final MostoRepository mostoRepo;

    public TalhaController(TalhaRepository repo, AdegaRepository adegaRepo, CastaRepository castaRepo,
                           CodigoService codigoService, MostoRepository mostoRepo) {
        this.repo = repo;
        this.adegaRepo = adegaRepo;
        this.castaRepo = castaRepo;
        this.codigoService = codigoService;
        this.mostoRepo = mostoRepo;
    }

    @GetMapping("/{id}/etiqueta")
    public String etiqueta(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Talha t = repo.findById(id).orElse(null);
        if (t == null) { ra.addFlashAttribute("erro", "Talha nao encontrada."); return "redirect:/fichas/talhas"; }
        model.addAttribute("tipo", "Talha");
        model.addAttribute("codigo", t.getCodigo());
        model.addAttribute("identificacao", t.getIdentificacao());
        model.addAttribute("adega", t.getAdega() != null ? t.getAdega().getNome() : null);
        model.addAttribute("capacidade", t.getCapacidadeLitros());
        model.addAttribute("volume", t.getVolumeAtualLitros());
        model.addAttribute("propriedade", t.getPropriedade().getDescricao());
        model.addAttribute("conteudos", mostoRepo.findByTalhaId(id));
        return "fichas/etiqueta";
    }

    @GetMapping
    public String listar(Model model) {
        List<Talha> talhas = repo.findAllByOrderByIdentificacaoAsc();
        // Resumo do conteudo (o que esta dentro de cada talha) para a listagem.
        java.util.Map<Long, String> conteudoResumo = new java.util.LinkedHashMap<>();
        for (Talha t : talhas) {
            List<Mosto> dentro = mostoRepo.findByTalhaId(t.getId());
            if (!dentro.isEmpty()) conteudoResumo.put(t.getId(), resumir(dentro));
        }
        model.addAttribute("talhas", talhas);
        model.addAttribute("conteudoResumo", conteudoResumo);
        return "fichas/talhas/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("talha", new Talha());
        preencherOpcoes(model);
        return "fichas/talhas/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Talha t = repo.findById(id).orElse(null);
        if (t == null) { ra.addFlashAttribute("erro", "Talha nao encontrada."); return "redirect:/fichas/talhas"; }
        model.addAttribute("talha", t);
        model.addAttribute("conteudos", mostoRepo.findByTalhaId(id));
        model.addAttribute("castas", castaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("estados", EstadoMosto.values());
        return "fichas/talhas/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Talha t = repo.findById(id).orElse(null);
        if (t == null) {
            ra.addFlashAttribute("erro", "Talha nao encontrada.");
            return "redirect:/fichas/talhas";
        }
        model.addAttribute("talha", t);
        preencherOpcoes(model);
        return "fichas/talhas/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("talha") Talha t, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "fichas/talhas/form";
        }
        if (t.getId() == null) {
            t.setCodigo(codigoService.proximoCodigo(Talha.PREFIXO));
        }
        repo.save(t);
        ra.addFlashAttribute("sucesso", "Talha guardada: " + t.getCodigo());
        return "redirect:/fichas/talhas";
    }

    /** Inserir manualmente conteudo (mosto ou vinho) numa talha. Cria uma ficha de Mosto e soma o volume. */
    @PostMapping("/{id}/conteudo")
    @Transactional
    public String inserirConteudo(@PathVariable Long id, @RequestParam EstadoMosto estado,
                                  @RequestParam(required = false) List<Long> castaIds,
                                  @RequestParam BigDecimal litros,
                                  @RequestParam(required = false) String vinhoNome,
                                  RedirectAttributes ra) {
        Talha t = repo.findById(id).orElse(null);
        if (t == null) { ra.addFlashAttribute("erro", "Talha nao encontrada."); return "redirect:/fichas/talhas"; }
        if (litros == null || litros.signum() <= 0) {
            ra.addFlashAttribute("erro", "Indique os litros (> 0).");
            return "redirect:/fichas/talhas/" + id;
        }
        BigDecimal volAtual = t.getVolumeAtualLitros() == null ? BigDecimal.ZERO : t.getVolumeAtualLitros();
        if (t.getCapacidadeLitros() != null && volAtual.add(litros).compareTo(t.getCapacidadeLitros()) > 0) {
            ra.addFlashAttribute("erro", String.format("Capacidade excedida: %s tem %s L de %s L, a entrar %s L.",
                    t.getIdentificacao(), volAtual.toPlainString(), t.getCapacidadeLitros().toPlainString(), litros.toPlainString()));
            return "redirect:/fichas/talhas/" + id;
        }

        List<pt.acv.adega.fichas.Casta> castas = new ArrayList<>();
        if (castaIds != null) {
            for (Long cid : castaIds) {
                if (cid != null) castaRepo.findById(cid).ifPresent(castas::add);
            }
        }

        Mosto m = new Mosto();
        m.setCodigo(codigoService.proximoCodigo(Mosto.PREFIXO));
        m.setLitros(litros);
        m.setEstado(estado);
        m.setCastas(castas);
        m.setCasta(castas.isEmpty() ? null : castas.get(0));
        m.setTalha(t);
        m.setDataProducao(LocalDateTime.now());
        m.setOrigemDescricao("Inserção manual");
        if (estado == EstadoMosto.VINHO_GRANEL && vinhoNome != null && !vinhoNome.isBlank()) {
            m.setVinhoNome(vinhoNome.trim());
        }
        mostoRepo.save(m);

        t.setVolumeAtualLitros(volAtual.add(litros));
        repo.save(t);
        ra.addFlashAttribute("sucesso", "Conteúdo inserido: " + m.getCodigo() + " · " + litros.toPlainString() + " L.");
        return "redirect:/fichas/talhas/" + id;
    }

    /** Remover conteudo inserido manualmente (nao mexe no que veio da moagem ou de movimentos). */
    @PostMapping("/{id}/conteudo/{mostoId}/remover")
    @Transactional
    public String removerConteudo(@PathVariable Long id, @PathVariable Long mostoId, RedirectAttributes ra) {
        Talha t = repo.findById(id).orElse(null);
        Mosto m = mostoRepo.findById(mostoId).orElse(null);
        if (t == null || m == null || m.getTalha() == null || !m.getTalha().getId().equals(id)) {
            ra.addFlashAttribute("erro", "Conteúdo não encontrado nesta talha.");
            return "redirect:/fichas/talhas/" + id;
        }
        if (m.getOrigemMoagemId() != null || m.getOrigemMovimentoId() != null) {
            ra.addFlashAttribute("erro", "Este conteúdo veio de um processo (moagem/movimento). Reverta esse processo em vez de o remover aqui.");
            return "redirect:/fichas/talhas/" + id;
        }
        BigDecimal vol = t.getVolumeAtualLitros() == null ? BigDecimal.ZERO : t.getVolumeAtualLitros();
        BigDecimal lit = m.getLitros() == null ? BigDecimal.ZERO : m.getLitros();
        BigDecimal novo = vol.subtract(lit);
        t.setVolumeAtualLitros(novo.signum() < 0 ? BigDecimal.ZERO : novo);
        repo.save(t);
        mostoRepo.delete(m);
        ra.addFlashAttribute("sucesso", "Conteúdo removido.");
        return "redirect:/fichas/talhas/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        if (!mostoRepo.findByTalhaId(id).isEmpty()) {
            ra.addFlashAttribute("erro", "A talha tem conteúdo. Remova o conteúdo antes de a eliminar.");
            return "redirect:/fichas/talhas/" + id;
        }
        repo.deleteById(id);
        ra.addFlashAttribute("sucesso", "Talha eliminada.");
        return "redirect:/fichas/talhas";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());
        model.addAttribute("propriedades", Propriedade.values());
    }

    /** Texto curto do que esta dentro (ex.: "Vinho pronto a granel · 500 L (Antão Vaz)"). */
    private String resumir(List<Mosto> dentro) {
        List<String> partes = new ArrayList<>();
        for (Mosto m : dentro) {
            String lit = m.getLitros() == null ? "0" : m.getLitros().toPlainString();
            String castas = m.getCastasDescricao();
            partes.add(m.getEstado().getDescricao() + " · " + lit + " L"
                    + (castas != null && !"—".equals(castas) ? " (" + castas + ")" : ""));
        }
        return String.join(" · ", partes);
    }
}
