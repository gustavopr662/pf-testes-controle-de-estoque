package com.faculdade.testes.estoque.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class MovimentacaoEstoque {

	public enum Tipo {
		ENTRADA,
		SAIDA
	}

	private String id;
	private String skuProduto;
	private Tipo tipo;
	private int quantidade;
	private LocalDateTime dataHora;
	private String usuarioId;

	public MovimentacaoEstoque() {
	}

	public MovimentacaoEstoque(String skuProduto, Tipo tipo, int quantidade, String usuarioId) {
		this(UUID.randomUUID().toString(), skuProduto, tipo, quantidade, LocalDateTime.now(), usuarioId);
	}

	public MovimentacaoEstoque(String id, String skuProduto, Tipo tipo, int quantidade, LocalDateTime dataHora,
			String usuarioId) {
		this.id = id;
		this.skuProduto = skuProduto;
		this.tipo = tipo;
		this.quantidade = quantidade;
		this.dataHora = dataHora;
		this.usuarioId = usuarioId;
	}

	public MovimentacaoEstoque(MovimentacaoEstoque movimentacao) {
		this(movimentacao.id, movimentacao.skuProduto, movimentacao.tipo, movimentacao.quantidade,
				movimentacao.dataHora, movimentacao.usuarioId);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSkuProduto() {
		return skuProduto;
	}

	public void setSkuProduto(String skuProduto) {
		this.skuProduto = skuProduto;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}

	public int getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(int quantidade) {
		this.quantidade = quantidade;
	}

	public LocalDateTime getDataHora() {
		return dataHora;
	}

	public void setDataHora(LocalDateTime dataHora) {
		this.dataHora = dataHora;
	}

	public String getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(String usuarioId) {
		this.usuarioId = usuarioId;
	}
}
