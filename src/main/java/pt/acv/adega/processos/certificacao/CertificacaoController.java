package pt.acv.adega.processos.certificacao;

import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.AdegaRepository;
import pt.acv.adega.fichas.ContentorGarrafas;
import pt.acv.adega.fichas.ContentorGarrafasRepository;
import pt.acv.adega.fichas.TrabalhadorRepository;
import pt.acv.adega.planeamento.PlaneamentoVinho;
import pt.acv.adega.processos.loteamento.Loteamento;
import pt.acv.adega.processos.loteamento.LoteamentoRepository;
import pt.acv.adega.processos.moagem.ProcessoMoagem;
import pt.acv.adega.processos.moagem.ProcessoMoagemRepository;
import pt.acv.adega.produtos.EstadoMosto;
import pt.acv.adega.produtos.Mosto;
import pt.acv.adega.produtos.MostoRepository;
import pt.acv.adega.produtos.VinhoEngarrafado;
import pt.acv.adega.produtos.VinhoEngarrafadoRepository;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/processos/certificacao")
public class CertificacaoController {

    private final ProcessoCertificacaoRepository repo;
    private final CertificacaoService service;
    private final MostoRepository mostoRepo;
    private final VinhoEngarrafadoRepository engarrafadoRepo;
    private final AdegaRepository adegaRepo;
    private final ProcessoMoagemRepository moagemRepo;
    private final LoteamentoRepository loteRepo;
    private final ContentorGarrafasRepository contentorRepo;
    private final TrabalhadorRepository trabalhadorRepo;
    private final CodigoService codigoService;

    public CertificacaoController(ProcessoCertificacaoRepository repo, CertificacaoService service,
                                  MostoRepository mostoRepo, VinhoEngarrafadoRepository engarrafadoRepo,
                                  AdegaRepository adegaRepo, ProcessoMoagemRepository moagemRepo,
                                  LoteamentoRepository loteRepo, ContentorGarrafasRepository contentorRepo,
                                  TrabalhadorRepository trabalhadorRepo, CodigoService codigoService) {
        this.repo = repo;
        this.service = service;
        this.mostoRepo = mostoRepo;
        this.engarrafadoRepo = engarrafadoRepo;
        this.adegaRepo = adegaRepo;
        this.moagemRepo = moagemRepo;
        this.loteRepo = loteRepo;
        this.contentorRepo = contentorRepo;
        this.trabalhadorRepo = trabalhadorRepo;
        this.codigoService = codigoService;
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("processos", isAdmin(auth)
                ? repo.findAllByOrderByDataCriacaoDesc()
                : repo.findByCriadoPorOrderByDataCriacaoDesc(auth.getName()));
        model.addAttribute("admin", isAdmin(auth));
        return "processos/certificacao/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        ProcessoCertificacao p = new ProcessoCertificacao();
        p.setDataHoraInicio(LocalDateTime.now());
        model.addAttribute("cert", p);
        preencherOpcoes(model);
        return "processos/certificacao/form";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoCertificacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/certificacao"; }
        model.addAttribute("cert", p);
        return "processos/certificacao/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        ProcessoCertificacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/certificacao"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Processo fechado — não editável."); return "redirect:/processos/certificacao/" + id; }
        model.addAttribute("cert", p);
        preencherOpcoes(model);
        return "processos/certificacao/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("cert") ProcessoCertificacao cert, BindingResult result,
                          @RequestParam(value = "certificadoFicheiro", required = false) MultipartFile certificadoFicheiro,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            preencherOpcoes(model);
            return "processos/certificacao/form";
        }
        boolean granel = cert.getAlvo() == AlvoCertificacao.GRANEL;
        boolean selecaoFeita = false;

        if (granel && cert.getItemIds() != null && !cert.getItemIds().isEmpty()) {
            // A granel: depósitos/talhas escolhidos + amostra CVR/IVV.
            StringJoiner ids = new StringJoiner(",");
            StringJoiner desc = new StringJoiner("; ");
            for (Long itemId : cert.getItemIds()) {
                boolean amostra = itemId.equals(cert.getAmostraId());
                Mosto m = mostoRepo.findById(itemId).orElse(null);
                if (m != null) { ids.add(String.valueOf(itemId)); desc.add(m.getCodigo() + " · " + m.getLocalizacao() + (amostra ? " (amostra)" : "")); }
            }
            cert.setItensIdsCsv(ids.length() > 0 ? ids.toString() : null);
            cert.setItensDescricao(desc.length() > 0 ? desc.toString() : null);
            cert.setEngarrafado(null);
            cert.setContentorId(null);
            cert.setContentorDescricao(null);
            cert.setVinhoGranel(cert.getAmostraId() != null ? mostoRepo.findById(cert.getAmostraId()).orElse(null) : null);
            selecaoFeita = ids.length() > 0;
        } else if (!granel && cert.getContentorId() != null) {
            // Engarrafado: as garrafas saem de um contentor; o vinho vem daí.
            ContentorGarrafas c = contentorRepo.findById(cert.getContentorId()).orElse(null);
            VinhoEngarrafado v = (c != null && c.getVinhoEngarrafadoId() != null)
                    ? engarrafadoRepo.findById(c.getVinhoEngarrafadoId()).orElse(null) : null;
            if (v != null) {
                cert.setVinhoGranel(null);
                cert.setEngarrafado(v);
                cert.setItensIdsCsv(String.valueOf(v.getId()));
                cert.setContentorDescricao(c.getNome() + " · " + c.getLocalizacao());
                cert.setItensDescricao(v.getCodigo() + " · " + v.getNome()
                        + " · " + cert.getGarrafasCertificacao() + " garrafa(s) p/ certificação (de " + c.getNome() + ")");
                selecaoFeita = true;
            }
        }

