package br.com.nissan.domain;

public class User {

	private String codigo;

	private String nome;

	private int index;

	public User(String codigo, String nome, int index) {
		super();
		this.codigo = codigo;
		this.nome = nome;
		this.index = index;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String descricao) {
		this.nome = descricao;
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

		User other = (User) obj;

		return this.codigo != null ? this.codigo.equals(other.codigo) : false;

	}

}
