package fachada;

import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import dao.DAO;
import dao.DAOLog;
import dao.DAOMensagem;
import dao.DAOUsuario;
import modelo.Administrador;
import modelo.Log;
import modelo.Mensagem;
import modelo.Usuario;

public class Fachada {
	private static DAOUsuario daousuario = new DAOUsuario();
	private static DAOMensagem daomensagem = new DAOMensagem();
	private static DAOLog daolog = new DAOLog();

	private static Usuario usuariologado = null;

	public static void inicializar() {
		DAO.open();
	}

	public static void finalizar() {
		DAO.close();
	}

	public static List<Usuario> listarUsuarios() {
		// nao precisa estar logado
		return daousuario.readAll();
	}

	public static List<Mensagem> listarMensagens() {
		// nao precisa estar logado
		return daomensagem.readAll();
	}

	public static List<Log> listarLogs() {
		// nao precisa estar logado
		return daolog.readAll();
	}

	public static List<Mensagem> buscarMensagens(String termo) throws Exception {

		if (termo.isEmpty()) {
			return daomensagem.readAll();
		} else {
			return daomensagem.buscarPorTermo(termo);
		}
		/*
		 * nao precisa estar logado query no banco para obter mensagens do grupo que
		 * contenha o termo (considerar case insensitive)
		 * 
		 */
	}

	public static Usuario buscarUsuario(String termo) throws Exception {
		return daousuario.read(termo);
	}

	public static Usuario criarUsuario(String nome, String senha) throws Exception {
		// nao precisa estar logado
		DAO.begin();
		Usuario u = daousuario.read(nome + "/" + senha);
		if (u != null) {
			DAO.rollback();
			throw new Exception("criar usuario - usuario existente:" + nome);
		}

		u = new Usuario(nome + "/" + senha);
		daousuario.create(u);
		DAO.commit();
		return u;
	}

	public static void login(String nome, String senha) throws Exception {
		// verificar se ja existe um usuario logada
		if (usuariologado != null)
			throw new Exception("ja existe um usuario logado" + usuariologado.getNome());

		DAO.begin();
		Usuario u = daousuario.read(nome + "/" + senha);
		if (u == null) {
			DAO.rollback();
			throw new Exception("login - usuario inexistente:" + nome);
		}
		if (!u.ativo()) {
			DAO.rollback();
			throw new Exception("login - usuario nao ativo:" + nome);
		}
		usuariologado = u; // altera o logado na fachada

		Log log = new Log(usuariologado.getNome() + " - login");
		daolog.create(log);
		DAO.commit();
	}

	public static void logoff() {
		DAO.begin();
		Log log = new Log(usuariologado.getNome() + " - logoff");
		daolog.create(log);
		DAO.commit();

		usuariologado = null; // altera o logado na fachada
	}

	public static Usuario getLogado() {
		return usuariologado;
	}

	public static Mensagem criarMensagem(String texto) throws Exception {

		DAO.begin();

		if (usuariologado == null) {
			DAO.rollback();
			throw new Exception("O usuário precisa estar logado");
		}

		Usuario criador = usuariologado;

		Mensagem mensagem = new Mensagem(criador, texto);
		criador.adicionar(mensagem);

		daomensagem.create(mensagem);
		DAO.commit();
		return mensagem;

	}

	public static List<Mensagem> listarMensagensUsuario() throws Exception {

		DAO.begin();

		if (usuariologado == null) {
			DAO.rollback();
			throw new Exception("O usuário precisa estar logado");
		}

		List<Mensagem> mensagens = daomensagem.readAll();
		DAO.commit();

		return mensagens;
	}

	public static void apagarMensagem(int id) throws Exception {

		DAO.begin();

		if (usuariologado == null) {
			DAO.rollback();
			throw new Exception("O usuário precisa estar logado");
		}

		List<Mensagem> mensagens = usuariologado.getMensagens();

		Mensagem mensagemParaExcluir = null;

		for (Mensagem m : mensagens) {
			if (m.getId() == id) {
				mensagemParaExcluir = m;
			}
		}

		if (mensagemParaExcluir == null) {
			DAO.rollback();
			throw new Exception("Mensagem não encontrada");
		}

		usuariologado.remover(mensagemParaExcluir);
		daousuario.update(usuariologado);

		DAO.commit();
		/*
		 * tem que esta logado recebe uma lista de numeros de id (id é um numero entre 1
		 * a N, onde N é a quatidade atual de mensagens do grupo) validar se ids são de
		 * mensagens criadas pelo usuario logado (um usuario nao pode apagar mensagens
		 * de outros usuarios)
		 * 
		 * remover cada mensagem da lista de mensagens do usuario logado apagar cada
		 * mensagem do banco
		 */
	}

	public static void sairDoGrupo() throws Exception {

		if (usuariologado != null) {
			Fachada.criarMensagem(usuariologado.getNome() + " saiu do grupo!");
		}

		usuariologado.desativar();
		Fachada.logoff();

		/*
		 * tem que esta logado
		 * 
		 * criar a mensagem "fulano saiu do grupo" desativar o usuario logado e fazer
		 * logoff dele
		 */
	}

	// public static int totalMensagensUsuario() throws Exception{
	// /*
	// * tem que esta logado
	// * retorna total de mensagens criadas pelo usuario logado
	// *
	// */
	// }

	public static void esvaziar() throws Exception {
		DAO.clear();
	}

	/**************************************************************
	 * 
	 * NOVOS MÉTODOS DA FACHADA PARA O PROJETO 2
	 * 
	 **************************************************************/

