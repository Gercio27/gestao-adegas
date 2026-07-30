package pt.acv.adega.processos.comercial;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.ContentorService;
import pt.acv.adega.fichas.TipoEmbalagem;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.produtos.StockRotulado;
import pt.acv.adega.produtos.StockRotuladoRepository;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/processos/comercial")
public class ComercialController {

    private final ProcessoPassagemComercialRepository repo;
    private final ComercialService service;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final ContentorService contentorService;
    private final StockRotuladoRepository stockRepo;
    private final CodigoService codigoService;

    public ComercialController(ProcessoPassagemComercialRepository repo, ComercialService service,
                               VinhoEngarrafadoRepository engarrafadoRepo, TrabalhadorRepository trabalhadorRepo,
                               ContentorService contentorService, StockRotuladoRepository stockRepo,
                               CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.engarrafadoRepo = engarrafadoRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.contentorService = contentorService;
        this.stockRepo = stockRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/comercial/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoPassagemComercial p = new ProcessoPassagemComercial();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("comercial", p);
        preencherOpcoes(model);
        return "processos/comercial/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoPassagemComercial p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/comercial"; }
        model.addAttribute("comercial", p);
        return "processos/comercial/detalhe";
    }

    /**
     * Historico das entregas: quem levou, que vinho, quantas garrafas e quando.
     * Com filtros por cliente, vinho, datas e estado, e totais no fim.
     */
    @GetMapping("/historico")
    public String historico(Authentication auth,
                            @RequestParam(required = false) String cliente,
                            @RequestParam(required = false) String vinho,
                            @RequestParam(required = false) String de,
                            @RequestParam(required = false) String ate,
                            @RequestParam(required = false) String estado,
                            Model model) {
        List<ProcessoPassagemComercial> todos = isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName());

        java.time.LocalDate dataDe = parseData(de);
        java.time.LocalDate dataAte = parseData(ate);

        List<ProcessoPassagemComercial> linhas = new ArrayList<>();
        for (ProcessoPassagemComercial p : todos) {
            if (contem(cliente) && !contemTexto(p.getDestinatario(), cliente)) continue;
            if (contem(vinho) && !contemTexto(p.getEngarrafado() != null ? p.getEngarrafado().getNome() : null, vinho)) continue;
            java.time.LocalDate data = dataDaEntrega(p);
            if (dataDe != null && (data == null || data.isBefore(dataDe))) continue;
            if (dataAte != null && (data == null || data.isAfter(dataAte))) continue;
            if ("FECHADO".equals(estado) && p.isAberto()) continue;
            if ("ABERTO".equals(estado) && !p.isAberto()) continue;
            linhas.add(p);
        }
        model.addAttribute("linhas", linhas);

        // Totais: so' contam as entregas ja fechadas (as abertas ainda nao sairam).
        int totalGarrafas = 0, totalUnidades = 0, fechadas = 0;
        Map<String, Integer> porCliente = new LinkedHashMap<>();
        Map<String, Integer> porVinho = new LinkedHashMap<>();
        for (ProcessoPassagemComercial p : linhas) {
            if (p.isAberto()) continue;
            fechadas++;
            if (p.getTipoEmbalagem() == TipoEmbalagem.BAG_IN_BOX) totalUnidades += p.getQuantidadeGarrafas();
            else totalGarrafas += p.getQuantidadeGarrafas();
            String c = p.getDestinatario() != null && !p.getDestinatario().isBlank() ? p.getDestinatario() : "(sem cliente)";
            porCliente.merge(c, p.getQuantidadeGarrafas(), Integer::sum);
            String v = p.getEngarrafado() != null ? p.getEngarrafado().getNome() : "(sem vinho)";
            porVinho.merge(v, p.getQuantidadeGarrafas(), Integer::sum);
        }
        model.addAttribute("totalGarrafas", totalGarrafas);
        model.addAttribute("totalUnidades", totalUnidades);
        model.addAttribute("entregasFechadas", fechadas);
        model.addAttribute("porCliente", porCliente);
        model.addAttribute("porVinho", porVinho);

        model.addAttribute("cliente", cliente);
        model.addAttribute("vinho", vinho);
        model.addAttribute("de", de);
        model.addAttribute("ate", ate);
        model.addAttribute("estadoFiltro", estado);
        return "processos/comercial/historico";
    }

    /** Data que conta para o historico: a do fecho, ou a de inicio se ainda estiver aberta. */
    private java.time.LocalDate dataDaEntrega(ProcessoPassagemComercial p) {
        if (p.getDataFecho() != null) return p.getDataFecho().toLocalDate();
        if (p.getDataHoraFim() != null) return p.getDataHoraFim().toLocalDate();
        if (p.getDataHoraInicio() != null) return p.getDataHoraInicio().toLocalDate();
        return null;
    }

    private boolean contem(String s) { return s != null && !s.isBlank(); }

    private boolean contemTexto(String campo, String procura) {
        return campo != null && campo.toLowerCase().contains(procura.trim().toLowerCase());
    }

    private java.time.LocalDate parseData(String s) {
        if (s == null || s.isBlank()) return null;
        try { return java.time.LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }

    /** Nota de entrega imprimivel (imprimir / guardar como PDF pelo browser). */
    @GetMapping("/{id}/nota")
    public String nota(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoPassagemComercial p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/comercial"; }
        if (p.getNumeroNota() == null) { ra.addFlashAttribute("erro", "A nota só é emitida ao fechar o processo."); return "redirect:/processos/comercial/" + id; }
        model.addAttribute("comercial", p);
        return "processos/comercial/nota";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoPassagemComercial p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/comercial"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/comercial/" + id; }
        model.addAttribute("comercial", p);
        preencherOpcoes(model);
        return "processos/comercial/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("comercial") ProcessoPassagemComercial com, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/comercial/form";
        }
        // A partir do contentor escolhido, resolve o vinho engarrafado e o local de origem.
        TipoEmbalagem tipo = com.getTipoEmbalagem() != null ? com.getTipoEmbalagem() : TipoEmbalagem.GARRAFA;
        if (tipo == TipoEmbalagem.GARRAFA) {
            com.setContentorId(null);
            StockRotulado st = com.getStockRotuladoId() != null
                    ? stockRepo.findById(com.getStockRotuladoId()).orElse(null) : null;
            if (st != null) {
                com.setOrigemDescricao(st.getDescricao());
                com.setEngarrafado(engarrafadoRepo.findById(st.getVinhoEngarrafadoId()).orElse(null));
            }
        } else {
            com.setStockRotuladoId(null);
            ContentorService.Opcao c = contentorService.procurar(tipo, com.getContentorId());
            if (c != null) {
                com.setOrigemDescricao(c.label());
                Long vegId = contentorService.vinhoDe(tipo, com.getContentorId());
                if (vegId != null) com.setEngarrafado(engarrafadoRepo.findById(vegId).orElse(null));
            }
        }
        if (com.getId() == null) {
            com.setCodigo(codigoService.proximoCodigo(ProcessoPassagemComercial.PREFIXO));
            com.setCriadoPor(auth.getName());
        } else {
            ProcessoPassagemComercial existente = repo.findById(com.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/comercial";
            }
            com.setCriadoPor(existente.getCriadoPor());
            com.setEstado(existente.getEstado());
            com.setDataFecho(existente.getDataFecho());
            com.setNumeroNota(existente.getNumeroNota());
        }
        repo.save(com);
        ra.addFlashAttribute("sucesso", "Registo guardado: " + com.getCodigo());
        return "redirect:/processos/comercial/" + com.getId();
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoPassagemComercial p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/comercial"; }
        try {
            service.fechar(id);
            ra.addFlashAttribute("sucesso", "Entrega registada. Nota de entrega emitida.");
        } catch (ComercialException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/comercial/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/comercial/" + id; }
        try {
            service.reabrir(id);
            ra.addFlashAttribute("sucesso", "Reaberto. Garrafas repostas no stock disponível.");
        } catch (ComercialException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/comercial/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoPassagemComercial p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/comercial"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra o processo antes de o eliminar (para repor o stock)."); return "redirect:/processos/comercial/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Registo eliminado.");
        return "redirect:/processos/comercial";
    }

    private void preencherOpcoes(Model model) {
        // Disponivel para entrega: contentores rotulados com stock, organizados
        // por tipo → local (adega/armazém) → nome do vinho. Sem isto vinha tudo
        // de todos os armazéns numa lista só, misturado.
        Map<String, Object> porTipo = new LinkedHashMap<>();
        Map<String, Object> locaisPorTipo = new LinkedHashMap<>();
        for (TipoEmbalagem tipo : TipoEmbalagem.values()) {
            Map<String, String> nomesLocais = contentorService.nomesDosLocais(tipo);
            Map<String, Map<String, List<Map<String, Object>>>> porLocal = new LinkedHashMap<>();
            if (tipo == TipoEmbalagem.GARRAFA) {
                // Garrafas: ja estao rotuladas e em caixas, no local — vem do stock rotulado.
                for (StockRotulado st : stockRepo.findByGarrafasGreaterThanOrderByVinhoNomeAsc(0)) {
                    String ref = st.getLocalRef();
                    if (ref.isEmpty()) continue;
                    nomesLocais.putIfAbsent(ref, st.getLocalNome());
                    String vinho = st.getVinhoNome() != null ? st.getVinhoNome() : "(vinho sem nome)";
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", st.getId());
                    row.put("engarrafadoId", st.getVinhoEngarrafadoId());
                    row.put("label", st.getGarrafas() + " garrafas (" + st.getCaixasInteiras() + " caixas) · " + st.getLocalNome());
                    porLocal.computeIfAbsent(ref, k -> new LinkedHashMap<>())
                            .computeIfAbsent(vinho, k -> new ArrayList<>()).add(row);
                }
            } else {
            for (ContentorService.Opcao o : contentorService.rotuladosComStock(tipo)) {
                String ref = contentorService.localDe(tipo, o.id());
                if (ref == null || ref.isEmpty()) continue;
                String vinho = o.vinhoNome() != null ? o.vinhoNome() : "(vinho sem nome)";
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", o.id());
                row.put("engarrafadoId", contentorService.vinhoDe(tipo, o.id()));
                row.put("label", o.label());
                porLocal.computeIfAbsent(ref, k -> new LinkedHashMap<>())
                        .computeIfAbsent(vinho, k -> new ArrayList<>()).add(row);
            }
            }
            List<Map<String, Object>> locais = new ArrayList<>();
            for (String ref : porLocal.keySet()) {
                Map<String, Object> l = new LinkedHashMap<>();
                l.put("ref", ref);
                l.put("nome", nomesLocais.getOrDefault(ref, ref));
                locais.add(l);
            }
            porTipo.put(tipo.name(), porLocal);
            locaisPorTipo.put(tipo.name(), locais);
        }
        model.addAttribute("porTipoLocalEVinho", porTipo);
        model.addAttribute("locaisPorTipo", locaisPorTipo);
        model.addAttribute("tiposEmbalagem", TipoEmbalagem.values());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoPassagemComercial p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
