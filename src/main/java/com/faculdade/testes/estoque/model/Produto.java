package com.faculdade.testes.estoque.model;

import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;

public class Produto {

	private String id;
	private String sku;
	private String nome;
	private int quantidadeAtual;
	private int estoqueMinimo;
	private int estoqueMaximo;
	private boolean ativo;

	public Produto() {
	}

	public Produto(String id, String sku, String nome, int quantidadeAtual, int estoqueMinimo, int estoqueMaximo,
			boolean ativo) {
		this.id = normalizarId(id);
		this.sku = ValidacaoDominio.validarSku(sku);
		this.nome = ValidacaoDominio.validarTextoObrigatorio(nome, "Nome do produto");
		ValidacaoDominio.validarNaoNegativo(quantidadeAtual, "Quantidade atual");
		ValidacaoDominio.validarNaoNegativo(estoqueMinimo, "Estoque mínimo");
		validarEstoqueMaximo(estoqueMaximo);
		validarInvariantes(quantidadeAtual, estoqueMinimo, estoqueMaximo);
		this.quantidadeAtual = quantidadeAtual;
		this.estoqueMinimo = estoqueMinimo;
		this.estoqueMaximo = estoqueMaximo;
		this.ativo = ativo;
	}

	public Produto(Produto produto) {
		this(produto.id, produto.sku, produto.nome, produto.quantidadeAtual, produto.estoqueMinimo,
				produto.estoqueMaximo, produto.ativo);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = normalizarId(id);
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = ValidacaoDominio.validarSku(sku);
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = ValidacaoDominio.validarTextoObrigatorio(nome, "Nome do produto");
	}

	public int getQuantidadeAtual() {
		return quantidadeAtual;
	}

	public void setQuantidadeAtual(int quantidadeAtual) {
		ValidacaoDominio.validarNaoNegativo(quantidadeAtual, "Quantidade atual");
		validarInvariantes(quantidadeAtual, this.estoqueMinimo, this.estoqueMaximo);
		this.quantidadeAtual = quantidadeAtual;
	}

	public int getEstoqueMinimo() {
		return estoqueMinimo;
	}

	public void setEstoqueMinimo(int estoqueMinimo) {
		ValidacaoDominio.validarNaoNegativo(estoqueMinimo, "Estoque mínimo");
		validarInvariantes(this.quantidadeAtual, estoqueMinimo, this.estoqueMaximo);
		this.estoqueMinimo = estoqueMinimo;
	}

	public int getEstoqueMaximo() {
		return estoqueMaximo;
	}

	public void setEstoqueMaximo(int estoqueMaximo) {
		validarEstoqueMaximo(estoqueMaximo);
		validarInvariantes(this.quantidadeAtual, this.estoqueMinimo, estoqueMaximo);
		this.estoqueMaximo = estoqueMaximo;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}

	private static String normalizarId(String id) {
		return id == null || id.isBlank() ? null : id.trim();
	}

	private static void validarEstoqueMaximo(int estoqueMaximo) {
		if (estoqueMaximo < 1) {
			throw new OperacaoEstoqueInvalidaException("Estoque máximo deve ser maior ou igual a 1");
		}
	}

	private static void validarInvariantes(int quantidadeAtual, int estoqueMinimo, int estoqueMaximo) {
		if (estoqueMinimo > estoqueMaximo) {
			throw new OperacaoEstoqueInvalidaException("Estoque mínimo não pode exceder o estoque máximo");
		}
		if (quantidadeAtual > estoqueMaximo) {
			throw new OperacaoEstoqueInvalidaException("Quantidade atual não pode exceder o estoque máximo");
		}
	}
}
