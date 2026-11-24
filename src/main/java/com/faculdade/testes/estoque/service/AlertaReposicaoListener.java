package com.faculdade.testes.estoque.service;

import com.faculdade.testes.estoque.model.Produto;

public interface AlertaReposicaoListener {

	void onAlertaReposicao(Produto produto);
}
