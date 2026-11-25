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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class EstoqueNaoFuncionalTest {

	@Test
	void processaLoteDeMovimentacoesDentroDoLimiteDeTempo() {
		EstoqueService estoque = novoServico();

		assertTimeout(Duration.ofSeconds(5), () -> {
			for (int indice = 0; indice < 200; indice++) {
				String sku = "SKU-LOTE-" + indice;
				estoque.cadastrarProduto(new Produto(null, sku, "Produto " + indice, 0, 1, 50, true));
				estoque.entrada(sku, 10);
				estoque.saida(sku, 5);
			}
			assertEquals(5, estoque.consultarProduto("SKU-LOTE-0").getQuantidadeAtual());
		});
	}

	@Test
	void evitaCondicaoDeCorridaEmSaidasSimultaneas() throws Exception {
		EstoqueService estoque = novoServico();
		estoque.cadastrarProduto(new Produto(null, "SKU-CONCORRENCIA", "Produto concorrente", 10, 2, 20, true));
		int quantidadeThreads = 20;
		ExecutorService executor = Executors.newFixedThreadPool(quantidadeThreads);
		CountDownLatch inicio = new CountDownLatch(1);
		AtomicInteger sucessos = new AtomicInteger();
		AtomicInteger falhas = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();

		try {
			for (int indice = 0; indice < quantidadeThreads; indice++) {
				futures.add(executor.submit(() -> {
					inicio.await();
					try {
						estoque.saida("SKU-CONCORRENCIA", 1);
						sucessos.incrementAndGet();
					} catch (EstoqueInsuficienteException excecao) {
						falhas.incrementAndGet();
					}
					return null;
				}));
			}
			inicio.countDown();
			for (Future<?> future : futures) {
				future.get(5, TimeUnit.SECONDS);
			}
			assertEquals(10, sucessos.get());
			assertEquals(10, falhas.get());
			assertEquals(0, estoque.consultarProduto("SKU-CONCORRENCIA").getQuantidadeAtual());
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
}
