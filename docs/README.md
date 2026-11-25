# Documentação complementar

Esta pasta reúne material de apoio do projeto de Controle de Estoque.

- `visao-geral.md`: visão geral das regras R1 a R4 e dos cenários de teste implementados.
- `regras-de-negocio.md`: detalhamento de R1 a R4, entradas válidas/inválidas, valores-limite e exceções esperadas.
- `testes-nao-funcionais.md`: cenários de desempenho/carga, concorrência, métricas medidas e critérios de aceitação.
- `teste-regressao.md`: comportamentos protegidos, alterações que causariam regressão e testes que as detectam.
- Os testes automatizados estão em `src/test/java/com/faculdade/testes/estoque/`, com destaque para `EstoqueUnitarioTest` e `EstoqueRegressaoTest`.
- O pipeline de regressão está em `.github/workflows/ci.yml`.
