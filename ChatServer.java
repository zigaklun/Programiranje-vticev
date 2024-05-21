import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.*;

public class ChatServer {

	protected int serverPort = 1234;
	protected Map<Socket, String> clients = new HashMap<Socket, String>();

	public static void main(String[] args) throws Exception {
		new ChatServer();
	}

	public ChatServer() {
		SSLServerSocket ss = null;
		String username = null;

		// create socket
		try {
			String passphrase = "serverpwd";
			
			// preberi datoteko z odjemalskimi certifikati
			KeyStore clientKeyStore = KeyStore.getInstance("JKS"); // KeyStore za shranjevanje odjemalčevih javnih ključev (certifikatov)
			clientKeyStore.load(new FileInputStream("clients.public"), "public".toCharArray());

			// preberi datoteko s svojim certifikatom in tajnim ključem
			KeyStore serverKeyStore = KeyStore.getInstance("JKS"); // KeyStore za shranjevanje strežnikovega tajnega in javnega ključa
			serverKeyStore.load(new FileInputStream("server.private"), passphrase.toCharArray());

			// vzpostavi SSL kontekst (komu zaupamo, kakšni so moji tajni ključi in certifikati)
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(clientKeyStore);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(serverKeyStore, passphrase.toCharArray());
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), (new SecureRandom()));

			// kreiramo socket
			SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
			ss = (SSLServerSocket) factory.createServerSocket(this.serverPort);
			ss.setNeedClientAuth(true); // tudi odjemalec se MORA predstaviti s certifikatom
			ss.setEnabledCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"});


