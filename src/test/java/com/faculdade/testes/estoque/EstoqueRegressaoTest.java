package com.faculdade.testes.estoque;

import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;
import com.faculdade.testes.estoque.model.Produto;
import com.faculdade.testes.estoque.service.AlertaReposicaoMemoriaListener;
import com.faculdade.testes.estoque.service.AutenticacaoService;
import com.faculdade.testes.estoque.service.EstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstoqueRegressaoTest {

	private static final String SKU_ALERTA = "SKU-REGRESSAO-ALERTA";
	private static final String SKU_MAXIMO = "SKU-REGRESSAO-MAXIMO";
	private static final String USUARIO = "operador.regressao";

	private EstoqueService estoque;
	private AlertaReposicaoMemoriaListener alertas;

	@BeforeEach
	void configurar() {
		AutenticacaoService autenticacao = new AutenticacaoService();
		autenticacao.registrarUsuario(USUARIO);
		autenticacao.login(USUARIO);
		estoque = new EstoqueService(autenticacao);
		alertas = new AlertaReposicaoMemoriaListener();
		estoque.adicionarListener(alertas);
	}

	@Test
	void alertaNoLimiteMinimo_deveSerDetectadoSeCondicaoForAlteradaParaMenor() {
		estoque.cadastrarProduto(produto(SKU_ALERTA, 10, 5, 20));

		estoque.saida(SKU_ALERTA, 5);

		assertEquals(5, estoque.consultarProduto(SKU_ALERTA).getQuantidadeAtual());
		assertEquals(1, alertas.quantidadeAlertas());
		assertEquals(5, alertas.alertas().get(0).getQuantidadeAtual());
	}

	@Test
	void limiteMaximo_deveSerDetectadoSeValidacaoForRemovidaOuApertada() {
		estoque.cadastrarProduto(produto(SKU_MAXIMO, 9, 0, 10));

		estoque.entrada(SKU_MAXIMO, 1);
		assertEquals(10, estoque.consultarProduto(SKU_MAXIMO).getQuantidadeAtual());
		assertEquals(1, estoque.consultarMovimentacoes().size());

		OperacaoEstoqueInvalidaException excecao = assertThrows(OperacaoEstoqueInvalidaException.class,
				() -> estoque.entrada(SKU_MAXIMO, 1));
		assertTrue(excecao.getMessage().contains("Entrada excede o estoque máximo"));
		assertEquals(10, estoque.consultarProduto(SKU_MAXIMO).getQuantidadeAtual());
		assertEquals(1, estoque.consultarMovimentacoes().size());
		assertEquals(0, alertas.quantidadeAlertas());
	}

	private Produto produto(String sku, int quantidadeAtual, int estoqueMinimo, int estoqueMaximo) {
		return new Produto(null, sku, "Produto de regressão", quantidadeAtual, estoqueMinimo, estoqueMaximo, true);
	}
}
