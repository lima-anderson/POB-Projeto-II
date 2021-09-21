package dao;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import modelo.Log;

public class DAOLog extends DAO<Log> {

	public Log read(Object chave) {
		try {
			String nome = (String) chave;
			TypedQuery<Log> q = manager.createQuery("select l from Log l where l.id = :x", Log.class);
			q.setParameter("x", nome);
			Log l = q.getSingleResult();

			return l;
		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	public List<Log> readAll() {
		TypedQuery<Log> query;
		query = manager.createQuery("select l from Log l", Log.class);

		return query.getResultList();
	}

}
