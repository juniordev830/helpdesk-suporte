# Roteiro de manutenção — Helpdesk Suporte

## O que o código faz

Biblioteca Java 17 de chamados, eventos, SLA e relatórios CSV/Markdown, com persistência em memória.

## Arquivos de referência

`CentralAtendimento.java`, `PoliticaSla.java`, `CsvChamados.java`, `RelatorioHelpdesk.java` e testes de domínio.

## Executar verificações

`mvn -B test`.

Use ambiente local e dados fictícios. O CI complementa, mas não substitui a inspeção do comportamento e das entradas de fronteira.

## Testes que não podem regredir

Verificar cálculo de prazo em fins de semana/feriados, relatórios com caracteres hostis, exportação CSV segura, paginação e eventos em ordem cronológica.

## Próxima melhoria útil

Persistência e restauração de chamados ainda faltam. Não registrar dados pessoais em fixtures de exemplo ou documentos versionados.

### Antes de abrir PR

- [ ] Teste ou exemplo reproduzível para a alteração.
- [ ] README atualizado se o comando ou escopo mudou.
- [ ] Dados privados e segredos fora do controle de versão.
- [ ] Revisão em branch, sem merge automático.
