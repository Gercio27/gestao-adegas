package pt.acv.adega.processos.engarrafamento;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.ContentorGarrafas;
import pt.acv.adega.fichas.ContentorGarrafasRepository;
import pt.acv.adega.fichas.ConsumivelRepository;
import pt.acv.adega.fichas.TipoConsumivel;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.fichas.AdegaRepository;
import pt.acv.adega.planeamento.PlaneamentoVinho;
import pt.acv.adega.processos.moagem.ProcessoMoagem;
import pt.acv.adega.processos.moagem.ProcessoMoagemRepository;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/processos/engarrafamento")
public class EngarrafamentoController {

    private final ProcessoEngarrafamentoRepository repo;
    private final EngarrafamentoService service;
    private final MostoRepository mostoRepo;
    private final ConsumivelRepository consumivelRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final AdegaRepository adegaRepo;
    private final ProcessoMoagemRepository moagemRepo;
    private final ContentorGarrafasRepository contentorRepo;
    private final CodigoService codigoService;

    public EngarrafamentoController(ProcessoEngarrafamentoRepository repo, EngarrafamentoService service,
                                    MostoRepository mostoRepo, ConsumivelRepository consumivelRepo,
                                    TrabalhadorRepository trabalhadorRepo, VinhoEngarrafadoRepository engarrafadoRepo,
                                    AdegaRepository adegaRepo, ProcessoMoagemRepository moagemRepo,
                                    ContentorGarrafasRepository contentorRepo, CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.mostoRepo = mostoRepo;
        this.consumivelRepo = consumivelRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.adegaRepo = adegaRepo;
        this.moagemRepo = moagemRepo;
        this.contentorRepo = contentorRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/engarrafamento/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        ProcessoEngarrafamento p = new ProcessoEngarrafamento();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("engarrafamento", p);
        preencherOpcoes(model);
        return "processos/engarrafamento/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoEngarrafamento p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/engarrafamento"; }
        model.addAttribute("engarrafamento", p);
        model.addAttribute("engarrafados", engarrafadoRepo.findByOrigemEngarrafamentoId(p.getId()));
        return "processos/engarrafamento/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoEngarrafamento p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/engarrafamento"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/engarrafamento/" + id; }
        model.addAttribute("engarrafamento", p);
        preencherOpcoes(model);
        return "processos/engarrafamento/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("engarrafamento") ProcessoEngarrafamento eng, BindingResult result,
                          @RequestParam(value = "distribuicaoInput", required = false) String distribuicaoInput,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/engarrafamento/form";
        }
        // Distribuicao das garrafas por contentor (opcional): "id:qtd,id:qtd".
        aplicarDistribuicao(eng, distribuicaoInput);

        if (eng.getId() == null) {
            eng.setCodigo(codigoService.proximoCodigo(ProcessoEngarrafamento.PREFIXO));
            eng.setCriadoPor(auth.getName());
        } else {
            ProcessoEngarrafamento existente = repo.findById(eng.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/engarrafamento";
            }
            eng.setCriadoPor(existente.getCriadoPor());
            eng.setEstado(existente.getEstado());
            eng.setDataFecho(existente.getDataFecho());
            // Mantem a distribuicao anterior se nada foi submetido agora.
            if (distribuicaoInput == null || distribuicaoInput.isBlank()) {
                eng.setDistribuicaoContentores(existente.getDistribuicaoContentores());
                eng.setContentoresDescricao(existente.getContentoresDescricao());
                if (existente.getDistribuicaoContentores() != null && eng.getNumeroGarrafas() <= 0) {
                    eng.setNumeroGarrafas(existente.getNumeroGarrafas());
                }
            }
        }
        repo.save(eng);
        ra.addFlashAttribute("sucesso", "Engarrafamento guardado: " + eng.getCodigo());
        return "redirect:/processos/engarrafamento/" + eng.getId();
    }

    /** Interpreta a distribuicao "id:qtd,id:qtd", define o total de garrafas e a descricao. */
    private void aplicarDistribuicao(ProcessoEngarrafamento eng, String distribuicaoInput) {
        if (distribuicaoInput == null || distribuicaoInput.isBlank()) return;
        StringJoiner csv = new StringJoiner(";");
        StringJoiner desc = new StringJoiner("; ");
        int total = 0;
        for (String par : distribuicaoInput.split(",")) {
            String[] kv = par.split(":");
            if (kv.length != 2) continue;
            Long cid;
            int qtd;
            try { cid = Long.valueOf(kv[0].trim()); qtd = Integer.parseInt(kv[1].trim()); }
            catch (Exception ex) { continue; }
            if (qtd <= 0) continue;
            ContentorGarrafas c = contentorRepo.findById(cid).orElse(null);
            if (c == null) continue;
            csv.add(cid + ":" + qtd);
            desc.add(c.getNome() + " (" + qtd + " garrafas)");
            total += qtd;
        }
        if (total > 0) {
            eng.setDistribuicaoContentores(csv.toString());
            eng.setContentoresDescricao(desc.toString());
            eng.setNumeroGarrafas(total); // o total de garrafas vem da distribuicao
        }
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean forcar,
                         Authentication auth, RedirectAttributes ra) {
        ProcessoEngarrafamento p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/engarrafamento"; }
        try {
            service.fechar(id, forcar);
            ra.addFlashAttribute("sucesso", "Engarrafamento fechado. Baixa de vinho, garrafas e rolhas; vinho engarrafado criado e colocado nos contentores.");
        } catch (EngarrafamentoException ex) {
            if (ex.isCapacidade()) {
                // Só um aviso de capacidade: deixa o utilizador fechar mesmo assim.
                ra.addFlashAttribute("avisoCapacidade", ex.getMessage());
            } else {
                ra.addFlashAttribute("erro", ex.getMessage());
            }
        }
        return "redirect:/processos/engarrafamento/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/engarrafamento/" + id; }
        try {
            service.reabrir(id);
            ra.addFlashAttribute("sucesso", "Engarrafamento reaberto. Vinho, garrafas e rolhas repostos; engarrafado anulado.");
        } catch (EngarrafamentoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/engarrafamento/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoEngarrafamento p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/engarrafamento"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra o processo antes de o eliminar (para repor stocks/vinho)."); return "redirect:/processos/engarrafamento/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Engarrafamento eliminado.");
        return "redirect:/processos/engarrafamento";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("garrafas", consumivelRepo.findByTipoOrderByDescricaoAsc(TipoConsumivel.GARRAFA));
        model.addAttribute("rolhas", consumivelRepo.findByTipoOrderByDescricaoAsc(TipoConsumivel.ROLHA));
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());

        // Vinhos prontos a granel (nao certificados nao e requisito aqui), por adega + vinho.
        Map<Long, PlaneamentoVinho> moagemPlano = new HashMap<>();
        for (ProcessoMoagem mo : moagemRepo.findAll()) {
            if (mo.getPlano() != null) moagemPlano.put(mo.getId(), mo.getPlano());
        }
        Map<Long, String> vinhoNome = new LinkedHashMap<>();
        List<Map<String, Object>> granel = new ArrayList<>();
        for (Mosto m : mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL)) {
            Long adegaId = null;
            String local = "—";
            if (m.getTalha() != null && m.getTalha().getAdega() != null) { adegaId = m.getTalha().getAdega().getId(); local = "Talha " + m.getTalha().getIdentificacao(); }
            else if (m.getDeposito() != null && m.getDeposito().getAdega() != null) { adegaId = m.getDeposito().getAdega().getId(); local = "Depósito " + m.getDeposito().getIdentificacao(); }
            PlaneamentoVinho w = m.getOrigemMoagemId() != null ? moagemPlano.get(m.getOrigemMoagemId()) : null;
            if (adegaId == null) continue;
            Long vinhoId = w != null ? w.getId() : null;
            if (w != null) vinhoNome.putIfAbsent(w.getId(), w.getNomeVinho());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("adegaId", adegaId);
            row.put("vinhoId", vinhoId);
            row.put("label", m.getCodigo() + " · " + local + " · " + (m.getLitros() == null ? "0" : m.getLitros().toPlainString()) + " L");
            granel.add(row);
        }
        List<Map<String, Object>> vinhos = new ArrayList<>();
        for (Map.Entry<Long, String> e : vinhoNome.entrySet()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", e.getKey());
            v.put("nome", e.getValue());
            vinhos.add(v);
        }
        model.addAttribute("vinhos", vinhos);
        model.addAttribute("granelDisponivel", granel);

        // Contentores com espaco livre, para distribuir as garrafas.
        List<Map<String, Object>> contentores = new ArrayList<>();
        for (ContentorGarrafas c : contentorRepo.findAllByOrderByNomeAsc()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("label", c.getCodigo() + " · " + c.getNome() + " · " + c.getLocalizacao()
                    + " · livre " + c.getEspacoLivre() + "/" + c.getCapacidadeGarrafas());
            contentores.add(row);
        }
        model.addAttribute("contentores", contentores);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoEngarrafamento p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
