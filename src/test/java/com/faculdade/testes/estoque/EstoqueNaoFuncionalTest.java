package com.faculdade.testes.estoque;

import com.faculdade.testes.estoque.exception.EstoqueInsuficienteException;
import com.faculdade.testes.estoque.model.Produto;
import com.faculdade.testes.estoque.service.AutenticacaoService;
import com.faculdade.testes.estoque.service.EstoqueService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class EstoqueNaoFuncionalTest {

	private static final int TOTAL_PRODUTOS = 2000;
	private static final int MOVIMENTACOES_POR_PRODUTO = 2;
	private static final Duration LIMITE_TEMPO = Duration.ofSeconds(10);

	@Test
	void processaMilharesDeMovimentacoesDentroDoLimiteDeTempo() {
		EstoqueService estoque = novoServico();
		AtomicLong tempoNanos = new AtomicLong();

		assertTimeout(LIMITE_TEMPO, () -> {
			long inicio = System.nanoTime();
			for (int indice = 0; indice < TOTAL_PRODUTOS; indice++) {
				String sku = "SKU-LOTE-" + indice;
				estoque.cadastrarProduto(new Produto(null, sku, "Produto " + indice, 0, 1, 50, true));
				estoque.entrada(sku, 10);
				estoque.saida(sku, 5);
			}
			tempoNanos.set(System.nanoTime() - inicio);

			assertEquals(5, estoque.consultarProduto("SKU-LOTE-0").getQuantidadeAtual());
			assertEquals(5, estoque.consultarProduto("SKU-LOTE-1999").getQuantidadeAtual());
			assertEquals(TOTAL_PRODUTOS * MOVIMENTACOES_POR_PRODUTO, estoque.consultarMovimentacoes().size());
		});

		int totalMovimentacoes = TOTAL_PRODUTOS * MOVIMENTACOES_POR_PRODUTO;
		registrarTempoMedio("lote sequencial", tempoNanos.get(), totalMovimentacoes);
	}

	@Test
	void processaLoteConcorrenteDistribuidoEmVariosSkus() throws Exception {
		EstoqueService estoque = novoServico();
		int totalSkus = 200;
		int totalThreads = 8;
		int saldoInicial = 100;
		for (int indice = 0; indice < totalSkus; indice++) {
			estoque.cadastrarProduto(new Produto(null, skuConcorrente(indice), "Produto concorrente " + indice,
					saldoInicial, 10, 200, true));
		}

		ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
		CountDownLatch inicio = new CountDownLatch(1);
		AtomicInteger operacoes = new AtomicInteger();
		AtomicInteger falhas = new AtomicInteger();
		AtomicLong tempoNanos = new AtomicLong();
		List<Future<?>> futures = new ArrayList<>();

		try {
			assertTimeout(LIMITE_TEMPO, () -> {
				long inicioTempo = System.nanoTime();
				for (int thread = 0; thread < totalThreads; thread++) {
					futures.add(executor.submit(() -> {
						inicio.await();
						for (int indice = 0; indice < totalSkus; indice++) {
							try {
								estoque.saida(skuConcorrente(indice), 1);
								operacoes.incrementAndGet();
							} catch (RuntimeException excecao) {
								falhas.incrementAndGet();
							}
						}
						return null;
					}));
				}
				inicio.countDown();
				for (Future<?> future : futures) {
					future.get(10, TimeUnit.SECONDS);
				}
				tempoNanos.set(System.nanoTime() - inicioTempo);
			});

			int totalMovimentacoes = totalThreads * totalSkus;
			assertEquals(0, falhas.get());
			assertEquals(totalMovimentacoes, operacoes.get());
			assertEquals(totalMovimentacoes, estoque.consultarMovimentacoes().size());
			assertEquals(saldoInicial - totalThreads,
					estoque.consultarProduto(skuConcorrente(0)).getQuantidadeAtual());
			assertEquals(saldoInicial - totalThreads,
					estoque.consultarProduto(skuConcorrente(totalSkus - 1)).getQuantidadeAtual());

			registrarTempoMedio("lote concorrente", tempoNanos.get(), totalMovimentacoes);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void operacoesSimultaneasNoMesmoSkuNaoPermitemSaldoNegativo() throws Exception {
		EstoqueService estoque = novoServico();
		String sku = "SKU-NFR-CONCORRENCIA";
		int saldoInicial = 25;
		int totalThreads = 60;
		estoque.cadastrarProduto(new Produto(null, sku, "Produto disputado", saldoInicial, 5, 50, true));

		ExecutorService executor = Executors.newFixedThreadPool(20);
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
						estoque.saida(sku, 1);
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
			assertEquals(0, estoque.consultarProduto(sku).getQuantidadeAtual());
			assertEquals(saldoInicial, estoque.consultarMovimentacoes().size());
		} finally {
			executor.shutdownNow();
		}
	}

	private EstoqueService novoServico() {
		AutenticacaoService autenticacao = new AutenticacaoService();
		autenticacao.registrarUsuario("operador");
		autenticacao.login("operador");
		return new EstoqueService(autenticacao);
	}

	private String skuConcorrente(int indice) {
		return "SKU-LOTE-CONC-" + indice;
	}

	private void registrarTempoMedio(String cenario, long tempoNanos, int totalMovimentacoes) {
		double tempoMs = tempoNanos / 1_000_000.0;
		double medioMs = tempoMs / totalMovimentacoes;
		System.out.printf("[NFR] %s: %d movimentações em %.2f ms (média %.4f ms/movimentação)%n",
				cenario, totalMovimentacoes, tempoMs, medioMs);
	}
}
