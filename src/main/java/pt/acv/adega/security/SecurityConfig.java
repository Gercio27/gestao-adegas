package pt.acv.adega.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/webjars/**", "/img/**", "/data/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/utilizadores/**").hasRole("ADMIN")
                .requestMatchers("/auditoria/**").hasRole("ADMIN")
                // Planeamento: ver é para todos; criar/alterar/eliminar só admin.
                .requestMatchers("/planeamento/nova", "/planeamento/*/editar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/planeamento", "/planeamento/*/eliminar").hasRole("ADMIN")
                // Saldo inicial de produtos (adega a meio): ver é para todos; alterar só admin.
                .requestMatchers("/produtos/mostos/saldo-inicial", "/produtos/mostos/*/editar",
                        "/produtos/engarrafados/saldo-inicial", "/produtos/engarrafados/*/editar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/produtos/mostos/saldo-inicial", "/produtos/mostos/*/eliminar",
                        "/produtos/engarrafados/saldo-inicial", "/produtos/engarrafados/*/eliminar").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // Necessario para a consola H2 (usa frames) e para o form da consola
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")));
        return http.build();
    }
}
