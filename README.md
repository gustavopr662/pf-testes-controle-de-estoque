# pf-testes-controle-de-estoque

Projeto acadêmico de Testes de Software para modelar e testar regras de controle de estoque com Spring Boot e JUnit 5.

## Integrantes

- Integrante 1: `[Nome completo]` — `[Matrícula]`
- Integrante 2: `[Nome completo]` — `[Matrícula]`

## Regras de negócio

- Todo produto possui `sku`, `nome`, quantidade atual, estoque mínimo, estoque máximo e situação (`ativo`/`inativo`).
- Entrada exige usuário autenticado e autorizado, quantidade maior ou igual a 1 e não pode fazer o estoque ultrapassar `estoqueMaximo`.
- Saída exige usuário autenticado e autorizado, produto ativo, quantidade maior ou igual a 1 e quantidade disponível suficiente.
- Saída maior que `quantidadeAtual` lança `EstoqueInsuficienteException`.
- Produto inativo bloqueia saídas com `OperacaoEstoqueInvalidaException`.
- SKU obrigatório, produto cadastrado, limites coerentes e valores suspeitos são rejeitados com `OperacaoEstoqueInvalidaException`.
- Usuário sem permissão lança `AcessoNegadoEstoqueException`.
- Quando `quantidadeAtual <= estoqueMinimo`, os listeners `AlertaReposicaoListener` são notificados.
- Operações de entrada, saída e consulta são sincronizadas para evitar condições de corrida no estoque em memória.

## Tecnologias

- Java 21
- Spring Boot 3.4.5 / Maven
- JUnit 5
- Mockito (incluído em `spring-boot-starter-test`)
- GitHub Actions

## Como rodar os testes

Com Maven instalado:

```bash
mvn test
```

Sem Maven instalado, use o wrapper incluído no projeto (Windows PowerShell):

```powershell
.\mvnw.cmd test
```

O build também é executado automaticamente no GitHub Actions a cada `push` e `pull_request`.
