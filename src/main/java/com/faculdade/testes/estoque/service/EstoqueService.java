package com.faculdade.testes.estoque.service;

import com.faculdade.testes.estoque.exception.AcessoNegadoEstoqueException;
import com.faculdade.testes.estoque.exception.EstoqueInsuficienteException;
import com.faculdade.testes.estoque.exception.OperacaoEstoqueInvalidaException;
import com.faculdade.testes.estoque.model.MovimentacaoEstoque;
import com.faculdade.testes.estoque.model.MovimentacaoEstoque.Tipo;
import com.faculdade.testes.estoque.model.Produto;
import com.faculdade.testes.estoque.model.ValidacaoDominio;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class EstoqueService {

	private final AutenticacaoService autenticacao;
	private final Map<String, Produto> produtos = new ConcurrentHashMap<>();
	private final List<MovimentacaoEstoque> movimentacoes = Collections.synchronizedList(new ArrayList<>());
	private final List<AlertaReposicaoListener> listeners = new CopyOnWriteArrayList<>();
	private final ConcurrentMap<String, ReentrantLock> bloqueiosPorSku = new ConcurrentHashMap<>();

	public EstoqueService(AutenticacaoService autenticacao) {
		this.autenticacao = Objects.requireNonNull(autenticacao, "autenticacao");
	}

	public Produto cadastrarProduto(Produto produto) {
		if (produto == null) {
			throw new OperacaoEstoqueInvalidaException("Produto não pode ser nulo");
		}
		Produto copia = new Produto(produto);
		if (copia.getId() == null || copia.getId().isBlank()) {
			copia.setId(UUID.randomUUID().toString());
		}
		String sku = copia.getSku();
		if (produtos.putIfAbsent(sku, copia) != null) {
			throw new OperacaoEstoqueInvalidaException("SKU já cadastrado: " + sku);
		}
		return new Produto(copia);
	}

	public Produto consultarProduto(String sku) {
		String skuNormalizado = ValidacaoDominio.validarSku(sku);
		Produto produto = obterProduto(skuNormalizado);
		ReentrantLock bloqueio = bloqueio(skuNormalizado);
		bloqueio.lock();
		try {
			return new Produto(produto);
		} finally {
			bloqueio.unlock();
		}
	}

	public List<MovimentacaoEstoque> consultarMovimentacoes() {
		synchronized (movimentacoes) {
			List<MovimentacaoEstoque> copias = new ArrayList<>(movimentacoes.size());
			for (MovimentacaoEstoque movimentacao : movimentacoes) {
				copias.add(new MovimentacaoEstoque(movimentacao));
			}
			return List.copyOf(copias);
		}
	}

	public void alterarStatusProduto(String sku, boolean ativo) {
		String skuNormalizado = ValidacaoDominio.validarSku(sku);
		Produto produto = obterProduto(skuNormalizado);
		ReentrantLock bloqueio = bloqueio(skuNormalizado);
		bloqueio.lock();
		try {
			produto.setAtivo(ativo);
		} finally {
			bloqueio.unlock();
		}
	}

	public void adicionarListener(AlertaReposicaoListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public MovimentacaoEstoque entrada(String sku, int quantidade) {
		String usuarioId = exigirUsuarioAutorizado();
		ValidacaoDominio.validarQuantidadeOperacao(quantidade);
		String skuNormalizado = ValidacaoDominio.validarSku(sku);
		Produto produto = obterProduto(skuNormalizado);
		ReentrantLock bloqueio = bloqueio(skuNormalizado);
		bloqueio.lock();
		try {
			long resultado = (long) produto.getQuantidadeAtual() + quantidade;
			if (resultado > produto.getEstoqueMaximo()) {
				throw new OperacaoEstoqueInvalidaException(
						"Entrada excede o estoque máximo do produto " + produto.getSku());
			}
			produto.setQuantidadeAtual((int) resultado);
			return registrar(produto, Tipo.ENTRADA, quantidade, usuarioId);
		} finally {
			bloqueio.unlock();
		}
	}

	public MovimentacaoEstoque saida(String sku, int quantidade) {
		String usuarioId = exigirUsuarioAutorizado();
		ValidacaoDominio.validarQuantidadeOperacao(quantidade);
		String skuNormalizado = ValidacaoDominio.validarSku(sku);
		Produto produto = obterProduto(skuNormalizado);
		ReentrantLock bloqueio = bloqueio(skuNormalizado);
		MovimentacaoEstoque movimentacao;
		Produto snapshotAlerta = null;
		bloqueio.lock();
		try {
			if (!produto.isAtivo()) {
				throw new OperacaoEstoqueInvalidaException("Produto inativo não permite saída: " + produto.getSku());
			}
			if (quantidade > produto.getQuantidadeAtual()) {
				throw new EstoqueInsuficienteException(
						"Quantidade solicitada maior que o estoque disponível para " + produto.getSku());
			}
			produto.setQuantidadeAtual(produto.getQuantidadeAtual() - quantidade);
			movimentacao = registrar(produto, Tipo.SAIDA, quantidade, usuarioId);
			if (produto.getQuantidadeAtual() <= produto.getEstoqueMinimo()) {
				snapshotAlerta = new Produto(produto);
			}
		} finally {
			bloqueio.unlock();
		}
		if (snapshotAlerta != null) {
			notificarAlerta(snapshotAlerta);
		}
		return movimentacao;
	}

	private String exigirUsuarioAutorizado() {
		String usuarioId = autenticacao.exigirUsuarioAutorizado();
		try {
			return ValidacaoDominio.validarUsuarioId(usuarioId);
		} catch (OperacaoEstoqueInvalidaException excecao) {
			throw new AcessoNegadoEstoqueException("Usuário autenticado com identificador inválido");
		}
	}

	private MovimentacaoEstoque registrar(Produto produto, Tipo tipo, int quantidade, String usuarioId) {
		MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(produto.getSku(), tipo, quantidade, usuarioId);
		movimentacoes.add(movimentacao);
		return movimentacao;
	}

	private void notificarAlerta(Produto snapshot) {
		for (AlertaReposicaoListener listener : listeners) {
			listener.onAlertaReposicao(new Produto(snapshot));
		}
	}

	private Produto obterProduto(String sku) {
		String skuNormalizado = ValidacaoDominio.validarSku(sku);
		Produto produto = produtos.get(skuNormalizado);
		if (produto == null) {
			throw new OperacaoEstoqueInvalidaException("Produto não cadastrado: " + skuNormalizado);
		}
		return produto;
	}

	private ReentrantLock bloqueio(String sku) {
		return bloqueiosPorSku.computeIfAbsent(sku, chave -> new ReentrantLock());
	}
}
