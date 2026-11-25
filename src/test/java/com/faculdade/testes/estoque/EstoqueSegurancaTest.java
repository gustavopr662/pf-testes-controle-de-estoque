package com.faculdade.testes.estoque;

import com.faculdade.testes.estoque.exception.AcessoNegadoEstoqueException;
import com.faculdade.testes.estoque.exception.EstoqueInsuficienteException;
import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;
import com.faculdade.testes.estoque.model.MovimentacaoEstoque;
import com.faculdade.testes.estoque.model.Produto;
import com.faculdade.testes.estoque.service.AlertaReposicaoMemoriaListener;
import com.faculdade.testes.estoque.service.AutenticacaoService;
import com.faculdade.testes.estoque.service.EstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstoqueSegurancaTest {

	private static final String SKU = "SKU-SEGURANCA";
	private static final String SKU_CONCORRENCIA = "SKU-SEGURANCA-CONCORRENCIA";
	private static final String USUARIO = "operador.seguranca";

	private AutenticacaoService autenticacao;
	private EstoqueService estoque;
	private AlertaReposicaoMemoriaListener alertas;

	@BeforeEach
	void configurar() {
		autenticacao = new AutenticacaoService();
		autenticacao.registrarUsuario(USUARIO);
		autenticacao.login(USUARIO);
		estoque = new EstoqueService(autenticacao);
		alertas = new AlertaReposicaoMemoriaListener();
		estoque.adicionarListener(alertas);
		estoque.cadastrarProduto(new Produto(null, SKU, "Produto seguro", 20, 5, 50, true));
	}

	@Test
	void usuarioInativoNaoRealizaEntradaNemSaida() {
		autenticacao.desativarUsuario(USUARIO);

		assertThrows(AcessoNegadoEstoqueException.class, () -> estoque.entrada(SKU, 1));
		assertThrows(AcessoNegadoEstoqueException.class, () -> estoque.saida(SKU, 1));

		assertEstadoInalterado();
	}

	@Test
	void usuarioSemPermissaoNaoRealizaOperacao() {
		autenticacao.revogarPermissao(USUARIO);

		assertThrows(AcessoNegadoEstoqueException.class, () -> estoque.entrada(SKU, 1));
		assertThrows(AcessoNegadoEstoqueException.class, () -> estoque.saida(SKU, 1));

		assertEstadoInalterado();
	}

	@Test
	void usuarioSemLoginNaoOperaOEstoque() {
		autenticacao.logout();

		assertThrows(AcessoNegadoEstoqueException.class, () -> estoque.saida(SKU, 1));
		assertThrows(AcessoNegadoEstoqueException.class, () -> estoque.entrada(SKU, 1));

		assertEstadoInalterado();
	}

	@Test
	void loginNegadoParaUsuarioDesconhecidoOuInativo() {
		assertThrows(AcessoNegadoEstoqueException.class, () -> autenticacao.login("desconhecido"));

		autenticacao.desativarUsuario(USUARIO);

		assertThrows(AcessoNegadoEstoqueException.class, () -> autenticacao.login(USUARIO));
		assertNull(autenticacao.usuarioAtivoId());
	}

	@ParameterizedTest(name = "SKU suspeito rejeitado: {0}")
	@MethodSource("skusSuspeitos")
	void skusSuspeitosSaoRejeitadosSemAlterarEstado(String sku) {
		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.entrada(sku, 1));
		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.saida(sku, 1));

		assertEstadoInalterado();
	}

	@ParameterizedTest(name = "usuário suspeito rejeitado: {0}")
	@MethodSource("idsSuspeitos")
	void identificadoresDeUsuarioSuspeitosSaoRejeitadosPelaAutenticacao(String usuarioId) {
		assertThrows(AcessoNegadoEstoqueException.class, () -> autenticacao.registrarUsuario(usuarioId));
		assertThrows(AcessoNegadoEstoqueException.class, () -> autenticacao.concederPermissao(usuarioId));
		assertThrows(AcessoNegadoEstoqueException.class, () -> autenticacao.ativarUsuario(usuarioId));
		assertThrows(AcessoNegadoEstoqueException.class, () -> autenticacao.login(usuarioId));
		assertThrows(AcessoNegadoEstoqueException.class, () -> autenticacao.desativarUsuario(usuarioId));
		assertThrows(AcessoNegadoEstoqueException.class, () -> autenticacao.revogarPermissao(usuarioId));

		assertEquals(USUARIO, autenticacao.usuarioAtivoId());
	}

	@ParameterizedTest(name = "ID de movimentação suspeito rejeitado: {0}")
	@MethodSource("idsSuspeitos")
	void movimentacaoComIdentificadorDeUsuarioSuspeitoEhRejeitada(String usuarioId) {
		assertThrows(OperacaoEstoqueInvalidaException.class,
				() -> new MovimentacaoEstoque(SKU, MovimentacaoEstoque.Tipo.SAIDA, 1, usuarioId));
	}

	@Test
	void concorrenciaEmSaidasDoMesmoProdutoNaoGeraSaldoNegativo() throws Exception {
		int saldoInicial = 10;
		int totalThreads = 50;
		estoque.cadastrarProduto(
				new Produto(null, SKU_CONCORRENCIA, "Produto disputado", saldoInicial, 2, 20, true));

		ExecutorService executor = Executors.newFixedThreadPool(16);
		CountDownLatch inicio = new CountDownLatch(1);
		AtomicInteger sucessos = new AtomicInteger();
		AtomicInteger semEstoque = new AtomicInteger();
		AtomicInteger errosInesperados = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();

		try {
			for (int indice = 0; indice < totalThreads; indice++) {
				futures.add(executor.submit(() -> {
					inicio.await();
					try {
						estoque.saida(SKU_CONCORRENCIA, 1);
						sucessos.incrementAndGet();
					} catch (EstoqueInsuficienteException excecao) {
						semEstoque.incrementAndGet();
					} catch (RuntimeException excecao) {
						errosInesperados.incrementAndGet();
					}
					return null;
				}));
			}
			inicio.countDown();
			for (Future<?> future : futures) {
				future.get(10, TimeUnit.SECONDS);
			}

			assertEquals(0, errosInesperados.get());
			assertEquals(saldoInicial, sucessos.get());
			assertEquals(totalThreads - saldoInicial, semEstoque.get());
			assertEquals(0, estoque.consultarProduto(SKU_CONCORRENCIA).getQuantidadeAtual());
			assertEquals(saldoInicial, estoque.consultarMovimentacoes().size());
		} finally {
			executor.shutdownNow();
		}
	}

	private void assertEstadoInalterado() {
		assertEquals(20, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(0, estoque.consultarMovimentacoes().size());
		assertEquals(0, alertas.quantidadeAlertas());
	}

	private static Stream<Arguments> skusSuspeitos() {
		return Stream.of(
				Arguments.of((Object) null),
				Arguments.of(""),
				Arguments.of("   "),
				Arguments.of("../etc/passwd"),
				Arguments.of("SKU' OR '1'='1"),
				Arguments.of("SKU;DROP TABLE produtos"),
				Arguments.of("<script>alert(1)</script>"),
				Arguments.of("SKU\\..\\secreto"),
				Arguments.of("A".repeat(65)));
	}

	private static Stream<Arguments> idsSuspeitos() {
		return Stream.of(
				Arguments.of((Object) null),
				Arguments.of(""),
				Arguments.of("   "),
				Arguments.of("admin' OR '1'='1"),
				Arguments.of("../../admin"),
				Arguments.of("<script>alert(1)</script>"),
				Arguments.of("user;DROP"),
				Arguments.of("U".repeat(65)));
	}
}
