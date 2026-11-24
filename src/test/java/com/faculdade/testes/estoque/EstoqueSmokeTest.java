package com.faculdade.testes.estoque;

import com.faculdade.testes.estoque.model.Produto;
import com.faculdade.testes.estoque.service.AutenticacaoService;
import com.faculdade.testes.estoque.service.EstoqueService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstoqueSmokeTest {

	@Test
	void deveExecutarEntradaESaidaBasicas() {
		AutenticacaoService autenticacao = new AutenticacaoService();
		autenticacao.concederPermissao("usuario-teste");
		autenticacao.login("usuario-teste");

		EstoqueService estoque = new EstoqueService(autenticacao);
		final boolean[] alertaRecebido = {false};
		estoque.adicionarListener(produto -> alertaRecebido[0] = true);
		estoque.cadastrarProduto(new Produto(null, "SKU-001", "Produto teste", 5, 2, 20, true));

		estoque.entrada("SKU-001", 5);
		assertEquals(10, estoque.consultarProduto("SKU-001").getQuantidadeAtual());

		estoque.saida("SKU-001", 8);
		assertEquals(2, estoque.consultarProduto("SKU-001").getQuantidadeAtual());
		assertEquals(2, estoque.consultarMovimentacoes().size());
		assertTrue(alertaRecebido[0]);
	}
}
