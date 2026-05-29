package com.grupo3aor.innovationlab.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * Criei esta classe para tratar de toda a Configuração de Segurança da nossa aplicação.
 *
 * Criei esta classe para centralizar e gerir toda a segurança web do projeto.
 * É aqui que eu tenho o poder de decidir:
 *   - Quais os URLs que vão ficar públicos (sem precisarem de login)
 *   - Quais os URLs que vou proteger (que vão requerer login obrigatório)
 *   - Que o algoritmo que vamos usar para encriptar as nossas passwords neste caso o BCrypt.
 *
 * NOTA: Por agora, como estamos no início, decidi deixar a segurança em modo de DESENVOLVIMENTO.
 *       Mantenho o H2 Console acessível à vontade para podermos fazer os nossos testes.
 *       
 */
@Configuration      // Com isto digo ao Spring: "Atenção, esta classe contém configurações!"
@EnableWebSecurity  // E com isto ligo imediatamente todo o sistema de segurança principal do Spring.
public class SecurityConfig {

    /**
     * Aqui é onde eu defino de facto as regras de acesso para cada URL da aplicação.
     *
     * Este SecurityFilterChain é basicamente uma "cadeia de filtros" — garanto que cada pedido HTTP
     * que entra na app vai ter de passar por esta cadeia e ser avaliado antes sequer de chegar aos nossos Controllers.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Ativar CORS globalmente
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Trato aqui da proteção CSRF (Cross-Site Request Forgery)
            // Desativei para a consola do H2 e para as rotas de Autenticação (/api/auth/**), 
            // caso contrário o Spring ia bloquear os pedidos POST de login que vêm do React.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/api/auth/**")
            )

            // Configurei o X-Frame-Options aqui porque eu quero permitir que o H2 Console consiga usar os iframes internamente, senão não abre.
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )

            // Aqui defino finalmente as minhas regras de acesso baseadas nos URLs
            .authorizeHttpRequests(auth -> auth
                // Estes são os URLs que eu defini como PÚBLICOS (qualquer um acede sem fazer login)
                .requestMatchers("/h2-console/**").permitAll()     // O meu adorado acesso livre à consola H2
                .requestMatchers("/api/auth/**").permitAll()        // Livre acesso aos endpoints de Login e Registo
                .requestMatchers("/actuator/health").permitAll()    // Livre acesso ao nosso endpoint de Health check para monitorização

                // Para TODOS os restantes URLs que eu não referi em cima, decidi que vão precisar obrigatoriamente de autenticação!
                .anyRequest().authenticated()
            )

            // Por enquanto ativei o formulário de login por defeito que vem com o Spring (aquela página muito básica e feia).
            // Eu prometo que mais tarde vamos remover isto quando o nosso frontend em React se começar a encarregar do ecrã de login!
            .formLogin(form -> form.permitAll())

            // E finalmente, aqui eu configuro como vai funcionar o processo de logout
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
                .permitAll()
            );

        return http.build();
    }

    /**
     * Aqui defino formalmente que vamos usar o BCrypt como o nosso algoritmo de encriptação das passwords.
     *
     * Escolhi o BCrypt porque é atualmente o padrão mais robusto da indústria para guardar passwords de forma segura na base de dados.
     * O que ele vai fazer internamente é algo deste género:
     *   Eu recebo "password123" -> O BCrypt encripta -> Guarda na BD "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     *
     * Como é um hash unidirecional, é literalmente impossível reverter o hash e descobrir a password original em texto limpo.
     * O que eu faço na hora de autenticar é simplesmente mandar o BCrypt comparar o hash que eu tenho guardado com o hash da tentativa de login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuração do CORS para permitir que o Frontend (React/Vite) comunique com o Backend.
     * Como o Frontend corre na porta 5173 e o Backend na 8080, isto é essencial para não ser bloqueado.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permite pedidos vindos da origem do frontend (Vite default port)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        // Permite os métodos HTTP essenciais
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Permite o envio de headers na request
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        // Permite o envio de credenciais (cookies, headers de autenticação)
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica esta configuração de CORS a todos os endpoints do backend
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Exponho o AuthenticationManager nativo do Spring Security como um Bean.
     * Isto é obrigatório porque injetámos este gestor no nosso AuthController para
     * processar programaticamente os logins e definir a sessão manual.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
