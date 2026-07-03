package pt.acv.adega.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilizadorRepository repo;

    public CustomUserDetailsService(UtilizadorRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilizador u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador nao encontrado: " + username));
        return User.builder()
                .username(u.getUsername())
                .password(u.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(u.getPerfil().authority())))
                .disabled(!u.isAtivo())
                .build();
    }
}