        if (cert.getId() == null) {
            cert.setCodigo(codigoService.proximoCodigo(ProcessoCertificacao.PREFIXO));
            cert.setCriadoPor(auth.getName());
        } else {
            ProcessoCertificacao existente = repo.findById(cert.getId()).orElse(null);
            if (existente == null || !podeAceder(existente, auth)) {
                ra.addFlashAttribute("erro", "Sem acesso a este processo.");
                return "redirect:/processos/certificacao";
            }
            cert.setCriadoPor(existente.getCriadoPor());
            cert.setEstado(existente.getEstado());
            cert.setDataFecho(existente.getDataFecho());
            // Mantém a seleção anterior se nada foi submetido agora.
            if (!selecaoFeita) {
                cert.setItensIdsCsv(existente.getItensIdsCsv());
                cert.setItensDescricao(existente.getItensDescricao());
                cert.setVinhoGranel(existente.getVinhoGranel());
                cert.setEngarrafado(existente.getEngarrafado());
                cert.setContentorId(existente.getContentorId());
                cert.setContentorDescricao(existente.getContentorDescricao());
            }
            // Mantém o PDF já guardado se não vier ficheiro novo.
            cert.setCertificadoPdf(existente.getCertificadoPdf());
            cert.setCertificadoPdfNome(existente.getCertificadoPdfNome());
            cert.setCertificadoPdfTipo(existente.getCertificadoPdfTipo());
        }

        // Novo PDF (substitui o anterior, se houver).
        if (certificadoFicheiro != null && !certificadoFicheiro.isEmpty()) {
            try {
                cert.setCertificadoPdf(certificadoFicheiro.getBytes());
                cert.setCertificadoPdfNome(certificadoFicheiro.getOriginalFilename());
                cert.setCertificadoPdfTipo(certificadoFicheiro.getContentType());
            } catch (java.io.IOException e) {
                ra.addFlashAttribute("erro", "Não foi possível ler o ficheiro do certificado.");
                return "redirect:/processos/certificacao";
            }
        }

