# Guia de Exploração: Transição de JAX-RS para Spring Boot
**Projeto:** Innovation Lab Management (Projeto AoR - 12ª Edição)  
**Autor:** Grupo 3 (Foco em Backend & Spring Boot)

---

## 1. Introdução: O que é o Spring Boot?

No curso *Acertar o Rumo*, a vossa experiência de backend foi focada em **JAX-RS** (Jakarta RESTful Web Services), que é a especificação padrão do Java EE/Jakarta EE para criar APIs REST. O JAX-RS necessita de um servidor de aplicação (como WildFly ou GlassFish) para rodar.

O **Spring Boot** é um framework construído sobre o ecossistema Spring. A sua principal filosofia é o **"Convention over Configuration"** (Convenção sobre Configuração) e o conceito de **Opinionated Runtime** (Ambiente Opiniático). Ele facilita o arranque e desenvolvimento de aplicações Java autónomas e prontas para produção.

---

## 2. Diferenças Críticas: JAX-RS vs. Spring Boot (Spring Web)

A tabela abaixo resume as diferenças na construção de APIs e injeção de dependências:

| Funcionalidade | JAX-RS (Jakarta EE) | Spring Boot (Spring Web / MVC) | Explicação / Impacto no Projeto |
| :--- | :--- | :--- | :--- |
| **Servidor / Execução** | Necessita de servidor externo (WildFly, Payara). Empacotado como `.war`. | Servidor embutido (Tomcat por padrão). Empacotado como `.jar` executável. | **Muito mais simples:** basta correr a classe principal com `public static void main` e o backend sobe sozinho. |
| **Definição de Controlador** | `@Path("/api/recursos")` | `@RestController` + `@RequestMapping("/api/recursos")` | Em Spring, `@RestController` combina `@Controller` e `@ResponseBody`, indicando que os dados retornados são serializados em JSON/XML. |
| **Mapeamento HTTP** | `@GET`, `@POST`, `@PUT`, `@DELETE` | `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` | O Spring fornece atalhos mais legíveis para os verbos HTTP. |
| **Injeção de Dependências** | `@Inject` (CDI) | `@Autowired` ou Injeção por Construtor (Recomendado) | O Spring possui o seu próprio contentor IoC (Inversion of Control) que gerencia os beans. |
| **Parâmetros de Rota** | `@PathParam("id")` | `@PathVariable("id")` | Para capturar variáveis na URI (ex: `/api/alertas/{id}`). |
| **Parâmetros de Query** | `@QueryParam("tipo")` | `@RequestParam("tipo")` | Para filtros e paginação (ex: `/api/alertas?tipo=cardio`). |
| **Corpo do Pedido** | Sem anotação específica (assumido por padrão) | `@RequestBody` | Necessário para desserializar o JSON recebido no corpo da requisição num objeto Java. |
| **Produção/Consumo de Media**| `@Produces(MediaType.APPLICATION_JSON)` | Tratado automaticamente pelo Spring. Controlado via propriedade `produces` se necessário. | Por padrão, se a biblioteca Jackson estiver no classpath (incluída no Starter Web), o Spring assume JSON. |

### Exemplo Comparativo de Código (Endpoint de Alertas)

#### Em JAX-RS:
```java
@Path("/alertas")
@RequestScoped
public class AlertaResource {
    @Inject
    private AlertaService alertaService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listarAlertas() {
        List<Alerta> alertas = alertaService.getTodos();
        return Response.ok(alertas).build();
    }
}
```

#### Em Spring Boot:
```java
@RestController
@RequestMapping("/api/alertas")
public class AlertaController {
    
    private final AlertaService alertaService;

    // Injeção por construtor (boa prática recomendada)
    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    public ResponseEntity<List<Alerta>> listarAlertas() {
        List<Alerta> alertas = alertaService.getTodos();
        return ResponseEntity.ok(alertas); // Converte automaticamente para JSON
    }
}
```

---

## 3. Arquitetura e Estrutura de Pastas Proposta

Para um projeto limpo e extensível, organizamos a estrutura do projeto seguindo uma **Arquitetura em Camadas** adaptada aos padrões comuns do Spring Boot.

```text
src/
└── main/
    ├── java/
    │   └── com/
    │       └── acertarorumo/
    │           └── innovationlab/
    │               ├── InnovationLabApplication.java (Classe de entrada do Spring Boot)
    │               ├── config/            (Configurações: Segurança, WebSockets, CORS, etc.)
    │               ├── controller/        (Endpoints REST e WebSockets - Camada de Apresentação)
    │               ├── model/             (Entidades JPA: Utilizador, Alerta, Leitura, Regra, etc.)
    │               ├── dto/               (Objetos de Transferência de Dados - Requests/Responses)
    │               ├── repository/        (Interfaces Spring Data JPA - Camada de Persistência)
    │               ├── service/           (Lógica de Negócio: Motor de regras, Processador de Leituras)
    │               │   └── impl/          (Implementações dos serviços)
    │               └── exception/         (Tratamento Global de Exceções)
    └── resources/
        ├── application.properties         (Configurações do Spring: BD H2, portas, flags, etc.)
        ├── db/migration/                  (Scripts de base de dados, se usarem Flyway/Liquibase)
        └── rules/                         (Ficheiros YAML/JSON padrão com as regras do motor)
```

