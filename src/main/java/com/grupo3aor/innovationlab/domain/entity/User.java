package com.grupo3aor.innovationlab.domain.entity;

import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Criei esta Entidade JPA para representar a tabela "users" na base de dados.
 *
 * NOTAS SOBRE A IMPLEMENTAÇÃO:
 * - As anotações @Entity, @Table, @Id e @Column são as anotações padrão da especificação JPA.
 * - A grande novidade que decidi introduzir aqui são as anotações do Lombok (@Data, @Builder).
 *   Isto é brutal porque elimina a necessidade de andarmos a escrever getters, setters e construtores à mão, o código fica muito mais limpo!
 */
@Entity
@Table(name = "users") // Defini o nome da tabela como "users" e não "user",
                        // porque "user" costuma ser uma palavra reservada no SQL e pode dar problemas.
@Getter             // Substituí o @Data pelas anotações granulares @Getter, @Setter e @ToString.
@Setter             // Optei por esta abordagem preventiva para garantir que, quando adicionarmos relações JPA
@ToString           // (como @OneToMany) no futuro, não teremos problemas de StackOverflow ou falhas de performance.
@NoArgsConstructor  // O Lombok cria o construtor vazio por nós — que o JPA obriga a existir!
@AllArgsConstructor // Criei o construtor com todos os campos porque o @Builder precisa dele para funcionar.
@Builder            // Coloquei o Lombok Builder: isto permite-me criar um User de forma muito fixe, tipo: User.builder().nome("Ana").build()
public class User {

    // =========================================================
    // A MINHA CHAVE PRIMÁRIA
    // Usei IDENTITY para o auto-increment. É exatamente o equivalente a quando usávamos BIGSERIAL no PostgreSQL.
    // =========================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // OS NOSSOS DADOS PESSOAIS
    // O @NotBlank garante que os campos obrigatórios (nome, apelido, email) não vêm nulos nem vazios.
    // O @Size adiciona uma restrição de segurança ao tamanho máximo na base de dados.
    // =========================================================
    @NotBlank(message = "O nome não pode estar vazio")
    @Size(max = 75, message = "O nome não pode ter mais de 75 caracteres")
    @Column(nullable = false, length = 75)
    private String nome;

    @NotBlank(message = "O apelido não pode estar vazio")
    @Size(max = 75, message = "O apelido não pode ter mais de 75 caracteres")
    @Column(nullable = false, length = 75)
    private String apelido;

    @Email(message = "O email nao tem formato valido")
    @NotBlank(message = "O email nao pode estar vazio")
    @Column(nullable = false, unique = true) // Pus o unique a true porque não quero deixar que existam dois utilizadores registados com o mesmo e-mail.
    private String email;

    // ATENÇÃO MUITO IMPORTANTE: Eu decidi guardar apenas o HASH da password, NUNCA a password em texto simples na base de dados!
    // Mais à frente, quando configurar o BCrypt (no Passo 9), ele vai pegar no "password123" e converter em algo como "$2a$10$xyz..."
    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    // =========================================================
    // O NOSSO PERFIL (vou usar o enum que criei lá no Passo 3)
    // Optei por EnumType.STRING para que na BD grave coisas como "ADMIN" ou "GESTOR".
    // Fiz isto porque fica muito mais fácil de ler na base de dados do que se guardasse índices numéricos soltos (0, 1, 2).
    // =========================================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilEnum perfil;

    // =========================================================
    // O MEU SOFT DELETE
    // Eu pensei: em vez de apagarmos definitivamente um registo da base de dados, vamos antes desativar o utilizador.
    // Assim nunca perdemos o histórico de dados no nosso sistema!
    // =========================================================
    @Column(nullable = false)
    @Builder.Default // Tive de meter isto para que o Lombok @Builder respeite o meu valor por defeito.
    private boolean ativo = true; // Por defeito, sempre que crio um utilizador novo, ele começa logo como ativo.

    // =========================================================
    // A MINHA LÓGICA DE ATIVAÇÃO DE CONTA POR EMAIL
    // Eu decidi que o utilizador só consegue fazer login depois de ir ao seu e-mail e ativar a conta.
    // =========================================================
    @Builder.Default
    private boolean ativado = false; // Ao contrário do "ativo", os novos utilizadores começam NÃO ativados (têm de confirmar o email).

    private String activationToken; // Aqui guardo o token que eu vou gerar no momento do registo e enviar no link por e-mail.

    // =========================================================
    // A MINHA AUDITORIA AUTOMÁTICA
    // Usei estas anotações espetaculares para que o Hibernate preencha estes campos sozinho, não preciso de escrever código extra para isto!
    // =========================================================
    @CreationTimestamp // O Hibernate preenche isto automaticamente no exato momento em que o registo é CRIADO.
    @Column(nullable = false, updatable = false) // Coloquei updatable=false para ter a certeza que esta data nunca mais muda depois da criação!
    private LocalDateTime criadoEm;

    @UpdateTimestamp // O Hibernate atualiza isto automaticamente sempre que o registo for ALTERADO.
    private LocalDateTime atualizadoEm;

    // =========================================================
    // IDENTIDADE DA ENTIDADE (EQUALS & HASHCODE)
    // Implementei o equals() e o hashCode() focados exclusivamente no 'id' para garantir 
    // estabilidade nas coleções (ex: Set) e evitar referências circulares perigosas em relacionamentos.
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        // Dois utilizadores são iguais estritamente se partilharem o mesmo ID, e esse ID já existir.
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        // Utilizo um valor fixo baseado na classe para garantir que o hashCode não se altera 
        // entre o momento em que a entidade é instanciada e o momento em que ganha um ID na base de dados.
        return getClass().hashCode();
    }
}
