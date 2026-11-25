package com.faculdade.testes.estoque;

import com.faculdade.testes.estoque.exception.EstoqueInsuficienteException;
import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;
import com.faculdade.testes.estoque.model.MovimentacaoEstoque;
import com.faculdade.testes.estoque.model.Produto;
import com.faculdade.testes.estoque.service.AlertaReposicaoListener;
import com.faculdade.testes.estoque.service.AutenticacaoService;
import com.faculdade.testes.estoque.service.EstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueUnitarioTest {

	private static final String SKU = "SKU-UNITARIO";
	private static final String USUARIO = "operador.unitario";

	@Mock
	private AutenticacaoService autenticacao;

	@Mock
	private AlertaReposicaoListener alerta;

	private EstoqueService estoque;

	@BeforeEach
	void configurar() {
		when(autenticacao.exigirUsuarioAutorizado()).thenReturn(USUARIO);
		estoque = new EstoqueService(autenticacao);
		estoque.adicionarListener(alerta);
	}

	@Test
	void entradaComQuantidadeMinima1_deveAtualizarSaldo() {
		cadastrar(0, 0, 10, true);

		MovimentacaoEstoque movimentacao = estoque.entrada(SKU, 1);

		assertEquals(1, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(MovimentacaoEstoque.Tipo.ENTRADA, movimentacao.getTipo());
		assertEquals(USUARIO, movimentacao.getUsuarioId());
		verifyNoInteractions(alerta);
	}

	@Test
	void entradaAcimaDoEstoqueMaximo_deveLancarExcecao() {
		cadastrar(9, 0, 10, true);

		OperacaoEstoqueInvalidaException excecao = assertThrows(OperacaoEstoqueInvalidaException.class,
				() -> estoque.entrada(SKU, 2));

		assertTrue(excecao.getMessage().contains("Entrada excede o estoque máximo"));
		assertEquals(9, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	@Test
	void saidaAcimaDoSaldoAtual_deveLancarEstoqueInsuficienteException() {
		cadastrar(3, 1, 10, true);

		assertThrows(EstoqueInsuficienteException.class, () -> estoque.saida(SKU, 4));

		assertEquals(3, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	@Test
	void saidaDeixandoSaldoIgualAoMinimo_deveDispararAlertaReposicao() {
		cadastrar(10, 5, 20, true);

		MovimentacaoEstoque movimentacao = estoque.saida(SKU, 5);

		assertEquals(5, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(MovimentacaoEstoque.Tipo.SAIDA, movimentacao.getTipo());
		ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
		verify(alerta, times(1)).onAlertaReposicao(captor.capture());
		assertEquals(SKU, captor.getValue().getSku());
		assertEquals(5, captor.getValue().getQuantidadeAtual());
	}

	@Test
	void saidaAcimaDoMinimo_naoDeveDispararAlertaReposicao() {
		cadastrar(10, 5, 20, true);

		estoque.saida(SKU, 4);

		assertEquals(6, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	@Test
	void saidaEmProdutoInativo_deveLancarExcecao() {
		cadastrar(10, 5, 20, false);

		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.saida(SKU, 1));

		assertEquals(10, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	@Test
	void entradaComQuantidadeZero_deveLancarExcecao() {
		cadastrar(5, 1, 20, true);

		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.entrada(SKU, 0));

		assertEquals(5, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	@Test
	void entradaComQuantidadeNegativa_deveLancarExcecao() {
		cadastrar(5, 1, 20, true);

		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.entrada(SKU, -1));

		assertEquals(5, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	private void cadastrar(int quantidadeAtual, int estoqueMinimo, int estoqueMaximo, boolean ativo) {
		estoque.cadastrarProduto(new Produto(null, SKU, "Produto unitário", quantidadeAtual, estoqueMinimo,
				estoqueMaximo, ativo));
	}
}
