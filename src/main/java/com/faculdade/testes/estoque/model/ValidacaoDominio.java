package com.faculdade.testes.estoque.model;

import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ValidacaoDominio {

	private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
	private static final Pattern USUARIO_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
	private static final String[] SEQUENCIAS_SUSPEITAS = {
			"--", "/*", "*/", ";", "'", "\"", "<", ">", "/", "\\", "..", "%", "?", "&", "|", "="
	};

	private ValidacaoDominio() {
	}

	public static String validarSku(String sku) {
		return validarIdentificador(sku, "SKU", SKU_PATTERN);
	}

	public static String validarUsuarioId(String usuarioId) {
		return validarIdentificador(usuarioId, "Identificador de usuário", USUARIO_ID_PATTERN);
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

	private static String validarIdentificador(String valor, String nomeCampo, Pattern padrao) {
		if (valor == null) {
			throw new OperacaoEstoqueInvalidaException(nomeCampo + " não pode ser nulo");
		}
		String normalizado = valor.trim();
		if (normalizado.isEmpty()) {
			throw new OperacaoEstoqueInvalidaException(nomeCampo + " não pode ser vazio");
		}
		if (contemConteudoSuspeito(normalizado)) {
			throw new OperacaoEstoqueInvalidaException(
					nomeCampo + " contém conteúdo suspeito (injeção, path traversal ou caracteres de controle)");
		}
		if (!padrao.matcher(normalizado).matches()) {
			throw new OperacaoEstoqueInvalidaException(
					nomeCampo + " contém caracteres inválidos: use apenas letras, números, '.', '_' ou '-'");
		}
		return normalizado;
	}

	private static boolean contemConteudoSuspeito(String valor) {
		String minusculas = valor.toLowerCase(Locale.ROOT);
		for (String sequencia : SEQUENCIAS_SUSPEITAS) {
			if (minusculas.contains(sequencia)) {
				return true;
			}
		}
		for (int indice = 0; indice < valor.length(); indice++) {
			if (Character.isISOControl(valor.charAt(indice))) {
				return true;
			}
		}
		return false;
	}
}
