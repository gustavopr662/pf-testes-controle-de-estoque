package com.faculdade.testes.estoque.model;

import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;

import java.util.regex.Pattern;

public final class ValidacaoDominio {

	private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

	private ValidacaoDominio() {
	}

	public static String validarSku(String sku) {
		if (sku == null) {
			throw new OperacaoEstoqueInvalidaException("SKU não pode ser nulo");
		}
		String normalizado = sku.trim();
		if (normalizado.isEmpty()) {
			throw new OperacaoEstoqueInvalidaException("SKU não pode ser vazio");
		}
		if (!SKU_PATTERN.matcher(normalizado).matches()) {
			throw new OperacaoEstoqueInvalidaException(
					"SKU contém caracteres inválidos: use apenas letras, números, '.', '_' ou '-'");
		}
		return normalizado;
	}

	public static String validarTextoObrigatorio(String valor, String nomeCampo) {
		if (valor == null || valor.isBlank()) {
			throw new OperacaoEstoqueInvalidaException(nomeCampo + " é obrigatório");
		}
		return valor.trim();
	}

	public static int validarQuantidadeOperacao(int quantidade) {
		if (quantidade < 1) {
			throw new OperacaoEstoqueInvalidaException("Quantidade deve ser maior ou igual a 1");
		}
		return quantidade;
	}

	public static void validarNaoNegativo(int valor, String nomeCampo) {
		if (valor < 0) {
			throw new OperacaoEstoqueInvalidaException(nomeCampo + " não pode ser negativo");
		}
	}
}
