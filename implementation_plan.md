# Análise de Vulnerabilidades e Plano de Segurança

Após analisar as críticas de segurança e robustez apresentadas, confirmo que **fazem total sentido** e abordam lacunas críticas de segurança e qualidade de código exigidas em ambientes de produção. 

Abaixo encontra-se a justificação técnica para cada ponto e a nossa estratégia de resolução.

## Análise Crítica

1. **Falha Crítica de Segurança: CSRF vs. Sessões**
   - *Porquê:* O nosso frontend React está configurado para enviar a sessão ativamente via Cookies (`credentials: 'include'`). Ao ter o CSRF desativado no `SecurityConfig`, um site atacante malicioso (onde a vítima estivesse a navegar noutro separador) poderia forçar o browser a enviar um pedido de eliminação de paciente em nosso nome.
   - *Impacto:* Elevado.
2. **Risco de Robustez (OOM/DoS): Parse Direto de Ficheiros**
   - *Porquê:* O servidor Spring Boot está cego quanto ao tamanho dos ficheiros `MultipartFile`. Um JSON malicioso ou de tamanho absurdo (ex: 2GB) provocaria o colapso do Jackson e da memória (Out of Memory) antes de qualquer validação de schema.
   - *Impacto:* Médio/Elevado.
3. **Silenciamento de Exceções em Produção**
   - *Porquê:* Ao intercetar o genérico `Exception.class` no `GlobalExceptionHandler` e mascará-lo ao utilizador sem invocar `log.error()`, estamos a esconder a *Stack Trace* de nós próprios. Num ambiente real, ficaríamos às escuras a tentar perceber a causa dos erros HTTP 500.
   - *Impacto:* Médio (dificulta a manutenção).
4. **Exposição da Consola H2**
   - *Porquê:* A H2 é útil para desenvolvimento, mas expô-la publicamente com `permitAll()` no ficheiro principal de segurança é um vetor de ataque clássico em ambientes expostos.
   - *Impacto:* Baixo em Dev / Crítico em Prod.
5. **Boas Práticas Criptográficas (Tokens)**
   - *Porquê:* O `UUID.randomUUID()` tem uma entropia limitada face ao poderio algorítmico de computação atual para dedução temporal. É boa prática usar criptografia forte baseada em `SecureRandom`.

---

## Proposta de Resolução (Plano de Ação)

Vou intervir nestes 5 pontos estratégicos. A execução não é invasiva e aumenta exponencialmente a robustez do software.

### Segurança Global (CSRF & H2 Console)
- **Ativar CSRF Dinâmico para SPAs:**
  Modificarei o `SecurityConfig.java` para utilizar um `CookieCsrfTokenRepository` com `withHttpOnlyFalse()`. Isto dirá ao Spring Boot para enviar de forma segura um token XSRF nos cookies que o frontend (React) saberá extrair e devolver automaticamente em todos os pedidos POST/PUT/DELETE.
- **Isolar a Consola H2:**
  Vou assegurar que as regras de segurança relaxadas que ativam a consola H2, incluindo a permissão do "Frame Options", estarão devidamente condicionadas (e idealmente recomendarei que no futuro se utilize um mecanismo associado a `@Profile("dev")`).

### Robustez do Controlador (Upload de Ficheiros JSON)
- **Bloqueios a Nível de Camada HTTP:**
  Inserirei limites máximos absolutos de upload no ficheiro `application.yml` (ou propriedades de configuração) bloqueando ameaças gigantes antes que a *stream* atinja o nível de código do Jackson. 
  Exemplo: `spring.servlet.multipart.max-file-size=5MB` e `max-request-size=10MB`.
- **Validação Primitiva MIME:**
  Em `ClinicalScenarioController.java`, adicionarei uma barreira rápida para garantir que ficheiros não-JSON são recusados preventivamente.

### Visibilidade Operacional
- **Logging Transparente:**
  Abrirei o `GlobalExceptionHandler.java` para injetar o Logger do SLF4J, garantindo que as catástrofes silenciadas (Exceptions não mapeadas) são devidamente reportadas nos logs da consola (ou ficheiro).

### Criptografia de Tokens
- **Atualização do Gerador:**
  Irei atualizar a secção de código responsável por criar e atribuir Tokens, mudando-a de `UUID` para uma cadeia segura de 32 bytes encriptada através do `java.security.SecureRandom` com formatação e conversão `Base64`.

## User Review Required

> [!IMPORTANT]
> Faz sentido prosseguir com a blindagem total da plataforma conforme sugerido?
> Ao aceitar as mudanças, a única possível implicação direta para si será que o Frontend (que por acaso até já está configurado por nós para ler e enviar o Token CSRF se ele for enviado) ficará imediatamente muito mais seguro! Confirme a aprovação para aplicar e testar o código.


---

## 🚀 Estado de Execução (Atualizado a 22-06-2026)

> [!NOTE]
> O utilizador aceitou e orientou a implementação do plano acima com algumas exceções estratégicas. Abaixo encontra-se o balanço da execução.

✅ **Concluído:**
1. **CSRF Ativado**: A vulnerabilidade de forja de pedidos na SPA React foi bloqueada. A linha `CookieCsrfTokenRepository.withHttpOnlyFalse()` foi injetada em `SecurityConfig.java`, passando a enviar os headers `X-XSRF-TOKEN` exigidos.
2. **Prevenção DoS / Ficheiros**: Foi implementado o escudo a dois níveis. Ficheiros JSON ficaram restritos a 5MB com validação MIME estrita (`application/json`) no `ClinicalScenarioController.java`. Simulações pesadas (BioGears) mantêm teto alocado de 500MB em `application.properties`.
3. **Visibilidade Operacional (Stack Traces)**: O `@Slf4j` e os devidos alertas `log.error("[FATAL ERROR]...")` foram ativados no `GlobalExceptionHandler.java` para capturar todos os erros HTTP 500 mudos.

🚫 **Cancelado/Adiado:**
1. **Criptografia de Tokens (UUID -> SecureRandom)**: A pedido expresso do utilizador, manteve-se a arquitetura de UUIDv4 no `AuthService.java`.
2. **Isolamento de Consola H2**: Deixado em repouso por não ter sido exigido e de modo a não interferir com os acessos visuais da base de dados de desenvolvimento.

*(Os relatórios de Avaliação PDF sofreram também, em paralelo, resoluções críticas na fórmula de contagem temporal baseada no frontend).*
