package com.faculdade.testes.estoque;

import com.faculdade.testes.estoque.model.MovimentacaoEstoque;
import com.faculdade.testes.estoque.model.MovimentacaoEstoque.Tipo;
import com.faculdade.testes.estoque.model.Produto;
import com.faculdade.testes.estoque.service.AlertaReposicaoMemoriaListener;
import com.faculdade.testes.estoque.service.AutenticacaoService;
import com.faculdade.testes.estoque.service.EstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstoqueIntegracaoTest {

	private static final String SKU = "SKU-INTEGRACAO";
	private static final String OPERADOR = "operador.integracao";

	private AutenticacaoService autenticacao;
	private EstoqueService estoque;
	private AlertaReposicaoMemoriaListener alertas;

	@BeforeEach
	void configurar() {
		autenticacao = new AutenticacaoService();
		autenticacao.registrarUsuario(OPERADOR);
		autenticacao.login(OPERADOR);
		estoque = new EstoqueService(autenticacao);
		alertas = new AlertaReposicaoMemoriaListener();
		estoque.adicionarListener(alertas);
		estoque.cadastrarProduto(new Produto(null, SKU, "Produto integração", 12, 5, 30, true));
	}

	@Test
	void colaboracaoRealRegistraHistoricoENotificaReposicao() {
		MovimentacaoEstoque entrada = estoque.entrada(SKU, 6);

		assertEquals(18, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(0, alertas.quantidadeAlertas());

		MovimentacaoEstoque saida = estoque.saida(SKU, 13);

		assertEquals(5, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(1, alertas.quantidadeAlertas());

		List<MovimentacaoEstoque> historico = estoque.consultarMovimentacoes();
		assertEquals(2, historico.size());
		assertEquals(entrada.getId(), historico.get(0).getId());
		assertEquals(SKU, historico.get(0).getSkuProduto());
		assertEquals(Tipo.ENTRADA, historico.get(0).getTipo());
		assertEquals(6, historico.get(0).getQuantidade());
		assertEquals(OPERADOR, historico.get(0).getUsuarioId());
		assertEquals(saida.getId(), historico.get(1).getId());
		assertEquals(SKU, historico.get(1).getSkuProduto());
		assertEquals(Tipo.SAIDA, historico.get(1).getTipo());
		assertEquals(13, historico.get(1).getQuantidade());
		assertEquals(OPERADOR, historico.get(1).getUsuarioId());
		assertTrue(historico.get(0).getDataHora().isBefore(historico.get(1).getDataHora())
				|| historico.get(0).getDataHora().isEqual(historico.get(1).getDataHora()));

		Produto notificado = alertas.alertas().get(0);
		assertEquals(SKU, notificado.getSku());
		assertEquals(5, notificado.getQuantidadeAtual());
		assertEquals(5, notificado.getEstoqueMinimo());
		assertEquals(30, notificado.getEstoqueMaximo());
		assertTrue(notificado.isAtivo());
	}

	@Test
	void usuarioAutenticadoEhRegistradoEmCadaMovimentacao() {
		autenticacao.logout();
		autenticacao.registrarUsuario("segundo.operador");
		autenticacao.login("segundo.operador");

		estoque.saida(SKU, 2);

		List<MovimentacaoEstoque> historico = estoque.consultarMovimentacoes();
		assertEquals(1, historico.size());
		assertEquals("segundo.operador", historico.get(0).getUsuarioId());
		assertEquals(10, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(0, alertas.quantidadeAlertas());
	}
}
