# Engenharia reversa e estudo — Helpdesk Suporte

## Objetivo e fluxo

Acompanhamento de chamados, indicadores e exportação. Chamado.java contém as transições de domínio. CentralAtendimento acrescenta metadados, filtros e métricas, e CsvChamados implementa a janela de exportação.

**Tecnologias identificadas** · Java.

**Revisão de referência** · `86c6091bfb93`.

## Mapa do código

| Caminho | Papel de leitura | Símbolos selecionados |
| --- | --- | --- |
| [src/main/java/br/junior/helpdesk/CentralAtendimento.java](../src/main/java/br/junior/helpdesk/CentralAtendimento.java) | Implementação | `CentralAtendimento`, `Metadados`, `Detalhe`, `Filtro`, `Pagina`, `Metricas`, `registrar`, `vencidoEm`, `contemEtiqueta` |
| [src/main/java/br/junior/helpdesk/Chamado.java](../src/main/java/br/junior/helpdesk/Chamado.java) | Implementação | `Chamado`, `Prioridade`, `Status`, `abrir`, `atribuir`, `resolver`, `vencido` |
| [src/main/java/br/junior/helpdesk/CsvChamados.java](../src/main/java/br/junior/helpdesk/CsvChamados.java) | Implementação | `CsvChamados`, `adicionarCampo`, `adicionarLinha`, `exportar`, `linha`, `escapar`, `ler`, `lerRelatorio` |
| [src/main/java/br/junior/helpdesk/HelpdeskService.java](../src/main/java/br/junior/helpdesk/HelpdeskService.java) | Implementação | `HelpdeskService`, `abrir`, `atribuir`, `resolver`, `vencidos`, `listar`, `indexOf` |
| [src/main/java/br/junior/helpdesk/PoliticaSla.java](../src/main/java/br/junior/helpdesk/PoliticaSla.java) | Implementação | `PoliticaSla`, `padrao`, `diasPara`, `diaUtil`, `proximoDiaUtil`, `prazo`, `diasUteisEntre`, `expirado`, `configuracao` |
| [src/main/java/br/junior/helpdesk/RegistroEvento.java](../src/main/java/br/junior/helpdesk/RegistroEvento.java) | Implementação | `RegistroEvento`, `Tipo`, `texto`, `realizadoPor`, `entre` |
| [src/main/java/br/junior/helpdesk/RelatorioHelpdesk.java](../src/main/java/br/junior/helpdesk/RelatorioHelpdesk.java) | Implementação | `RelatorioHelpdesk`, `linha`, `grupos`, `markdown`, `escaparMarkdown` |

## Configuração e referências

Ambiente e dependências: [pom.xml](../pom.xml).

Referências existentes: [docs/GUIA_DE_VALIDACAO.md](GUIA_DE_VALIDACAO.md).

## Roteiro de estudo e desenvolvimento

1. Comece pelo fluxo descrito acima e localize seus pontos de entrada no mapa. Acompanhe uma entrada válida até a transformação ou a resposta.
2. Abra um teste relacionado ao fluxo e descreva qual regra ele protege. Compare o tratamento esperado para entradas válidas, inválidas e nos limites.
3. Ao adicionar uma regra, determine qual módulo controla o dado e cubra o caso nesse nível. Em seguida, confira o efeito no comando, na API ou na interface de entrada.

**Pergunta de entrevista**

Como testaria a diferença entre um chamado vencido e um chamado encerrado no prazo?

## Testes versionados

Arquivos para começar a leitura dos cenários de teste: [src/test/java/br/junior/helpdesk/CentralAtendimentoTest.java](../src/test/java/br/junior/helpdesk/CentralAtendimentoTest.java), [src/test/java/br/junior/helpdesk/CsvChamadosTest.java](../src/test/java/br/junior/helpdesk/CsvChamadosTest.java), [src/test/java/br/junior/helpdesk/HelpdeskServiceTest.java](../src/test/java/br/junior/helpdesk/HelpdeskServiceTest.java), [src/test/java/br/junior/helpdesk/PoliticaSlaTest.java](../src/test/java/br/junior/helpdesk/PoliticaSlaTest.java), [src/test/java/br/junior/helpdesk/RegistroEventoTest.java](../src/test/java/br/junior/helpdesk/RegistroEventoTest.java), [src/test/java/br/junior/helpdesk/RelatorioHelpdeskTest.java](../src/test/java/br/junior/helpdesk/RelatorioHelpdeskTest.java).

Os comandos e versões de runtime usados no CI ficam em [.github/workflows/ci.yml](../.github/workflows/ci.yml).

[Voltar ao README](../README.md).
