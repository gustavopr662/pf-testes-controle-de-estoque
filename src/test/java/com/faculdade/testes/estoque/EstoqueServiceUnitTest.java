package com.faculdade.testes.estoque;

import com.faculdade.testes.estoque.exception.AcessoNegadoEstoqueException;
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

@ExtendWith(MockitoExtension.class)
class EstoqueServiceUnitTest {

	private static final String SKU = "SKU-001";
	private static final String USUARIO = "Usuario-Teste";

	@Mock
	private AlertaReposicaoListener alerta;

	private AutenticacaoService autenticacao;
	private EstoqueService estoque;

	@BeforeEach
	void configurar() {
		autenticacao = new AutenticacaoService();
		autenticacao.registrarUsuario(USUARIO);
		autenticacao.login(USUARIO);
		estoque = new EstoqueService(autenticacao);
		estoque.adicionarListener(alerta);
	}

	@Test
	void entradaValidaAtualizaSaldoSemDispararAlerta() {
		estoque.cadastrarProduto(produto(5, 2, 20, true));

		MovimentacaoEstoque movimentacao = estoque.entrada(SKU, 5);

		assertEquals(10, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(MovimentacaoEstoque.Tipo.ENTRADA, movimentacao.getTipo());
		assertEquals(USUARIO, movimentacao.getUsuarioId());
		verifyNoInteractions(alerta);
	}

	@Test
	void entradaAcimaDoMaximoLancaExcecaoENaoAlteraSaldo() {
		estoque.cadastrarProduto(produto(18, 2, 20, true));

		OperacaoEstoqueInvalidaException excecao = assertThrows(OperacaoEstoqueInvalidaException.class,
				() -> estoque.entrada(SKU, 3));

		assertTrue(excecao.getMessage().contains("máximo"));
		assertEquals(18, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	@Test
	void saidaValidaAtingindoMinimoDisparaAlertaComSnapshotDoProduto() {
		estoque.cadastrarProduto(produto(10, 5, 20, true));

		MovimentacaoEstoque movimentacao = estoque.saida(SKU, 5);

		assertEquals(5, estoque.consultarProduto(SKU).getQuantidadeAtual());
		assertEquals(MovimentacaoEstoque.Tipo.SAIDA, movimentacao.getTipo());
		ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
		verify(alerta, times(1)).onAlertaReposicao(captor.capture());
		assertEquals(SKU, captor.getValue().getSku());
		assertEquals(5, captor.getValue().getQuantidadeAtual());
	}

	@Test
	void saidaAcimaDoSaldoLancaEstoqueInsuficienteENaoAlteraSaldo() {
		estoque.cadastrarProduto(produto(3, 2, 20, true));

		assertThrows(EstoqueInsuficienteException.class, () -> estoque.saida(SKU, 4));

		assertEquals(3, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	@Test
	void saidaEmProdutoInativoLancaExcecaoENaoDisparaAlerta() {
		estoque.cadastrarProduto(produto(10, 2, 20, false));

		assertThrows(OperacaoEstoqueInvalidaException.class, () -> estoque.saida(SKU, 1));

		assertEquals(10, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	@Test
	void saidaQueMantemSaldoAcimaDoMinimoNaoDisparaAlerta() {
		estoque.cadastrarProduto(produto(10, 2, 20, true));

		estoque.saida(SKU, 3);

		assertEquals(7, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	@Test
	void entradaNaoDisparaAlertaMesmoQuandoSaldoFicaAbaixoDoMinimo() {
		estoque.cadastrarProduto(produto(1, 5, 20, true));

		estoque.entrada(SKU, 2);

		assertEquals(3, estoque.consultarProduto(SKU).getQuantidadeAtual());
		verifyNoInteractions(alerta);
	}

	@Test
	void usuarioSemPermissaoNaoRealizaEntrada() {
		AutenticacaoService semPermissao = new AutenticacaoService();
		semPermissao.registrarUsuario(USUARIO);
		semPermissao.revogarPermissao(USUARIO);
		EstoqueService servico = new EstoqueService(semPermissao);
		servico.cadastrarProduto(produto(1, 1, 10, true));

		assertThrows(AcessoNegadoEstoqueException.class, () -> servico.entrada(SKU, 1));
		assertEquals(1, servico.consultarProduto(SKU).getQuantidadeAtual());
	}

	@Test
	void usuarioInativoNaoRealizaSaida() {
		estoque.cadastrarProduto(produto(5, 2, 20, true));
		autenticacao.desativarUsuario(USUARIO);

		assertThrows(AcessoNegadoEstoqueException.class, () -> estoque.saida(SKU, 1));
		assertEquals(5, estoque.consultarProduto(SKU).getQuantidadeAtual());
	}

	@Test
	void produtoComSkuSuspeitoEhRejeitadoNaConstrucao() {
		assertThrows(OperacaoEstoqueInvalidaException.class,
				() -> new Produto(null, "SKU;DROP", "Produto", 1, 1, 10, true));
	}

	private Produto produto(int quantidadeAtual, int estoqueMinimo, int estoqueMaximo, boolean ativo) {
		return new Produto(null, SKU, "Produto unitário", quantidadeAtual, estoqueMinimo, estoqueMaximo, ativo);
	}
}