### Papel de Cada Camada no Projeto:
1. **`InnovationLabApplication.java`**: Contém o método `main` e a anotação `@SpringBootApplication`. É o ponto de partida do backend.
2. **`controller/`**: Recebe os pedidos HTTP e WebSocket. Não deve conter lógica de negócio. Apenas valida inputs e delega para os serviços.
3. **`service/`**: Onde reside toda a lógica do **Motor de Regras**, cálculo de limiares, execução da simulação, processamento das leituras em Batch/Stream e geração dos **Relatórios de Avaliação (PDF)**.
4. **`repository/`**: Interfaces que estendem `JpaRepository<Entity, ID>`. O Spring Data JPA implementa automaticamente os métodos CRUD (save, findById, delete, etc.) em tempo de execução sem precisarem de escrever SQL!
5. **`model/`**: Classes Java anotadas com `@Entity` do JPA que representam as tabelas na base de dados (H2/SQLite).

---

## 4. Como Mapear os Requisitos Técnicos no Spring Boot

Aqui explicamos quais as ferramentas e dependências nativas do Spring Boot que usaremos para resolver cada requisito técnico:

### A. Persistência de Dados e Cache (`H2/SQLite` + `In-Memory Cache`)
* **Dependência:** `Spring Data JPA` + `H2 Database` (adicionado como dependency no Maven).
* **Como funciona:** O Spring Boot configura a base de dados H2 (que pode correr em memória ou ficheiro local) automaticamente.
* **Cache em memória (para o Modo Degradado):** Podemos usar a abstração nativa `@EnableCaching` do Spring ou criar uma estrutura `ConcurrentHashMap` personalizada no serviço caso a base de dados falhe, conforme exigido no requisito 6.1 (Modo Degradado).

### B. Comunicação em Tempo Real (`WebSockets`)
* **Dependência:** `Spring WebSocket`.
* **Como funciona:** O Spring Boot permite configurar um broker simples de mensagens em WebSocket (frequentemente usando STOMP). O Frontend (React) subscreve a um canal (ex: `/topic/simulacao`) e o backend publica atualizações de leituras ou alertas em tempo real.

### C. Segurança, Sessão e Autenticação
* **Dependência:** `Spring Security`.
* **Como funciona:** 
  * Permite configurar a expiração da sessão (`server.servlet.session.timeout` no `application.properties`).
  * Fornece utilitários para hashing de passwords seguro (`BCryptPasswordEncoder`).
  * Facilita a proteção de endpoints baseado em perfis (Administrador, Gestor, Utilizador Padrão) usando `@PreAuthorize("hasRole('ADMIN')")`.

### D. Métricas, Health Checks e Logs
* **Dependência:** `Spring Boot Actuator` + `Micrometer`.
* **Actuator:** Expõe endpoints prontos como `/actuator/health` (Health Check exigido no requisito 8.7) e `/actuator/metrics` (Métricas exigidas no requisito 8.8).
* **Logs:** O Spring Boot vem com o Logback pré-configurado. Basta usar a biblioteca SLF4J com a anotação `@Slf4j` (da biblioteca Lombok) ou `LoggerFactory.getLogger()` para escrever logs em ficheiro ou consola (requisito 8.6).

### E. Motor de Regras (miniDSL YAML/JSON)
* O Spring Boot lê ficheiros YAML de forma nativa através da classe `YamlPropertiesFactoryBean` ou mapeando diretamente para objetos Java com Jackson. O vosso motor de regras analisará as leituras fisiológicas (ex: HR, SpO2) ao longo das janelas temporais configuradas nestes ficheiros.

---

## 5. Passos Práticos para Iniciar o Backend

Para criar o esqueleto do projeto backend, o método oficial e mais recomendado é usar o **Spring Initializr** (pode ser feito no browser em [start.spring.io](https://start.spring.io) ou via IDE):

1. **Configurações do Projeto:**
   * **Project:** Maven
   * **Language:** Java (versão 17 ou superior, conforme requisito técnico)
   * **Spring Boot:** 3.x.x (versão estável mais recente)
   * **Group:** `com.acertarorumo`
   * **Artifact:** `innovationlab`
   * **Packaging:** Jar
2. **Dependências Essenciais a Adicionar:**
   * **Spring Web**: Para criar APIs REST e endpoints HTTP.
   * **Spring Data JPA**: Para interagir com a base de dados.
   * **H2 Database**: Base de dados leve em memória/ficheiro recomendada no enunciado.
   * **Spring Security**: Para autenticação, controle de acessos e hashing de passwords.
   * **Lombok**: (Opcional, mas altamente recomendado) Para reduzir o código boilerplate (getters, setters, construtores).
   * **Spring Boot Actuator**: Para os Health Checks e métricas.
   * **WebSocket**: Para notificações em tempo real.
3. **Gerar e Importar:** Clicar em "Generate" para descarregar o `.zip`, extrair na vossa pasta de trabalho e importar na vossa IDE (Eclipse, IntelliJ, VS Code, etc.) como um projeto Maven.
