package com.faculdade.testes.estoque.service;

import com.faculdade.testes.estoque.model.Produto;

import java.util.ArrayList;
import java.util.List;

public class AlertaReposicaoMemoriaListener implements AlertaReposicaoListener {

	private final List<Produto> alertas = new ArrayList<>();

	@Override
	public synchronized void onAlertaReposicao(Produto produto) {
		alertas.add(new Produto(produto));
	}

	public synchronized List<Produto> alertas() {
		return List.copyOf(alertas);
	}

	public synchronized int quantidadeAlertas() {
		return alertas.size();
	}

	public synchronized void limpar() {
		alertas.clear();
	}
}
