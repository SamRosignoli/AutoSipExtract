package br.com.nissan.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Concessionaria {

	private String codigo;

	private String descricao;

	private int index;

	private List<User> users;

	public Concessionaria(String codigo, String descricao, int index) {
		super();
		this.codigo = codigo;
		this.descricao = descricao;
		this.index = index;
		users = new ArrayList<User>();
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	/**
	 * Adciona o usuário na lista sem permitir repetidos.
	 * 
	 * @param u
	 * @return
	 */
	public boolean addUser(User u) {
		return users.contains(u) ? true : users.add(u);
	}

	public Iterator<User> getIterator() {
		return users.iterator();
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		Concessionaria other = (Concessionaria) obj;

		return this.codigo != null ? this.codigo.equals(other.codigo) : false;

	}

	@Override
	public int hashCode() {
		return this.codigo != null ? this.codigo.hashCode() : super.hashCode();
	}

}
