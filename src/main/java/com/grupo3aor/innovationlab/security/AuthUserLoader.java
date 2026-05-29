package com.grupo3aor.innovationlab.security;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

/**
 * Criei esta classe com o propósito de carregar os dados do utilizador durante o processo de autenticação.
 * * Optei por implementar a interface UserDetailsService para estabelecer uma ponte direta
 * entre o motor de segurança nativo do Spring Security e a nossa persistência no H2.
 * Configurei este serviço para que o ecossistema do Spring Boot saiba exatamente como localizar
 * um utilizador e validar as suas permissões de acesso baseadas em sessões.
 */
@Service
public class AuthUserLoader implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Configurei a injeção de dependências através do construtor.
     * Optei por esta abordagem por ser a prática mais segura e recomendada pelo ecossistema Spring Boot,
     * garantindo a imutabilidade do componente e facilitando a futura escrita de testes unitários.
     *
     * @param userRepository O repositório utilizado para interagir com a tabela de utilizadores.
     */
    public AuthUserLoader(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Recuperei os dados do utilizador a partir da base de dados através do e-mail introduzido.
     * * Desenvolvi este método para ser invocado automaticamente pelo Spring Security no momento do login.
     * Se o registo for localizado com sucesso, converti os dados do nosso modelo para a classe nativa 
     * do Spring, mapeando os perfis de acesso diretamente no contexto de segurança da aplicação.
     *
     * @param email O endereço de e-mail fornecido no ecrã de login do frontend.
     * @return Um objeto UserDetails preenchido com as credenciais cifradas e permissões do utilizador.
     * @throws UsernameNotFoundException Lançada se o e-mail não existir na base de dados, interrompendo o login.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        // Decidi intersetar a busca e lançar uma exceção explícita caso o e-mail não exista na tabela
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado com o e-mail: " + email));

        // Mapeei a permissão do utilizador convertendo o nosso enum PerfilEnum numa GrantedAuthority.
        // Adiciono o prefixo "ROLE_" para respeitar o padrão de nomenclatura exigido pelo Spring Security.
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getPerfil().name());

        // Retornei a instância de utilizador nativa do Spring Security através do Builder.
        // Configurei o estado 'disabled' com base no nosso campo 'ativado', garantindo que 
        // utilizadores que não validaram a conta sejam automaticamente barrados pelo motor do Spring.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(authority))
                .disabled(!user.isAtivado()) // Bloqueia o login se ativado == false!
                .build();
    }
}
