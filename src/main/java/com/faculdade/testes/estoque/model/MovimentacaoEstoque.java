package com.faculdade.testes.estoque.model;

import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;

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
		setId(id);
		setSkuProduto(skuProduto);
		setTipo(tipo);
		setQuantidade(quantidade);
		setDataHora(dataHora);
		setUsuarioId(usuarioId);
	}

	public MovimentacaoEstoque(MovimentacaoEstoque movimentacao) {
		this(movimentacao.id, movimentacao.skuProduto, movimentacao.tipo, movimentacao.quantidade,
				movimentacao.dataHora, movimentacao.usuarioId);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
	}

	public String getSkuProduto() {
		return skuProduto;
	}

	public void setSkuProduto(String skuProduto) {
		this.skuProduto = ValidacaoDominio.validarSku(skuProduto);
	}

	public Tipo getTipo() {
		return tipo;
	}

	public void setTipo(Tipo tipo) {
		if (tipo == null) {
			throw new OperacaoEstoqueInvalidaException("Tipo de movimentação é obrigatório");
		}
		this.tipo = tipo;
	}

	public int getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(int quantidade) {
		this.quantidade = ValidacaoDominio.validarQuantidadeOperacao(quantidade);
	}

	public LocalDateTime getDataHora() {
		return dataHora;
	}

	public void setDataHora(LocalDateTime dataHora) {
		if (dataHora == null) {
			throw new OperacaoEstoqueInvalidaException("Data e hora da movimentação são obrigatórias");
		}
		this.dataHora = dataHora;
	}

	public String getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(String usuarioId) {
		this.usuarioId = ValidacaoDominio.validarUsuarioId(usuarioId);
	}
}