	public static Administrador criarAdministrador(String nome, String senha, String email) throws Exception {
		// nao precisa estar logado
		DAO.begin();
		Usuario u = daousuario.read(nome + "/" + senha);
		if (u != null) {
			DAO.rollback();
			throw new Exception("criar administrador - usuario ja existe:" + nome);
		}

		Administrador ad = new Administrador(nome + "/" + senha, email);
		daousuario.create(ad);
		DAO.commit();
		return ad;
	}

	public static void solicitarAtivacao(String nome, String senha) throws Exception {

		Usuario usuario = Fachada.buscarUsuario(nome);

		if (usuario.isAtivo() == false) {
			Fachada.enviarEmail("Exclusão", usuario.getNome() + " solicita ativação");
		}
		/*
		 * o usuario (nome+senha) tem que estar desativado
		 * 
		 * enviar um email para o administrador com a mensagem "nome solicita ativação"
		 * usar o método Fachada.enviarEmail(...)
		 * 
		 */
	}

	public static void solicitarExclusao(String nome, String senha) throws Exception {

		Usuario usuario = Fachada.buscarUsuario(nome);

		if (usuario.isAtivo() == false) {
			Fachada.enviarEmail("Exclusão", usuario.getNome() + " solicita exclusão");
		}

		/*
		 * o usuario (nome+senha) tem que estar desativado
		 * 
		 * enviar um email para o administrador com a mensagem "nome solicita exclusão"
		 * usar o método Fachada.enviarEmail(...)
		 * 
		 */
	}

	public static void ativarUsuario(String nome) throws Exception {

		DAO.begin();

		if (usuariologado == null) {
			DAO.rollback();
			throw new Exception("O usuário precisa estar logado");
		}

		boolean adm = usuariologado instanceof Administrador;

		if (adm) {
			Usuario usuarioParaSerAtivado = daousuario.read(nome);
			boolean estaDesativado = !usuarioParaSerAtivado.isAtivo();

			if (estaDesativado) {
				Fachada.ativarUsuario(usuarioParaSerAtivado.getNome());
				Fachada.criarMensagem(usuarioParaSerAtivado.getNome() + " entrou no grupo");
			}

		} else {
			DAO.rollback();
			throw new Exception("O usuário precisa ser um administrador");
		}

		DAO.commit();

		/*
		 * o usuario logado tem que ser um administrador e o usuario a ser ativado
		 * (nome) tem que estar desativado
		 * 
		 * ativar o usuario criar a mensagem "nome entrou no grupo"
		 * 
		 */
	}

	public static void apagarUsuario(String nome) throws Exception {

		DAO.begin();

		if (usuariologado == null) {
			DAO.rollback();
			throw new Exception("O usuário precisa estar logado");
		}

		boolean ehAdm = usuariologado instanceof Administrador;

		if (ehAdm) {
			Usuario usuarioParaSerApagado = daousuario.read(nome);
			boolean estaDesativado = !usuarioParaSerApagado.isAtivo();

			boolean usuarioComum = !(usuarioParaSerApagado instanceof Administrador);

			if (estaDesativado && usuarioComum) {
				criarMensagem(usuarioParaSerApagado.getNome() + " foi excluído do sistema");
				daousuario.delete(usuarioParaSerApagado);
			} else {
				DAO.rollback();
				throw new Exception("O usuário precisa estar desativado e não pode ser um administrador");
			}

		}

		DAO.commit();

		/*
		 * o usuario logado tem que ser um administrador e o usuario a ser apagado tem
		 * que estar desativado (e não pode ser do tipo Administrador)
		 * 
		 * apagar as mensagens do usuario e apagar o usuario criar a mensagem
		 * "nome foi excluido do sistema"
		 */
	}

	/**************************************************************
	 * 
	 * MÉTODO PARA ENVIAR EMAIL, USANDO UMA CONTA (SMTP) DO GMAIL ELE ABRE UMA
	 * JANELA PARA PEDIR A SENHA DO EMAIL DO EMITENTE ELE USA A BIBLIOTECA JAVAMAIL
	 * (ver pom.xml) Lembrar de: 1. desligar antivirus e de 2. ativar opcao "Acesso
	 * a App menos seguro" na conta do gmail
	 * 
	 **************************************************************/
	public static void enviarEmail(String assunto, String mensagem) {
		try {
			/*
			 * ******************************************************** Obs: lembrar de
			 * desligar antivirus e de ativar "Acesso a App menos seguro" na conta do gmail
			 * 
			 * pom.xml contem a dependencia javax.mail
			 * 
			 * ********************************************************
			 */
			// configurar emails
			String emailorigem = "xxxxxxxxxxxxx@gmail.com";
			String senhaorigem = pegarSenha();
			String emaildestino = "yyyyyyyyyyyy@gmail.com";

			// Gmail
			Properties props = new Properties();
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.port", "587");
			props.put("mail.smtp.auth", "true");

			Session session;
			session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(emailorigem, senhaorigem);
				}
			});

			MimeMessage message = new MimeMessage(session);
			message.setSubject(assunto);
			message.setFrom(new InternetAddress(emailorigem));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emaildestino));
			message.setText(mensagem); // usar "\n" para quebrar linhas
			Transport.send(message);

			// System.out.println("enviado com sucesso");

		} catch (MessagingException e) {
			System.out.println(e.getMessage());
		}
	}

	/*
	 * JANELA PARA DIGITAR A SENHA DO EMAIL
	 */
	public static String pegarSenha() {
		JPasswordField field = new JPasswordField(10);
		field.setEchoChar('*');
		JPanel painel = new JPanel();
		painel.add(new JLabel("Entre com a senha do email:"));
		painel.add(field);
		JOptionPane.showMessageDialog(null, painel, "Senha", JOptionPane.PLAIN_MESSAGE);
		String texto = new String(field.getPassword());
		return texto.trim();
	}
}
