package pt.acv.adega.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pt.acv.adega.common.CodigoService;
import pt.acv.adega.fichas.Casta;
import pt.acv.adega.fichas.CastaRepository;
import pt.acv.adega.fichas.CorCasta;
import pt.acv.adega.security.Perfil;
import pt.acv.adega.security.Utilizador;
import pt.acv.adega.security.UtilizadorRepository;

import java.util.List;

/**
 * Carrega dados iniciais no primeiro arranque:
 *  - Utilizador administrador (definido em application.yml).
 *  - Lista (inicial) de castas nacionais por ordem alfabetica.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UtilizadorRepository utilizadorRepo;
    private final CastaRepository castaRepo;
    private final CodigoService codigoService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}") private String adminUser;
    @Value("${app.admin.password}") private String adminPass;
    @Value("${app.admin.nome}") private String adminNome;

    public DataSeeder(UtilizadorRepository utilizadorRepo, CastaRepository castaRepo,
                      CodigoService codigoService, PasswordEncoder passwordEncoder) {
        this.utilizadorRepo = utilizadorRepo;
        this.castaRepo = castaRepo;
        this.codigoService = codigoService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        criarAdmin();
        carregarCastas();
    }

    private void criarAdmin() {
        if (!utilizadorRepo.existsByUsername(adminUser)) {
            Utilizador u = new Utilizador();
            u.setUsername(adminUser);
            u.setPassword(passwordEncoder.encode(adminPass));
            u.setNome(adminNome);
            u.setPerfil(Perfil.ADMIN);
            u.setAtivo(true);
            utilizadorRepo.save(u);
        }
    }

    private void carregarCastas() {
        if (castaRepo.count() > 0) return;
        for (CastaInicial ci : CASTAS_INICIAIS) {
            Casta c = new Casta();
            c.setCodigo(codigoService.proximoCodigo(Casta.PREFIXO));
            c.setNome(ci.nome());
            c.setCor(ci.cor());
            castaRepo.save(c);
        }
    }

    private record CastaInicial(String nome, CorCasta cor) { }

    /**
     * Seleccao inicial de castas (com enfase nas do Alentejo / vinho de talha),
     * por ordem alfabetica. Lista ampliavel/editavel pelo utilizador.
     */
    private static final List<CastaInicial> CASTAS_INICIAIS = List.of(
            // --- Brancas ---
            new CastaInicial("Alvarinho", CorCasta.BRANCA),
            new CastaInicial("Antão Vaz", CorCasta.BRANCA),
            new CastaInicial("Arinto", CorCasta.BRANCA),
            new CastaInicial("Avesso", CorCasta.BRANCA),
            new CastaInicial("Azal", CorCasta.BRANCA),
            new CastaInicial("Batoca", CorCasta.BRANCA),
            new CastaInicial("Bical", CorCasta.BRANCA),
            new CastaInicial("Boal", CorCasta.BRANCA),
            new CastaInicial("Cerceal", CorCasta.BRANCA),
            new CastaInicial("Chardonnay", CorCasta.BRANCA),
            new CastaInicial("Códega", CorCasta.BRANCA),
            new CastaInicial("Códega do Larinho", CorCasta.BRANCA),
            new CastaInicial("Diagalves", CorCasta.BRANCA),
            new CastaInicial("Dona Branca", CorCasta.BRANCA),
            new CastaInicial("Encruzado", CorCasta.BRANCA),
            new CastaInicial("Fernão Pires", CorCasta.BRANCA),
            new CastaInicial("Folgasão", CorCasta.BRANCA),
            new CastaInicial("Gouveio", CorCasta.BRANCA),
            new CastaInicial("Loureiro", CorCasta.BRANCA),
            new CastaInicial("Malvasia", CorCasta.BRANCA),
            new CastaInicial("Malvasia Fina", CorCasta.BRANCA),
            new CastaInicial("Manteúdo", CorCasta.BRANCA),
            new CastaInicial("Moscatel Galego Branco", CorCasta.BRANCA),
            new CastaInicial("Moscatel Graúdo", CorCasta.BRANCA),
            new CastaInicial("Perrum", CorCasta.BRANCA),
            new CastaInicial("Rabigato", CorCasta.BRANCA),
            new CastaInicial("Rabo de Ovelha", CorCasta.BRANCA),
            new CastaInicial("Ratinho", CorCasta.BRANCA),
            new CastaInicial("Roupeiro", CorCasta.BRANCA),
            new CastaInicial("Samarrinho", CorCasta.BRANCA),
            new CastaInicial("Sauvignon Blanc", CorCasta.BRANCA),
            new CastaInicial("Semillon", CorCasta.BRANCA),
            new CastaInicial("Sercial", CorCasta.BRANCA),
            new CastaInicial("Síria", CorCasta.BRANCA),
            new CastaInicial("Tamarez", CorCasta.BRANCA),
            new CastaInicial("Terrantez", CorCasta.BRANCA),
            new CastaInicial("Trajadura", CorCasta.BRANCA),
            new CastaInicial("Verdelho", CorCasta.BRANCA),
            new CastaInicial("Verdial", CorCasta.BRANCA),
            new CastaInicial("Viosinho", CorCasta.BRANCA),
            new CastaInicial("Vital", CorCasta.BRANCA),
            // --- Tintas ---
            new CastaInicial("Alfrocheiro", CorCasta.TINTA),
            new CastaInicial("Alicante Bouschet", CorCasta.TINTA),
            new CastaInicial("Alvarelhão", CorCasta.TINTA),
            new CastaInicial("Aragonez", CorCasta.TINTA),
            new CastaInicial("Baga", CorCasta.TINTA),
            new CastaInicial("Bastardo", CorCasta.TINTA),
            new CastaInicial("Cabernet Sauvignon", CorCasta.TINTA),
            new CastaInicial("Camarate", CorCasta.TINTA),
            new CastaInicial("Castelão", CorCasta.TINTA),
            new CastaInicial("Cornifesto", CorCasta.TINTA),
            new CastaInicial("Espadeiro", CorCasta.TINTA),
            new CastaInicial("Grand Noir", CorCasta.TINTA),
            new CastaInicial("Jaen", CorCasta.TINTA),
            new CastaInicial("Malbec", CorCasta.TINTA),
            new CastaInicial("Marufo", CorCasta.TINTA),
            new CastaInicial("Merlot", CorCasta.TINTA),
            new CastaInicial("Moreto", CorCasta.TINTA),
            new CastaInicial("Petit Verdot", CorCasta.TINTA),
            new CastaInicial("Preto Martinho", CorCasta.TINTA),
            new CastaInicial("Rufete", CorCasta.TINTA),
            new CastaInicial("Sousão", CorCasta.TINTA),
            new CastaInicial("Syrah", CorCasta.TINTA),
            new CastaInicial("Tinta Barroca", CorCasta.TINTA),
            new CastaInicial("Tinta Caiada", CorCasta.TINTA),
            new CastaInicial("Tinta Carvalha", CorCasta.TINTA),
            new CastaInicial("Tinta Francisca", CorCasta.TINTA),
            new CastaInicial("Tinta Miúda", CorCasta.TINTA),
            new CastaInicial("Tinto Cão", CorCasta.TINTA),
            new CastaInicial("Touriga Franca", CorCasta.TINTA),
            new CastaInicial("Touriga Nacional", CorCasta.TINTA),
            new CastaInicial("Trincadeira", CorCasta.TINTA),
            new CastaInicial("Vinhão", CorCasta.TINTA)
    );
}