        repo.save(cert);
        ra.addFlashAttribute("sucesso", "Certificação guardada: " + cert.getCodigo());
        return "redirect:/processos/certificacao/" + cert.getId();
    }

    @GetMapping("/{id}/certificado")
    public ResponseEntity<ByteArrayResource> descarregarCertificado(@PathVariable Long id, Authentication auth) {
        ProcessoCertificacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth) || !p.isTemPdf()) return ResponseEntity.notFound().build();
        String nome = p.getCertificadoPdfNome() != null ? p.getCertificadoPdfNome() : ("certificado-" + p.getCodigo() + ".pdf");
        MediaType tipo = p.getCertificadoPdfTipo() != null
                ? MediaType.parseMediaType(p.getCertificadoPdfTipo()) : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nome).build().toString())
                .body(new ByteArrayResource(p.getCertificadoPdf()));
    }

    @PostMapping("/{id}/fechar")
    public String fechar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoCertificacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/certificacao"; }
        try {
            service.fechar(id);
            ra.addFlashAttribute("sucesso", "Certificação fechada. Resultado registado no produto.");
        } catch (CertificacaoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/certificacao/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) { ra.addFlashAttribute("erro", "Apenas o administrador pode reabrir."); return "redirect:/processos/certificacao/" + id; }
        try {
            service.reabrir(id);
            ra.addFlashAttribute("sucesso", "Certificação reaberta. Marcação anulada no produto.");
        } catch (CertificacaoException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/processos/certificacao/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        ProcessoCertificacao p = repo.findById(id).orElse(null);
        if (p == null || !podeAceder(p, auth)) { ra.addFlashAttribute("erro", "Sem acesso a este processo."); return "redirect:/processos/certificacao"; }
        if (!p.isAberto()) { ra.addFlashAttribute("erro", "Reabra antes de eliminar (para anular a marcação)."); return "redirect:/processos/certificacao/" + id; }
        repo.delete(p);
        ra.addFlashAttribute("sucesso", "Certificação eliminada.");
        return "redirect:/processos/certificacao";
    }

    private void preencherOpcoes(Model model) {
        model.addAttribute("alvos", AlvoCertificacao.values());
        model.addAttribute("resultados", ResultadoCertificacao.values());
        model.addAttribute("trabalhadores", trabalhadorRepo.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("adegas", adegaRepo.findAllByOrderByNomeAsc());

        // GRANEL: depósitos/talhas com vinho a granel ainda NÃO certificado, por adega + vinho.
        Map<Long, PlaneamentoVinho> moagemPlano = new HashMap<>();
        for (ProcessoMoagem mo : moagemRepo.findAll()) {
            if (mo.getPlano() != null) moagemPlano.put(mo.getId(), mo.getPlano());
        }
        // Os vinhos dos lotes construidos (Fase 6) tambem se certificam: nao vem de
        // uma moagem, vem do lote. Ficam com chave negativa (-idDoLote) para nao
        // colidir com os ids dos vinhos planeados.
        Map<String, Loteamento> lotePorCodigo = new HashMap<>();
        for (Loteamento lt : loteRepo.findAll()) {
            if (lt.getCodigo() != null) lotePorCodigo.put(lt.getCodigo(), lt);
        }
        // A escolha e por adega + vinho, e o vinho identifica-se pelo nome (que por
        // regra nunca se repete, loteado ou nao). O nome pode vir do lote, do
        // planeamento da moagem, ou do proprio mosto (ex.: entrada da Fase 5).
        Map<String, String> vinhoNome = new LinkedHashMap<>();
        List<Map<String, Object>> granel = new ArrayList<>();
        for (Mosto m : mostoRepo.findByEstadoOrderByDataProducaoDesc(EstadoMosto.VINHO_GRANEL)) {
            if (m.isCertificado()) continue;
            Long adegaId = null;
            String local = "—";
            if (m.getTalha() != null && m.getTalha().getAdega() != null) { adegaId = m.getTalha().getAdega().getId(); local = "Talha " + m.getTalha().getIdentificacao(); }
            else if (m.getDeposito() != null && m.getDeposito().getAdega() != null) { adegaId = m.getDeposito().getAdega().getId(); local = "Depósito " + m.getDeposito().getIdentificacao(); }
            if (adegaId == null) continue;

            Loteamento lt = m.getLoteCodigo() != null ? lotePorCodigo.get(m.getLoteCodigo()) : null;
            PlaneamentoVinho w = m.getOrigemMoagemId() != null ? moagemPlano.get(m.getOrigemMoagemId()) : null;
            String nome;
            String etiqueta;
            if (lt != null) {
                nome = lt.getNome();
                etiqueta = lt.getNome() + " (lote " + lt.getCodigo() + ")";
            } else if (w != null) {
                nome = w.getNomeVinho();
                etiqueta = w.getNomeVinho();
            } else {
                nome = m.getVinhoNome();
                etiqueta = m.getVinhoNome();
            }
            if (nome == null || nome.isBlank()) continue;

            vinhoNome.putIfAbsent(nome, etiqueta);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("adegaId", adegaId);
            row.put("vinhoId", nome);
            row.put("label", m.getCodigo() + " · " + local + " · " + (m.getLitros() == null ? "0" : m.getLitros().toPlainString()) + " L");
            granel.add(row);
        }
        List<Map<String, Object>> vinhos = new ArrayList<>();
        for (Map.Entry<String, String> e : vinhoNome.entrySet()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", e.getKey());
            v.put("nome", e.getValue());
            vinhos.add(v);
        }
        model.addAttribute("vinhos", vinhos);
        model.addAttribute("granelDisponivel", granel);

        // ENGARRAFADO: contentores com garrafas de vinho engarrafado ainda NÃO
        // certificado — é de um contentor que saem as garrafas para certificação.
        Map<Long, Boolean> engCertificado = new HashMap<>();
        for (VinhoEngarrafado v : engarrafadoRepo.findAll()) engCertificado.put(v.getId(), v.isCertificado());
        List<Map<String, Object>> contentores = new ArrayList<>();
        for (ContentorGarrafas c : contentorRepo.findAllByOrderByNomeAsc()) {
            if (c.getGarrafasAtuais() <= 0 || c.getVinhoEngarrafadoId() == null) continue;
            if (Boolean.TRUE.equals(engCertificado.get(c.getVinhoEngarrafadoId()))) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("garrafas", c.getGarrafasAtuais());
            row.put("label", c.getNome() + " · " + (c.getVinhoNome() != null ? c.getVinhoNome() : "vinho")
                    + " · " + c.getLocalizacao() + " · " + c.getGarrafasAtuais() + " garrafas");
            contentores.add(row);
        }
        model.addAttribute("contentoresDisponiveis", contentores);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeAceder(ProcessoCertificacao p, Authentication auth) {
        return isAdmin(auth) || auth.getName().equals(p.getCriadoPor());
    }
}
