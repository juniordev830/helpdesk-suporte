# Helpdesk Suporte

Núcleo de gestão de chamados de TI em **Java 17**, com prioridades, prazos, responsáveis, auditoria, indicadores e relatórios. A expansão mantém a API original `HelpdeskService` e `Chamado` e adiciona uma camada operacional independente.

## Funcionalidades

- Abertura, atribuição e resolução de chamados com validação de entradas.
- Prazos de SLA em dias úteis por prioridade, considerando fins de semana e feriados configuráveis.
- Histórico imutável de eventos: abertura, atribuição, resolução, notas, etiquetas e categorias.
- Busca por título e descrição; filtros por status, prioridade, responsável, categoria, etiqueta e vencimento.
- Paginação limitada a 200 itens e ordenação por prioridade, prazo e identificador.
- Indicadores de chamados abertos, em atendimento, resolvidos, vencidos, categorias e responsáveis.
- Exportação CSV com neutralização de fórmulas de planilhas e parser CSV com limites de segurança.
- Relatório Markdown de atendimento com escape de texto não confiável.
- Testes automatizados de fluxos normais, entradas inválidas, limites, imutabilidade e relatórios.

## Requisitos e execução

Instale o JDK 17 ou superior e o Maven 3.9 ou superior.

```bash
mvn -B test
```

O workflow `.github/workflows/ci.yml` executa o mesmo comando nos pushes e nas Pull Requests.

## Exemplo de uso

```java
import br.junior.helpdesk.*;
import java.time.Clock;
import java.time.LocalDate;

var central = new CentralAtendimento(Clock.systemUTC(), PoliticaSla.padrao());
var chamado = central.abrir(
    "VPN indisponível",
    Chamado.Prioridade.ALTA,
    "Erro de conexão para equipe remota",
    "Rede",
    "Solicitante"
);
central.adicionarEtiqueta(chamado.chamado().id(), "vpn", "Analista");
central.atribuir(chamado.chamado().id(), "Ana", "Coordenador");
central.anotar(chamado.chamado().id(), "Ana", "Diagnóstico iniciado");

var pendentes = central.pesquisar(
    CentralAtendimento.Filtro.todos()
        .comStatus(Chamado.Status.EM_ATENDIMENTO),
    0,
    20
);
System.out.println("Total: " + pendentes.total());
System.out.println(central.exportarCsv());
System.out.println(RelatorioHelpdesk.markdown(central, LocalDate.now()));
```

## Política padrão de SLA

| Prioridade | Dias úteis até o vencimento |
|---|---:|
| CRITICA | 0 |
| ALTA | 1 |
| MEDIA | 3 |
| BAIXA | 5 |

O vencimento ocorre no fim da data calculada. Um chamado só é considerado vencido quando a data consultada é posterior ao prazo. Uma abertura em fim de semana é normalizada para o próximo dia útil.

A política é imutável e pode ser personalizada com `comFeriado`, `semFeriado` e `comPrazo`.

## Organização

- `Chamado`: entidade original imutável e regras de ciclo de vida.
- `HelpdeskService`: armazenamento original em memória.
- `PoliticaSla`: calendário de dias úteis e vencimentos.
- `RegistroEvento`: registro de auditoria imutável.
- `CentralAtendimento`: operações, filtros, paginação e métricas.
- `CsvChamados`: exportação segura e leitura validada.
- `RelatorioHelpdesk`: relatório Markdown operacional.
- `src/test/java`: testes JUnit 5 para regras de negócio e limites.

## Limitações e próximos passos

Esta implementação utiliza armazenamento **em memória**, sem banco de dados ou API HTTP. Reiniciar o processo elimina os chamados; o CSV serve como relatório, não como mecanismo de restauração. O parser valida o formato do CSV, mas não reconstrói objetos de domínio automaticamente. Não armazene informações sensíveis nas notas de exemplo.

O objetivo da expansão é adicionar funcionalidades e testes úteis, sem inflar o código artificialmente. As alterações devem ser revisadas por PR, sem merge automático na `main`.

## Prazos próximos no relatório Markdown

O relatório `RelatorioHelpdesk.markdown(central, hoje)` passou a incluir uma seção de **vencimentos nos próximos dois dias corridos**, além da lista de chamados que já estão vencidos. A visualização omite chamados resolvidos, ordena por prazo, escapa os títulos e nomes para impedir injeções de marcação e limita a exibição a 200 linhas, informando quando há mais registros. O cálculo de prazo continua seguindo a política de SLA em dias úteis; a janela de alerta é expressa em **dias corridos**. Novos testes verificam exclusão de chamados vencidos ou resolvidos e a formatação segura.

## Documentação para manutenção

O [guia de validação](docs/GUIA_DE_VALIDACAO.md) contém testes de regressão, pontos críticos e critérios para revisar a próxima alteração.
