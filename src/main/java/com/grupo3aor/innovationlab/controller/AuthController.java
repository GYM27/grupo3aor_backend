package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.dto.LoginRequest;
import com.grupo3aor.innovationlab.dto.RegisterRequest;
import com.grupo3aor.innovationlab.repository.UserRepository;
import com.grupo3aor.innovationlab.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Desenvolvi este controlador REST para expor os endpoints de autenticação à aplicação cliente.
 * * O objetivo desta classe é atuar como a porta de entrada para os processos de login e registo,
 * recebendo os dados em formato JSON através dos DTOs e encaminhando-os para a camada de serviço.
 * Configurei o CrossOrigin de forma explícita, ativando a partilha de credenciais para viabilizar
 * o tráfego dos cookies de sessão (JSESSIONID) com o nosso frontend local.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    /**
     * Configurei a injeção das dependências necessárias para as operações de segurança e negócio.
     * Optei por incluir o AuthenticationManager do Spring Security para me permitir delegar
     * o ciclo de autenticação e o registo da sessão HTTP de forma nativa e robusta.
     */
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                          AuthService authService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Implementei este endpoint para processar as tentativas de autenticação do utilizador.
     * * Optei por utilizar o ecossistema nativo do Spring Security, submetendo um token de credenciais
     * ao AuthenticationManager. Desta forma, garanto que, se as credenciais estiverem corretas, 
     * a sessão do utilizador é guardada no contexto do servidor e o cookie JSESSIONID é gerado 
     * e enviado automaticamente para o browser, cumprindo os requisitos de controlo de acessos.
     *
     * @param request O objeto contendo o e-mail e a palavra-passe submetidos.
     * @param httpRequest O objeto de pedido do servlet que utilizei para forçar a criação da sessão.
     * @return Uma resposta HTTP contendo os dados do perfil em caso de sucesso, ou o erro apropriado.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        // Busquei o utilizador pelo e-mail para validar previamente o estado de ativação da conta
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciais inválidas"));
        }

        User user = userOptional.get();

        // Validou-se se a conta já foi ativada antes de prosseguir com o gasto computacional de autenticação
        if (!user.isAtivado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Conta não ativada. Verifique o seu e-mail."));
        }

        try {
            // Criei o token de autenticação em texto limpo com as credenciais submetidas
            UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

            // Delegou-se ao AuthenticationManager a tarefa de validar a password contra o AuthUserLoader
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // Depositei a autenticação validada no contexto de segurança global do Spring Boot
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Forcei explicitamente a criação ou recuperação da sessão HTTP no servidor (Requisito 8.4)
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // Devolvi o perfil e o nome para a construção da interface no cliente
            return ResponseEntity.ok(Map.of(
                    "message", "Login efetuado com sucesso",
                    "perfil", user.getPerfil().name(),
                    "nomeCompleto", user.getNome() + " " + user.getApelido()
            ));

        } catch (Exception e) {
            // Se o gestor de autenticação falhar (password errada), capturo a exceção e respondo com 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciais inválidas"));
        }
    }

    /**
     * Desenvolvi este endpoint para delegar o processo de criação de novas contas.
     * * A lógica de negócio (validação de e-mails duplicados, confirmação de password e encriptação)
     * foi isolada no AuthService para manter este controlador limpo e focado apenas no tráfego HTTP.
     *
     * @param request O objeto contendo todos os dados submetidos no formulário de registo.
     * @return Uma resposta HTTP 200 em caso de sucesso, ou um erro 400 em caso de falha de validação.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.registarNovoUtilizador(request);
            return ResponseEntity.ok(Map.of("message", "Registo efetuado com sucesso."));
        } catch (RuntimeException e) {
            // Capturo as exceções de negócio lançadas pelo AuthService e devolvo-as ao frontend de forma amigável
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
