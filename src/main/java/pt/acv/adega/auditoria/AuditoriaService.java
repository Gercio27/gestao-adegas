package pt.acv.adega.auditoria;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.acv.adega.security.UtilizadorRepository;

import java.time.LocalDateTime;

/**
 * Grava os registos de auditoria na base de dados (numa transacao propria, para
 * ficarem persistidos independentemente do pedido que os originou) e resolve o
 * nome do utilizador a partir do username.
 */
@Service
public class AuditoriaService {

    private final RegistoAuditoriaRepository repo;
    private final UtilizadorRepository utilizadorRepo;

    public AuditoriaService(RegistoAuditoriaRepository repo, UtilizadorRepository utilizadorRepo) {
        this.repo = repo;
        this.utilizadorRepo = utilizadorRepo;
    }

    @Transactional
    public void registar(String username, String metodo, String caminho,
                         String descricao, int estado, String ip) {
        RegistoAuditoria r = new RegistoAuditoria();
        r.setDataHora(LocalDateTime.now());
        r.setUsername(username);
        r.setNomeUtilizador(nomeDe(username));
        r.setMetodo(metodo);
        r.setCaminho(caminho);
        r.setDescricao(descricao);
        r.setEstado(estado);
        r.setIp(ip);
        repo.save(r);
    }

    private String nomeDe(String username) {
        if (username == null) return "—";
        return utilizadorRepo.findByUsername(username)
                .map(u -> u.getNome())
                .orElse(username);
    }
}
