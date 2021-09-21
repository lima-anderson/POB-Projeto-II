package dao;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import modelo.Mensagem;

public class DAOMensagem extends DAO<Mensagem> {

	public Mensagem read(Object chave) {
		try {
			String datahora = (String) chave;
			TypedQuery<Mensagem> q = manager.createQuery("select m from Mensagem m where m.id = :x", Mensagem.class);
			q.setParameter("x", datahora);
			Mensagem m = q.getSingleResult();

			return m;
		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	public List<Mensagem> readAll() {
		TypedQuery<Mensagem> query;
		query = manager.createQuery("select m from Mensagem m", Mensagem.class);

		return query.getResultList();
	}

	public List<Mensagem> buscarPorTermo(Object chave) {
		try {
			TypedQuery<Mensagem> q = manager.createQuery("select m from Mensagem m where m.texto like :x",
					Mensagem.class);
			q.setParameter("x", "%chave%");

			List<Mensagem> mensagens = q.getResultList();

			return mensagens;

		} catch (NoResultException e) {
			return null;
		}
	}
}