			Socket socket = ss.accept(); // vzpostavljena povezava
			((SSLSocket)socket).startHandshake(); // eksplicitno sprozi SSL Handshake
			username = ((SSLSocket) socket).getSession().getPeerPrincipal().getName();
			System.out.println("Established SSL connection with: " + username);

		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		try {
			while (true) {
				Socket newClientSocket = ss.accept(); // wait for a new client connection
				String newClientName = username;
				synchronized (this) {
					clients.put(newClientSocket, newClientName); // add client to the list of clients
				}
				ChatServerConnector conn = new ChatServerConnector(this, newClientSocket, newClientName); // create a new thread for communication with the new client
				conn.start(); // run the new thread

			}
		} catch (Exception e) {
			System.err.println("[error] Accept failed.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// close socket
		System.out.println("[system] closing server socket ...");
		try {
			ss.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	// send a message to all clients connected to the server
	public void sendToAllClients(String message) throws Exception {

		Set<Socket> allClientSockets = clients.keySet();
		Iterator<Socket> i = allClientSockets.iterator();

		while (i.hasNext()) { // iterate through the client list
			Socket socket = (Socket) i.next(); // get the socket for communicating with this client
			try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client
				out.writeUTF(message); // send message to the client
			} catch (Exception e) {
				System.err.println("[system] could not send message to a client");
				e.printStackTrace(System.err);
			}
		}
	}

	public void sendToOneClient (String message) throws Exception {
		String dolzinaPos = message.substring(1, 2);
		String posiljatelj = message.substring(2, 2 + Integer.parseInt(dolzinaPos));
		String dolzPrej = message.substring(19 + Integer.parseInt(dolzinaPos), 20 + Integer.parseInt(dolzinaPos));
		String prejemnik = message.substring(21 + Integer.parseInt(dolzinaPos),21 + Integer.parseInt(dolzinaPos)+ Integer.parseInt(dolzPrej));
		int stevec = 0;
		for(Map.Entry<Socket, String> entry : clients.entrySet()){
			Socket s = entry.getKey();
			String n = entry.getValue();
			if(n.equals(prejemnik)){
				stevec++;
				try {
					DataOutputStream out = new DataOutputStream(s.getOutputStream()); // create output stream for sending messages to the client
					out.writeUTF(message);
				}catch(Exception e){
					System.err.println("[system] could not send message to a client");
					e.printStackTrace(System.err);
				}
			}
		}
		if(stevec == 0){
			for(Map.Entry<Socket, String> entry : clients.entrySet()){
				Socket s = entry.getKey();
				String n = entry.getValue();
				if(n.equals(posiljatelj)){
					try {
						DataOutputStream out = new DataOutputStream(s.getOutputStream()); // create output stream for sending messages to the client
						out.writeUTF("Username not found");
					}catch(Exception e){
						System.err.println("[system] could not send message to a client");
						e.printStackTrace(System.err);
					}
					break;
				}
			}
		}
		else {
			for(Map.Entry<Socket, String> entry : clients.entrySet()){
				Socket s = entry.getKey();
				String n = entry.getValue();
				if(n.equals(posiljatelj)){
					try {
						DataOutputStream out = new DataOutputStream(s.getOutputStream()); // create output stream for sending messages to the client
						out.writeUTF(message);
					}catch(Exception e){
						System.err.println("[system] could not send message to a client");
						e.printStackTrace(System.err);
					}
					break;
				}
			}
		}
	}


	public void removeClient(Socket socket) {
		synchronized (this) {
			clients.remove(socket);
		}
	}
}

class ChatServerConnector extends Thread {
	private ChatServer server;
	private Socket socket;
	private String name;

	public ChatServerConnector(ChatServer server, Socket socket, String name) {
		this.server = server;
		this.socket = socket;
		this.name = name;
	}

	public void setCName(String ime) {
		this.name = ime;
	}

	public void run() {
		System.out.println("[system] connected with " + this.socket.getInetAddress().getHostName() + ":"+ this.socket.getPort() + ":" + this.name);

		DataInputStream in;
		try {
			in = new DataInputStream(this.socket.getInputStream()); // create input stream for listening for incoming messages
		} catch (IOException e) {
			System.err.println("[system] could not open input stream!");
			e.printStackTrace(System.err);
			this.server.removeClient(socket);
			return;
		}

		while (true) { // infinite loop in which this thread waits for incoming messages and processes them
			String msg_received;
			try {
				msg_received = in.readUTF(); // read the message from the client
			} catch (Exception e) {
				System.err.println("[system] there was a problem while reading message client on port " + this.socket.getPort() + ", removing client");
				e.printStackTrace(System.err);
				this.server.removeClient(this.socket);
				return;
			}
			String prva_crka = msg_received.substring(0, 1);
			try {
				if (prva_crka.equals("U")) {
					int indeks = 5;
					for (int i = 5; i < msg_received.length() - 4; i++) {
						if (msg_received.charAt(i) == ' ')
							break;
						else {
							indeks++;
						}
					}

					setCName(msg_received.substring(5, indeks).toUpperCase());
					synchronized(this){
						this.server.clients.putIfAbsent(this.socket,this.name );
					}
					System.out.println("[ " + this.name + "- " + this.socket.getPort() + " ]: " + msg_received);

				}

			} catch (Exception e) {
				System.out.println("Ne gre!!!");
			}

			try{
				if (msg_received.length() == 0) // invalid message
					continue;
				if(prva_crka.equals("O")){
					System.out.println("[ " + this.name + "- " + this.socket.getPort() + " ]: "+ msg_received.substring(18+this.name.length()));
				} else if(prva_crka.equals("P")){
					String dolPos = msg_received.substring(19+this.name.length(),20 + this.name.length());
					System.out.println("[ " + this.name + "- " + this.socket.getPort() + " ]: " + msg_received.substring(21 + this.name.length() + Integer.parseInt(dolPos) ));
				}
			}
			catch(Exception e){
				System.out.println("[ " + this.name + "- " + this.socket.getPort() + " ]: (prislo je do napake pri desifriranju)" + msg_received); // print the message to console
			}

			String msg_send = msg_received.toUpperCase(); // TODO

			try {
				if(prva_crka.equals("P")){
					this.server.sendToOneClient(msg_send);
				}
				else 
				this.server.sendToAllClients(msg_send); // send message to all clients
			} catch (Exception e) {
				System.err.println("[system] there was a problem while sending the message to all clients");
				e.printStackTrace(System.err);
				continue;
			}
		}
	}
}
