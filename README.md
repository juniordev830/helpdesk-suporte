# Helpdesk Suporte

Núcleo de atendimento em **Java 17 e Maven**, com abertura de chamados, atribuição, resolução, controle de prazos, fila de triagem e painel de indicadores em memória.

## Funcionalidades implementadas

- Abrir chamados com prioridade `BAIXA`, `MEDIA`, `ALTA` ou `CRITICA` e prazo de atendimento.
- Atribuir responsável antes de resolver.
- Consultar chamados vencidos sem incluir os resolvidos.
- `filaAtendimento(hoje)`: lista imutável de chamados pendentes, primeiro os vencidos, depois por prioridade (crítica antes de alta, média e baixa), prazo crescente e ID.
- `painel(hoje)`: contagem de chamados abertos, em atendimento, resolvidos e vencidos.

## Exemplo de uso

```java
var helpdesk = new HelpdeskService();
var hoje = LocalDate.of(2026, 11, 3);
var urgente = helpdesk.abrir("Serviço indisponível", Chamado.Prioridade.CRITICA, hoje);
helpdesk.abrir("Trocar mouse", Chamado.Prioridade.BAIXA, hoje.plusDays(7));

System.out.println(helpdesk.filaAtendimento(hoje).get(0).id()); // ID do chamado urgente
System.out.println(helpdesk.painel(hoje)); // Totais por status e vencimento
helpdesk.atribuir(urgente.id(), "Analista");
helpdesk.resolver(urgente.id());
```

Imports: `br.junior.helpdesk.*` e `java.time.LocalDate`.

## Executar os testes

```bash
mvn -B test
```

O GitHub Actions valida o fluxo básico, a ordenação da fila, a exclusão de chamados resolvidos, a imutabilidade do resultado e os contadores do painel.

## Limitações

Biblioteca de domínio de demonstração: sem API, autenticação, persistência, histórico completo, envio de alertas ou sincronização entre usuários. Os dados desaparecem ao reiniciar. Os métodos públicos não oferecem garantia de concorrência entre múltiplas threads. O painel é um resumo da memória atual, não um monitoramento de SLA com métricas históricas.

Use dados fictícios nos exemplos. Não versionar credenciais ou informações pessoais.

## Licença

Ainda não definida.
