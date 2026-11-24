package com.faculdade.testes.estoque.service;

import com.faculdade.testes.estoque.exception.EstoqueInsuficienteException;
import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;
import com.faculdade.testes.estoque.model.MovimentacaoEstoque;
import com.faculdade.testes.estoque.model.MovimentacaoEstoque.Tipo;
import com.faculdade.testes.estoque.model.Produto;
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
		validarProduto(produto);
		if (produto.getId() == null || produto.getId().isBlank()) {
			produto.setId(UUID.randomUUID().toString());
		}
		produto.setSku(produto.getSku().trim());
		Produto copia = new Produto(produto);
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

	public synchronized void adicionarListener(AlertaReposicaoListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public synchronized MovimentacaoEstoque entrada(String sku, int quantidade) {
		String usuarioId = autenticacao.exigirUsuarioAutorizado();
		validarQuantidade(quantidade);
		Produto produto = obterProduto(sku);
		long resultado = (long) produto.getQuantidadeAtual() + quantidade;
		if (resultado > produto.getEstoqueMaximo()) {
			throw new OperacaoEstoqueInvalidaException(
					"Entrada excede o estoque máximo do produto " + produto.getSku());
		}
		produto.setQuantidadeAtual((int) resultado);
		MovimentacaoEstoque movimentacao = registrar(produto, Tipo.ENTRADA, quantidade, usuarioId);
		notificarAlerta(produto);
		return movimentacao;
	}

	public synchronized MovimentacaoEstoque saida(String sku, int quantidade) {
		String usuarioId = autenticacao.exigirUsuarioAutorizado();
		validarQuantidade(quantidade);
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
		notificarAlerta(produto);
		return movimentacao;
	}

	private MovimentacaoEstoque registrar(Produto produto, Tipo tipo, int quantidade, String usuarioId) {
		MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(produto.getSku(), tipo, quantidade, usuarioId);
		movimentacoes.add(movimentacao);
		return movimentacao;
	}

	private void notificarAlerta(Produto produto) {
		if (produto.getQuantidadeAtual() <= produto.getEstoqueMinimo()) {
			Produto snapshot = new Produto(produto);
			for (AlertaReposicaoListener listener : listeners) {
				listener.onAlertaReposicao(snapshot);
			}
		}
	}

	private Produto obterProduto(String sku) {
		String skuNormalizado = validarSku(sku);
		Produto produto = produtos.get(skuNormalizado);
		if (produto == null) {
			throw new OperacaoEstoqueInvalidaException("Produto não cadastrado: " + skuNormalizado);
		}
		return produto;
	}

	private void validarProduto(Produto produto) {
		if (produto == null) {
			throw new OperacaoEstoqueInvalidaException("Produto não pode ser nulo");
		}
		String sku = validarSku(produto.getSku());
		if (produto.getNome() == null || produto.getNome().isBlank()) {
			throw new OperacaoEstoqueInvalidaException("Nome do produto é obrigatório");
		}
		if (produto.getQuantidadeAtual() < 0 || produto.getEstoqueMinimo() < 0 || produto.getEstoqueMaximo() < 1) {
			throw new OperacaoEstoqueInvalidaException("Quantidades e limites de estoque devem ser válidos");
		}
		if (produto.getEstoqueMinimo() > produto.getEstoqueMaximo()) {
			throw new OperacaoEstoqueInvalidaException("Estoque mínimo não pode exceder o estoque máximo");
		}
		if (produto.getQuantidadeAtual() > produto.getEstoqueMaximo()) {
			throw new OperacaoEstoqueInvalidaException("Quantidade atual não pode exceder o estoque máximo");
		}
		if (produtos.containsKey(sku)) {
			throw new OperacaoEstoqueInvalidaException("SKU já cadastrado: " + sku);
		}
	}

	private String validarSku(String sku) {
		if (sku == null || sku.isBlank()) {
			throw new OperacaoEstoqueInvalidaException("SKU é obrigatório");
		}
		return sku.trim();
	}

	private void validarQuantidade(int quantidade) {
		if (quantidade < 1) {
			throw new OperacaoEstoqueInvalidaException("Quantidade deve ser maior ou igual a 1");
		}
	}
}
