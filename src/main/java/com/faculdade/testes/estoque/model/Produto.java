package com.faculdade.testes.estoque.model;

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
		this.id = id;
		this.sku = sku;
		this.nome = nome;
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
		this.id = id;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public int getQuantidadeAtual() {
		return quantidadeAtual;
	}

	public void setQuantidadeAtual(int quantidadeAtual) {
		this.quantidadeAtual = quantidadeAtual;
	}

	public int getEstoqueMinimo() {
		return estoqueMinimo;
	}

	public void setEstoqueMinimo(int estoqueMinimo) {
		this.estoqueMinimo = estoqueMinimo;
	}

	public int getEstoqueMaximo() {
		return estoqueMaximo;
	}

	public void setEstoqueMaximo(int estoqueMaximo) {
		this.estoqueMaximo = estoqueMaximo;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}
}
