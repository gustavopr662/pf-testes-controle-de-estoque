# Visão geral

O sistema controla entradas e saídas de produtos com validação de saldo, limite máximo, situação do produto, permissão de usuário e alerta de reposição.

## Regras

- R1: entrada com quantidade maior ou igual a 1 e sem ultrapassar `estoqueMaximo`.
- R2: saída apenas para produto ativo, com quantidade maior ou igual a 1 e menor ou igual ao saldo atual.
- R3: saída que resulta em `quantidadeAtual <= estoqueMinimo` dispara `AlertaReposicaoListener`.
- R4: dados inválidos ou usuários inativos/sem permissão são rejeitados.

## Cenários de teste

- `EstoqueServiceUnitTest`: testes unitários com mock do listener de alerta.
- `EstoqueFuncionalTest`: fluxo completo de entradas, saídas, reposição e bloqueios com listener em memória.
- `EstoqueParametrizadoTest`: valores-limite e massas de dados inválidas com `@CsvSource` e `@MethodSource`.
- `EstoqueNaoFuncionalTest`: tempo de resposta em lote com `assertTimeout`, lote concorrente distribuído e contenção no mesmo SKU.
- `EstoqueIntegracaoTest`: colaboração real entre autenticação, estoque e listener em memória, sem mocks.
- `EstoqueSegurancaTest`: acesso negado, payloads suspeitos e concorrência em saídas.

O detalhamento das regras está em [`regras-de-negocio.md`](regras-de-negocio.md) e as métricas não funcionais em [`testes-nao-funcionais.md`](testes-nao-funcionais.md).
