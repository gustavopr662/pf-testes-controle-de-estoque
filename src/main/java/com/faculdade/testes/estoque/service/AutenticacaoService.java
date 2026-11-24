package com.faculdade.testes.estoque.service;

import com.faculdade.testes.estoque.exception.AcessoNegadoEstoqueException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AutenticacaoService {

	private final Set<String> usuariosAutorizados = new HashSet<>();
	private String usuarioAtivoId;

	public synchronized void concederPermissao(String usuarioId) {
		usuariosAutorizados.add(validarUsuarioId(usuarioId));
	}

	public synchronized void revogarPermissao(String usuarioId) {
		String id = validarUsuarioId(usuarioId);
		usuariosAutorizados.remove(id);
		if (id.equals(usuarioAtivoId)) {
			usuarioAtivoId = null;
		}
	}

	public synchronized void login(String usuarioId) {
		String id = validarUsuarioId(usuarioId);
		if (!usuariosAutorizados.contains(id)) {
			throw new AcessoNegadoEstoqueException("Usuário não autorizado para operações de estoque: " + id);
		}
		usuarioAtivoId = id;
	}

	public synchronized void logout() {
		usuarioAtivoId = null;
	}

	public synchronized String usuarioAtivoId() {
		return usuarioAtivoId;
	}

	public synchronized String exigirUsuarioAutorizado() {
		if (usuarioAtivoId == null || !usuariosAutorizados.contains(usuarioAtivoId)) {
			throw new AcessoNegadoEstoqueException("É necessário estar autenticado para operar o estoque");
		}
		return usuarioAtivoId;
	}

	private String validarUsuarioId(String usuarioId) {
		if (usuarioId == null || usuarioId.isBlank()) {
			throw new AcessoNegadoEstoqueException("Identificador de usuário inválido");
		}
		return usuarioId.trim();
	}
}
