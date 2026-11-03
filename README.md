# Helpdesk Suporte

Chamados de TI com prioridade, responsável e prazo de atendimento.

## Estado atual

MVP executável em branch de desenvolvimento, com testes automatizados. Chamados em memória com atribuição, status e verificação de prazo. Ainda não há API, interface ou persistência.

## Tecnologias implementadas

Java 17 e Maven.

## Escopo da primeira versão

- [ ] Abrir chamados
- [ ] Atribuir responsável e prioridade
- [ ] Acompanhar prazos e histórico

## Critérios para concluir a primeira versão

- [ ] Implementar o fluxo principal com validação de entradas e tratamento de erros.
- [ ] Documentar instalação, configuração e execução com comandos reproduzíveis.
- [ ] Adicionar exemplos de uso e dados fictícios.
- [ ] Verificar os casos principais e limites e registrar os resultados.

## Desenvolvimento

Execute na raiz do repositório:

```bash
mvn -B test
```

O GitHub Actions executa a validação em pushes e pull requests. Este é um núcleo demonstrativo; as funcionalidades ainda marcadas como pendentes na lista acima continuam planejadas.

## Privacidade e configuração

Usar dados fictícios nos exemplos e manter senhas, tokens, arquivos pessoais e credenciais fora do controle de versão.

## Licença

Ainda não definida.
