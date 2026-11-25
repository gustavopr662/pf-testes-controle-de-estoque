package com.faculdade.testes.estoque;

import com.faculdade.testes.estoque.exception.EstoqueInsuficienteException;
import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;
import com.faculdade.testes.estoque.model.Produto;
import com.faculdade.testes.estoque.service.AlertaReposicaoMemoriaListener;
import com.faculdade.testes.estoque.service.AutenticacaoService;
import com.faculdade.testes.estoque.service.EstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstoqueParametrizadoTest {

	private static final String SKU = "SKU-LIMITE";
	private static final String USUARIO = "operador";

	private AlertaReposicaoMemoriaListener alertas;

	@BeforeEach
	void configurar() {
		alertas = new AlertaReposicaoMemoriaListener();
	}

	@ParameterizedTest(name = "entrada válida: quantidade={0}, saldo final={1}")
	@CsvSource({"1,1", "5,5", "9,9", "10,10"})
	void entradasValidasNosLimites(int quantidade, int saldoEsperado) {
		EstoqueService estoque = novoServico(0, 0, 10);

		estoque.entrada(SKU, quantidade);

		assertEquals(saldoEsperado, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(0, alertas.quantidadeAlertas());
	}

	@ParameterizedTest(name = "entrada inválida: quantidade={0}")
	@CsvSource({"0,10", "-1,10", "11,10"})
	void entradasInvalidasSaoRejeitadas(int quantidade, int estoqueMaximo) {
		EstoqueService estoque = novoServico(0, 0, estoqueMaximo);

		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.entrada(SKU, quantidade));
		assertEquals(0, estoque.consultarProduto(SKU).getQuantidadeAtual());
	}

	@ParameterizedTest(name = "SKU inválido: {0}")
	@MethodSource("skusInvalidos")
	void entradasComSkuInvalidoSaoRejeitadas(String sku) {
		EstoqueService estoque = novoServico(0, 0, 10);

		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.entrada(sku, 1));
	}

	@ParameterizedTest(name = "saída quantidade={3}, saldo inicial={0}, mínimo={1}, máximo={2}")
	@MethodSource("valoresLimiteSaida")
	void valoresLimiteDeSaida(int saldoInicial, int estoqueMinimo, int estoqueMaximo, int quantidade,
			int saldoFinalEsperado, boolean alertaEsperado, Class<? extends Throwable> excecaoEsperada) {
		EstoqueService estoque = novoServico(saldoInicial, estoqueMinimo, estoqueMaximo);

		if (excecaoEsperada != null) {
			assertThrows(excecaoEsperada, () -> estoque.saida(SKU, quantidade));
			assertEquals(saldoInicial, estoque.consultarProduto(SKU).getQuantidadeAtual());
			assertEquals(0, alertas.quantidadeAlertas());
			return;
		}

		estoque.saida(SKU, quantidade);
		assertEquals(saldoFinalEsperado, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(alertaEsperado ? 1 : 0, alertas.quantidadeAlertas());
	}

	private EstoqueService novoServico(int quantidadeAtual, int estoqueMinimo, int estoqueMaximo) {
		AutenticacaoService autenticacao = new AutenticacaoService();
		autenticacao.registrarUsuario(USUARIO);
		autenticacao.login(USUARIO);
		EstoqueService estoque = new EstoqueService(autenticacao);
		estoque.adicionarListener(alertas);
		estoque.cadastrarProduto(new Produto(null, SKU, "Produto limite", quantidadeAtual, estoqueMinimo,
				estoqueMaximo, true));
		return estoque;
	}

	private static Stream<Arguments> skusInvalidos() {
		return Stream.of(
				Arguments.of((Object) null),
				Arguments.of(""),
				Arguments.of("   "),
				Arguments.of("SKU/001"),
				Arguments.of("SKU;DROP"),
				Arguments.of("<script>"));
	}

	private static Stream<Arguments> valoresLimiteSaida() {
		return Stream.of(
				Arguments.of(10, 5, 20, 0, 10, false, OperacaoEstoqueInvalidaException.class),
				Arguments.of(10, 5, 20, -3, 10, false, OperacaoEstoqueInvalidaException.class),
				Arguments.of(10, 5, 20, 1, 9, false, (Object) null),
				Arguments.of(10, 5, 20, 4, 6, false, (Object) null),
				Arguments.of(10, 5, 20, 5, 5, true, (Object) null),
				Arguments.of(10, 5, 20, 6, 4, true, (Object) null),
				Arguments.of(20, 5, 20, 19, 1, true, (Object) null),
				Arguments.of(20, 5, 20, 20, 0, true, (Object) null),
				Arguments.of(20, 5, 20, 21, 20, false, EstoqueInsuficienteException.class));
	}
}
