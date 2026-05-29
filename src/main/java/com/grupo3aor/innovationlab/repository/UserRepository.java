package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Criei esta interface para gerir o acesso aos dados e a persistência da entidade User.
 * Optei por herdar de JpaRepository para delegar ao Spring Data JPA a geração automática
 * de todas as operações fundamentais de CRUD no banco de dados H2.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Configurei esta consulta baseada em convenção de nomenclatura para localizar
     * um utilizador através do seu endereço de e-mail.
     * * Escolhi envolver o retorno num objeto 'Optional' porque compreendi que o e-mail
     * introduzido pode não existir na tabela, permitindo-me tratar a ausência do registo
     * de forma segura na camada de serviço e evitar exceções nulas involuntárias.
     *
     * @param email Endereço de e-mail utilizado na pesquisa.
     * @return Um Optional contendo o utilizador, caso ele seja localizado.
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Decidi adicionar este método de validação rápida para verificar a existência
     * prévia de um e-mail durante o fluxo de registo.
     * * Optei por esta abordagem porque ela executa uma verificação otimizada no banco de dados,
     * retornando um valor booleano simples. Isto evita que eu tenha de carregar a entidade
     * completa para a memória do servidor apenas para saber se o e-mail já está ocupado.
     *
     * @param email Endereço de e-mail a ser verificado.
     * @return Verdadeiro se o e-mail já existir na tabela, falso caso contrário.
     */
    boolean existsByEmail(String email);
}