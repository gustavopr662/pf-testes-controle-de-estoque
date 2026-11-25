package com.faculdade.testes.estoque.service;

import com.faculdade.testes.estoque.exception.EstoqueInsuficienteException;
import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;
import com.faculdade.testes.estoque.model.MovimentacaoEstoque;
import com.faculdade.testes.estoque.model.MovimentacaoEstoque.Tipo;
import com.faculdade.testes.estoque.model.Produto;
import com.faculdade.testes.estoque.model.ValidacaoDominio;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class EstoqueService {

	private final AutenticacaoService autenticacao;
	private final Map<String, Produto> produtos = new HashMap<>();
	private final List<MovimentacaoEstoque> movimentacoes = new ArrayList<>();
	private final List<AlertaReposicaoListener> listeners = new ArrayList<>();

	public EstoqueService(AutenticacaoService autenticacao) {
		this.autenticacao = Objects.requireNonNull(autenticacao, "autenticacao");
	}

	public synchronized Produto cadastrarProduto(Produto produto) {
		if (produto == null) {
			throw new OperacaoEstoqueInvalidaException("Produto não pode ser nulo");
		}
		Produto copia = new Produto(produto);
		if (copia.getId() == null || copia.getId().isBlank()) {
			copia.setId(UUID.randomUUID().toString());
		}
		if (produtos.containsKey(copia.getSku())) {
			throw new OperacaoEstoqueInvalidaException("SKU já cadastrado: " + copia.getSku());
		}
		produtos.put(copia.getSku(), copia);
		return new Produto(copia);
	}

	public synchronized Produto consultarProduto(String sku) {
		return new Produto(obterProduto(sku));
	}

	public synchronized List<MovimentacaoEstoque> consultarMovimentacoes() {
		List<MovimentacaoEstoque> copias = new ArrayList<>();
		for (MovimentacaoEstoque movimentacao : movimentacoes) {
			copias.add(new MovimentacaoEstoque(movimentacao));
		}
		return List.copyOf(copias);
	}

	public synchronized void alterarStatusProduto(String sku, boolean ativo) {
		obterProduto(sku).setAtivo(ativo);
	}

	public synchronized void adicionarListener(AlertaReposicaoListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public synchronized MovimentacaoEstoque entrada(String sku, int quantidade) {
		String usuarioId = autenticacao.exigirUsuarioAutorizado();
		ValidacaoDominio.validarQuantidadeOperacao(quantidade);
		Produto produto = obterProduto(sku);
		long resultado = (long) produto.getQuantidadeAtual() + quantidade;
		if (resultado > produto.getEstoqueMaximo()) {
			throw new OperacaoEstoqueInvalidaException(
					"Entrada excede o estoque máximo do produto " + produto.getSku());
		}
		produto.setQuantidadeAtual((int) resultado);
		return registrar(produto, Tipo.ENTRADA, quantidade, usuarioId);
	}

	public synchronized MovimentacaoEstoque saida(String sku, int quantidade) {
		String usuarioId = autenticacao.exigirUsuarioAutorizado();
		ValidacaoDominio.validarQuantidadeOperacao(quantidade);
		Produto produto = obterProduto(sku);
		if (!produto.isAtivo()) {
			throw new OperacaoEstoqueInvalidaException("Produto inativo não permite saída: " + produto.getSku());
		}
		if (quantidade > produto.getQuantidadeAtual()) {
			throw new EstoqueInsuficienteException(
					"Quantidade solicitada maior que o estoque disponível para " + produto.getSku());
		}
		produto.setQuantidadeAtual(produto.getQuantidadeAtual() - quantidade);
		MovimentacaoEstoque movimentacao = registrar(produto, Tipo.SAIDA, quantidade, usuarioId);
		if (produto.getQuantidadeAtual() <= produto.getEstoqueMinimo()) {
			notificarAlerta(produto);
		}
		return movimentacao;
	}

	private MovimentacaoEstoque registrar(Produto produto, Tipo tipo, int quantidade, String usuarioId) {
		MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(produto.getSku(), tipo, quantidade, usuarioId);
		movimentacoes.add(movimentacao);
		return movimentacao;
	}

	private void notificarAlerta(Produto produto) {
		Produto snapshot = new Produto(produto);
		for (AlertaReposicaoListener listener : listeners) {
			listener.onAlertaReposicao(snapshot);
		}
	}

	private Produto obterProduto(String sku) {
		String skuNormalizado = ValidacaoDominio.validarSku(sku);
		Produto produto = produtos.get(skuNormalizado);
		if (produto == null) {
			throw new OperacaoEstoqueInvalidaException("Produto não cadastrado: " + skuNormalizado);
		}
		return produto;
	}
}
