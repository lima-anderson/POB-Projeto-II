package dao;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import modelo.Usuario;

public class DAOUsuario extends DAO<Usuario> {

	public Usuario read(Object chave) {
		try {
			String nome = (String) chave;
			TypedQuery<Usuario> q = manager.createQuery("select u from Usuario u where u.id = :x", Usuario.class);
			q.setParameter("x", nome);
			Usuario u = q.getSingleResult();

			return u;
		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	public List<Usuario> readAll() {
		TypedQuery<Usuario> query;
		query = manager.createQuery("select u from Usuario u", Usuario.class);

		return query.getResultList();
	}

}
