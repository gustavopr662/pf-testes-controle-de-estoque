# Regras de negócio (R1 a R4)

Este documento detalha as regras implementadas em `EstoqueService`, `AutenticacaoService` e nos modelos de domínio. Ele é complementar ao `README.md` e à [visão geral](visao-geral.md).

## Modelo e invariantes

| Elemento | Regra |
| --- | --- |
| `sku` | Obrigatório, 1 a 64 caracteres após `trim()`, apenas `[A-Za-z0-9._-]`. |
| `id` do produto | Opcional; quando nulo/vazio, é gerado um `UUID`. |
| `nome` | Obrigatório após `trim()`. |
| `quantidadeAtual` | `>= 0` e `<= estoqueMaximo`. |
| `estoqueMinimo` | `>= 0` e `<= estoqueMaximo`. |
| `estoqueMaximo` | `>= 1`. |
| `ativo` | Controla se o produto aceita saídas. |
| `usuarioId` | Obrigatório, 1 a 64 caracteres após `trim()`, apenas `[A-Za-z0-9._-]`. |

Sku e `usuarioId` também são rejeitados quando contêm conteúdo suspeito: caracteres de controle, `--`, `/*`, `*/`, `;`, `'`, `"`, `<`, `>`, `/`, `\`, `..`, `%`, `?`, `&`, `|` ou `=`. Isso cobre tentativas de injeção SQL-like, path traversal e script injection antes de qualquer consulta ao domínio.

## R1 — Entradas de estoque

Uma entrada incrementa o saldo do produto desde que a quantidade seja válida e o novo saldo não exceda `estoqueMaximo`. O cálculo é feito em `long` para evitar overflow de `int`.

### Entradas válidas

| Situação | Exemplo | Resultado |
| --- | --- | --- |
| Quantidade mínima | saldo `0`, máximo `10`, entrada `1` | saldo `1`, uma movimentação `ENTRADA` |
| Quantidade no limite do máximo | saldo `0`, máximo `10`, entrada `10` | saldo `10` |
| Reposição após alerta | saldo `5`, máximo `20`, entrada `5` | saldo `10`, nenhum alerta novo |

### Entradas inválidas

| Entrada | Exceção esperada | Efeito |
| --- | --- | --- |
| `quantidade <= 0` | `OperacaoEstoqueInvalidaException` | saldo e histórico inalterados |
| saldo + quantidade `> estoqueMaximo` | `OperacaoEstoqueInvalidaException` | saldo e histórico inalterados |
| `sku` nulo, vazio, suspeito ou não cadastrado | `OperacaoEstoqueInvalidaException` | saldo e histórico inalterados |
| usuário sem login, inativo ou sem permissão | `AcessoNegadoEstoqueException` | saldo e histórico inalterados |

### Valores-limite

| Campo | Limite inferior | Limite superior |
| --- | --- | --- |
| `quantidade` da operação | `1` (inclusive) | limitada por `estoqueMaximo - quantidadeAtual` |
| `estoqueMaximo` | `1` (inclusive) | `Integer.MAX_VALUE` |

## R2 — Saídas de estoque

Uma saída decrementa o saldo apenas se o produto estiver ativo, a quantidade for `>= 1` e couber no saldo atual. Após uma saída bem-sucedida o saldo nunca é negativo.

### Entradas válidas

| Situação | Exemplo | Resultado |
| --- | --- | --- |
| Saída abaixo do mínimo | saldo `10`, mínimo `5`, saída `4` | saldo `6`, sem alerta |
| Saída exata no mínimo | saldo `10`, mínimo `5`, saída `5` | saldo `5`, alerta disparado |
| Saída até o saldo zero | saldo `20`, mínimo `5`, saída `20` | saldo `0`, alerta disparado |

### Entradas inválidas

| Entrada | Exceção esperada | Efeito |
| --- | --- | --- |
| `quantidade <= 0` | `OperacaoEstoqueInvalidaException` | saldo e histórico inalterados |
| `quantidade > quantidadeAtual` | `EstoqueInsuficienteException` | saldo e histórico inalterados |
| produto inativo | `OperacaoEstoqueInvalidaException` | saldo e histórico inalterados |
| `sku` nulo, vazio, suspeito ou não cadastrado | `OperacaoEstoqueInvalidaException` | saldo e histórico inalterados |
| usuário sem login, inativo ou sem permissão | `AcessoNegadoEstoqueException` | saldo e histórico inalterados |

### Valores-limite

| Campo | Limite inferior | Limite superior |
| --- | --- | --- |
| `quantidade` da operação | `1` (inclusive) | `quantidadeAtual` (inclusive) |
| `quantidadeAtual` permitida após saída | `0` (inclusive) | — |

## R3 — Alerta de reposição

Após **toda saída bem-sucedida**, se `quantidadeAtual <= estoqueMinimo`, um snapshot do produto é enviado a todos os `AlertaReposicaoListener` registrados.

| Situação | Alerta |
| --- | --- |
| Saída que resulta em saldo `> estoqueMinimo` | Não |
| Saída que resulta em saldo `== estoqueMinimo` | Sim |
| Saída que resulta em saldo `< estoqueMinimo` | Sim |
| Entrada (mesmo com saldo abaixo do mínimo) | Não |
| Operação rejeitada | Não |
| Saídas seguidas enquanto a condição se mantém | Sim, uma notificação por saída |

O snapshot é produzido dentro do lock do SKU e entregue aos listeners fora dele; a notificação recebe uma cópia, sem exponer o objeto mutável do estoque.

## R4 — Segurança, dados inválidos e concorrência

### Estados de autenticação

| Estado do usuário | Comportamento |
| --- | --- |
| Registrado, ativo e com login | Operação permitida; `usuarioId` é gravado na movimentação |
| Sem login (logout) | `AcessoNegadoEstoqueException` |
| Inativo (`desativarUsuario`) | `AcessoNegadoEstoqueException` |
| Permissão revogada | `AcessoNegadoEstoqueException` |
| Não registrado | `AcessoNegadoEstoqueException` no `login` |
| `usuarioId` nulo/vazio/suspeito | `AcessoNegadoEstoqueException` na autenticação e `OperacaoEstoqueInvalidaException` ao registrar em `MovimentacaoEstoque` |

A verificação é feita em duas camadas: `AutenticacaoService.exigirUsuarioAutorizado()` valida sessão, permissão e estado ativo; `EstoqueService` revalida o identificador retornado antes de registrar qualquer movimentação.

### Concorrência

`EstoqueService` usa `ConcurrentHashMap` para produtos e locks, `CopyOnWriteArrayList` para listeners e lista sincronizada para o histórico. Cada SKU possui um `ReentrantLock`: verificação de saldo, atualização e registro da movimentação são atômicos por produto. Operações em SKUs diferentes podem evoluir em paralelo, enquanto saídas simultâneas no mesmo SKU são serializadas — sem saldo negativo e sem perda de movimentações.

## Matriz resumida de exceções

| Exceção | Causa típica |
| --- | --- |
| `OperacaoEstoqueInvalidaException` | quantidade inválida, produto inativo/inexistente, SKU inválido, limite máximo excedido, `usuarioId` inválido em movimentação |
| `EstoqueInsuficienteException` | saída maior que o saldo disponível |
| `AcessoNegadoEstoqueException` | usuário ausente, inativo, sem permissão ou com identificador inválido/suspeito |

## Onde verificar

- Regras R1/R2/R3: [`EstoqueServiceUnitTest`](../src/test/java/com/faculdade/testes/estoque/EstoqueServiceUnitTest.java), [`EstoqueFuncionalTest`](../src/test/java/com/faculdade/testes/estoque/EstoqueFuncionalTest.java) e [`EstoqueParametrizadoTest`](../src/test/java/com/faculdade/testes/estoque/EstoqueParametrizadoTest.java).
- R4 e concorrência: [`EstoqueSegurancaTest`](../src/test/java/com/faculdade/testes/estoque/EstoqueSegurancaTest.java).
- Colaboração real entre componentes: [`EstoqueIntegracaoTest`](../src/test/java/com/faculdade/testes/estoque/EstoqueIntegracaoTest.java).
- Desempenho e carga: [`EstoqueNaoFuncionalTest`](../src/test/java/com/faculdade/testes/estoque/EstoqueNaoFuncionalTest.java) e [testes não funcionais](testes-nao-funcionais.md).
