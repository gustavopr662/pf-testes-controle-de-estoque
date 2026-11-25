# Testes não funcionais

Este documento descreve os cenários de desempenho, carga e concorrência implementados em [`EstoqueNaoFuncionalTest`](../src/test/java/com/faculdade/testes/estoque/EstoqueNaoFuncionalTest.java), além das métricas obtidas localmente e dos critérios de aceitação.

## Ambiente da medição

- Java 21 (compatibilidade de bytecode; executor local Java 27), Maven via `./mvnw`.
- Máquina local de desenvolvimento, suíte completa executada em 24/11/2026.
- Tempos são informativos; o critério objetivo é o limite de tempo do `assertTimeout`, não uma comparação entre máquinas.

## Cenários

### C1 — Lote sequencial de milhares de movimentações

- 2.000 produtos cadastrados (`SKU-LOTE-0` a `SKU-LOTE-1999`).
- Para cada produto: uma entrada de 10 unidades e uma saída de 5 unidades.
- Total: **4.000 movimentações** além dos cadastros.
- Limite: `assertTimeout(Duration.ofSeconds(10), ...)`.
- Verificações: saldo final `5` no primeiro e no último SKU; histórico com exatamente 4.000 registros.
- Tempo médio por movimentação calculado com `System.nanoTime()` e impresso no console com prefixo `[NFR]`.

### C2 — Lote concorrente distribuído em vários SKUs

- 200 SKUs pré-cadastrados com saldo inicial `100`, mínimo `10` e máximo `200`.
- 8 threads (`CountDownLatch` para largada simultânea), cada uma realizando uma saída de 1 unidade em cada SKU.
- Total: **1.600 movimentações concorrentes**.
- Limite: `assertTimeout(Duration.ofSeconds(10), ...)` e `future.get(10, SECONDS)` por thread.
- Verificações: zero falhas, 1.600 movimentações registradas e saldo final `92` (`100 - 8`) nos SKUs amostrados.

### C3 — Contenção no mesmo SKU

- Um produto com saldo inicial `25` e 60 threads disputando saídas de 1 unidade.
- Verificações: exatamente 25 saídas bem-sucedidas, 35 `EstoqueInsuficienteException`, saldo final `0`, 25 movimentações e nenhuma exceção inesperada.
- O mesmo cenário também é coberto na suíte de segurança, reforçando a ausência de condição de corrida.

## Métricas medidas

| Cenário | Volume | Limite (`assertTimeout`) | Tempo medido | Média por movimentação | Resultado |
| --- | --- | --- | --- | --- | --- |
| C1 — lote sequencial | 4.000 | 10 s | ~94 ms | ~0,024 ms | Passou |
| C2 — lote concorrente | 1.600 | 10 s | ~10 ms | ~0,006 ms | Passou |
| C3 — contenção no mesmo SKU | 60 threads / 25 saídas válidas | 10 s por `Future` | < 1 s | — | Passou |

Os valores acima foram extraídos da execução da suíte completa; a medição individual pode variar com aquecimento da JVM e carga da máquina.

## Critérios de aceitação

1. Nenhum cenário excede o limite de 10 segundos do `assertTimeout`.
2. Nenhuma movimentação é perdida: a quantidade de registros no histórico é exatamente a esperada.
3. Nenhum saldo negativo é produzido, mesmo com 60 threads disputando o mesmo produto.
4. Saídas excedentes falham apenas com `EstoqueInsuficienteException`; erros inesperados são considerados falha.
5. Operações em SKUs diferentes não são serializeadas globalmente: cada SKU tem seu próprio `ReentrantLock`.
6. A suíte completa permanece verde a cada alteração (regressão via `.github/workflows/ci.yml`).

## Como reproduzir

```bash
./mvnw test -Dtest=EstoqueNaoFuncionalTest
```

```powershell
.\mvnw.cmd test -Dtest=EstoqueNaoFuncionalTest
```

Para ver a suíte toda, incluindo os cenários de segurança e integração:

```bash
./mvnw test
```
