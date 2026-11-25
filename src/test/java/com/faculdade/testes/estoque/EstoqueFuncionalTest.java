package com.faculdade.testes.estoque;

import com.faculdade.testes.estoque.exception.EstoqueInsuficienteException;
import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;
import com.faculdade.testes.estoque.model.Produto;
import com.faculdade.testes.estoque.service.AlertaReposicaoMemoriaListener;
import com.faculdade.testes.estoque.service.AutenticacaoService;
import com.faculdade.testes.estoque.service.EstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstoqueFuncionalTest {

	private static final String SKU = "SKU-100";

	private EstoqueService estoque;
	private AlertaReposicaoMemoriaListener alertas;

	@BeforeEach
	void configurar() {
		AutenticacaoService autenticacao = new AutenticacaoService();
		autenticacao.registrarUsuario("operador");
		autenticacao.login("operador");
		estoque = new EstoqueService(autenticacao);
		alertas = new AlertaReposicaoMemoriaListener();
		estoque.adicionarListener(alertas);
		estoque.cadastrarProduto(new Produto(null, SKU, "Teclado", 10, 5, 20, true));
	}

	@Test
	void fluxoCompletoComEntradasSaidasReposicaoEBloqueios() {
		estoque.entrada(SKU, 5);
		estoque.saida(SKU, 9);
		assertEquals(6, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(0, alertas.quantidadeAlertas());

		estoque.saida(SKU, 1);
		assertEquals(5, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(1, alertas.quantidadeAlertas());
		assertEquals(5, alertas.alertas().get(0).getQuantidadeAtual());

		estoque.entrada(SKU, 5);
		assertEquals(10, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(1, alertas.quantidadeAlertas());

		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.entrada(SKU, 11));
		assertThrows(EstoqueInsuficienteException.class, () -> estoque.saida(SKU, 11));
		assertEquals(10, estoque.consultarProduto(SKU).getQuantidadeAtual());

		estoque.alterarStatusProduto(SKU, false);
		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.saida(SKU, 1));
		assertEquals(10, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(4, estoque.consultarMovimentacoes().size());
	}

	@Test
	void reposicaoDepoisDoAlertaNaoGeraNovoAlerta() {
		estoque.saida(SKU, 6);
		assertEquals(4, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(1, alertas.quantidadeAlertas());

		estoque.entrada(SKU, 10);
		assertEquals(14, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(1, alertas.quantidadeAlertas());
	}

	@Test
	void operacoesComSkuInvalidoSaoRejeitadas() {
		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.entrada(null, 1));
		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.entrada("   ", 1));
		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.saida("SKU/100", 1));
		assertEquals(0, estoque.consultarMovimentacoes().size());
	}
}
