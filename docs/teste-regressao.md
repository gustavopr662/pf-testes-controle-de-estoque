# Teste de regressão

Este documento registra alterações de código que causariam regressão nas regras R1 e R3 e os testes que as detectam. Os testes são executados localmente com `.\mvnw.cmd test` e na pipeline `.github/workflows/ci.yml`.

## Comportamento protegido 1 — alerta no limite inferior

- **Comportamento:** uma saída que resulta em `quantidadeAtual == estoqueMinimo` deve disparar exatamente um `AlertaReposicaoListener`, com o snapshot do produto no saldo atingido.
- **Alteração que causaria regressão:** trocar em `EstoqueService.saida` a condição `produto.getQuantidadeAtual() <= produto.getEstoqueMinimo()` por `<`. Com saldo final igual ao mínimo, o alerta deixaria de ser enviado.
- **Teste que detecta:** `EstoqueRegressaoTest.alertaNoLimiteMinimo_deveSerDetectadoSeCondicaoForAlteradaParaMenor`. O cenário parte de 10 unidades, mínimo 5, e retira 5; espera saldo 5 e `quantidadeAlertas() == 1`. Também protegido por `EstoqueUnitarioTest.saidaDeixandoSaldoIgualAoMinimo_deveDispararAlertaReposicao` e pelo caso `10,5,20,5,5,true,null` de `EstoqueParametrizadoTest.valoresLimiteDeSaida`.

## Comportamento protegido 2 — limite máximo de entrada

- **Comportamento:** entrada até `estoqueMaximo` deve ser aceita; entrada acima do limite deve ser rejeitada pela validação de R1 com a mensagem `Entrada excede o estoque máximo do produto ...`, sem registrar movimentação.
- **Alteração que causaria regressão:** remover em `EstoqueService.entrada` a checagem `resultado > produto.getEstoqueMaximo()` ou trocá-la por `>=`. Sem a checagem, a exceção viria apenas da invariante de `Produto`, com outra mensagem; com `>=`, uma entrada exatamente no limite passaria a falhar.
- **Teste que detecta:** `EstoqueRegressaoTest.limiteMaximo_deveSerDetectadoSeValidacaoForRemovidaOuApertada`. O cenário entra 1 unidade em um produto com saldo 9 e máximo 10 (deve chegar a 10), tenta nova entrada (deve falhar com a mensagem específica de R1) e confirma que o saldo e a quantidade de movimentações permanecem 10 e 1. Também protegido por `EstoqueUnitarioTest.entradaAcimaDoEstoqueMaximo_deveLancarExcecao` e `EstoqueParametrizadoTest.entradasAcimaDoMaximoSaoRejeitadas`.

## Evidência

- Comando: `.\mvnw.cmd test`
- Resultado obtido: `Tests run: 85, Failures: 0, Errors: 0, Skipped: 0`
- Classes de regressão: [`EstoqueRegressaoTest`](../src/test/java/com/faculdade/testes/estoque/EstoqueRegressaoTest.java) e [`EstoqueUnitarioTest`](../src/test/java/com/faculdade/testes/estoque/EstoqueUnitarioTest.java).
