package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import com.grupo3aor.innovationlab.dto.RegisterRequest;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por encapsular a lógica de negócio associada à autenticação.
 * 
 * Centraliza as regras de registo (como a validação de e-mails duplicados e 
 * confirmação de senhas) e comunica com o repositório para persistir os dados.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Construtor para injeção de dependências.
     * Injetamos a ferramenta de encriptação (PasswordEncoder) e a nossa ligação
     * à base de dados (UserRepository).
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Processa o pedido de registo de um novo utilizador.
     * 
     * Executa as seguintes validações:
     * 1. Verifica se as passwords coincidem.
     * 2. Garante (através de uma query otimizada) que o e-mail não está em uso.
     * 
     * Converte o DTO (RegisterRequest) numa Entidade (User), encripta a password 
     * e guarda o registo.
     *
     * @param request O objeto vindo do frontend com os dados do registo (firstName, email, etc.)
     * @throws RuntimeException se alguma das regras de negócio for violada.
     */
    public void registarNovoUtilizador(RegisterRequest request) {
        
        // 1. As passwords batem certo? (Validação inicial de segurança)
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("As passwords não coincidem. Por favor, tente novamente.");
        }

        // 2. O e-mail já existe? Utilizamos aqui a pesquisa otimizada que configurou no UserRepository!
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Este e-mail já se encontra registado no sistema.");
        }

        // 3. Traduzir o "Mensageiro" (DTO) para a "Realidade" (Base de Dados)
        User novoUser = User.builder()
                .nome(request.getFirstName())
                .apelido(request.getLastName())
                .email(request.getEmail())
                // A regra de ouro da cibersegurança: NUNCA guardar passwords em texto limpo!
                .passwordHash(passwordEncoder.encode(request.getPassword())) 
                // Por defeito, os novos utilizadores têm apenas as permissões base.
                .perfil(PerfilEnum.UTILIZADOR) 
                // TODO: Conforme acordado, forçamos 'ativado = true' temporariamente para podermos testar o login de imediato.
                // Quando o envio de e-mails de ativação for implementado, remover esta linha!
                .ativado(true) 
                .build();

        // 4. Gravar permanentemente no H2 Database
        userRepository.save(novoUser);
    }
}
