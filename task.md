# Lista de Tarefas: Implementação de Segurança e Robustez

- `[x]` 1. **Ativação CSRF**: Modificado `SecurityConfig.java` para injetar `CookieCsrfTokenRepository` com `withHttpOnlyFalse()`.
- `[x]` 2. **Prevenção DoS em Uploads**:
  - `[x]` Limite no `application.properties` para BioGears (`500MB`).
  - `[x]` Barreira MIME e limite de 5MB via código no `ClinicalScenarioController.java`.
- `[x]` 3. **Visibilidade Operacional**: Adicionado `@Slf4j` e `log.error` na exceção genérica em `GlobalExceptionHandler.java`.
- `[~]` 4. **Criptografia Forte**: *CANCELADO A PEDIDO DO UTILIZADOR* (Mantido o UUID no `AuthService.java`).
- `[ ]` 5. **Restringir H2 Console**: Proteger a H2 Console usando *Profiles* (ex: `@Profile("dev")`) para evitar exposição global.
