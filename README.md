# pf-testes-controle-de-estoque

## Integrantes

- Gustavo Pereira Ramos — Matrícula: 202510130006
- Kevin Alexandre Oliveira Gomes — Matrícula: [A definir]

## Descrição

Sistema de Controle de Estoque que registra entradas e saídas de produtos, controla saldo, limites mínimo e máximo, situação do produto e permissão de usuários. O foco da disciplina de Testes de Software é demonstrar, por meio de testes automatizados, que as regras de negócio funcionam adequadamente.

## Principais Regras de Negócio

R1 — Entradas de estoque devem possuir quantidade maior ou igual a 1 e não podem ultrapassar o limite máximo (estoqueMaximo) definido para o produto.

R2 — Saídas de estoque só podem ser realizadas para produtos ativos, com quantidade maior ou igual a 1 e menor ou igual ao saldo atual em estoque.

R3 — Sempre que uma saída resultar em quantidade atual menor ou igual ao estoque mínimo (quantidadeAtual <= estoqueMinimo), um alerta de reposição deve ser disparado.

R4 — Operações com dados inválidos (valores nulos, negativos, SKU vazio ou caracteres suspeitos) ou realizadas por usuários inativos/sem permissão devem ser rejeitadas.

## Tecnologias Utilizadas

- Java 21
- Maven (`./mvnw`)
- Spring Boot
- JUnit 5
- Mockito
- GitHub Actions

## Como Executar o Projeto

```bash
./mvnw spring-boot:run
```

```powershell
.\mvnw.cmd spring-boot:run
```

```bash
./mvnw clean package
```

## Como Executar os Testes

```bash
./mvnw test
```

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test -Dtest=EstoqueServiceUnitTest
./mvnw test -Dtest=EstoqueFuncionalTest
./mvnw test -Dtest=EstoqueParametrizadoTest
./mvnw test -Dtest=EstoqueNaoFuncionalTest
```

## Principais Técnicas de Teste Utilizadas

- Particionamento em classes de equivalência
- Análise de valor-limite
- Testes unitários com dublês/mocks (Mockito)
- Testes funcionais
- Testes parametrizados
- Testes de integração
- Testes de regressão (CI no GitHub Actions)
- Testes de segurança

## Breve Descrição dos Testes Não Funcionais

Os testes não funcionais validam o desempenho com `assertTimeout` em um lote de movimentações e o comportamento sob concorrência, garantindo que saídas simultâneas não provoquem condição de corrida nem saldo negativo.

## Principais Evidências do Projeto

- Suíte de testes: [`src/test/java/com/faculdade/testes/estoque/`](src/test/java/com/faculdade/testes/estoque/)
- Pipeline de CI/regressão: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)
- Histórico de commits e Pull Requests: [GitHub](https://github.com/gustavopr662/pf-testes-controle-de-estoque)
- Documentação complementar: [`docs/`](docs/)
