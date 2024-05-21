import java.io.*;
import java.net.*;
import java.time.*;
import javax.net.ssl.*;
import java.security.*;

public class ChatClient extends Thread {
	protected int serverPort = 1234;
	protected String nameOfUser;
	protected String mode;

	public static void main(String[] args) throws Exception {
		new ChatClient();
	}

	public void nastaviMode(String line){
		
		mode = line;
		
		if(mode.length()>1){
			mode = mode.substring(0,1);
		}
	}

	public ChatClient() throws Exception {
		
		DataInputStream in = null;
		DataOutputStream out = null;
		SSLSocket socket = null;
		// vzamemo ime uporabnika
		BufferedReader std_in = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Prosim vpisite svoje ime: ");
		nameOfUser = std_in.readLine();

		String passphrase = "rkpwd1";
		try {
			// preberi datoteko s strežnikovim certifikatom
			KeyStore serverKeyStore = KeyStore.getInstance("JKS");
			serverKeyStore.load(new FileInputStream("server.public"), "public".toCharArray());

			// preberi datoteko s svojim certifikatom in tajnim ključem
			KeyStore clientKeyStore = KeyStore.getInstance("JKS");
			clientKeyStore.load(new FileInputStream(nameOfUser+".private"), passphrase.toCharArray());

			// vzpostavi SSL kontekst (komu zaupamo, kakšni so moji tajni ključi in certifikati)
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(serverKeyStore);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(clientKeyStore, passphrase.toCharArray());
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), (new SecureRandom()));
			System.out.println("[system] connecting to chat server ...");
			// kreiramo socket
			SSLSocketFactory sf = sslContext.getSocketFactory();
			socket = (SSLSocket) sf.createSocket("localhost", serverPort);
			socket.setEnabledCipherSuites(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256" }); // dovoljeni nacin kriptiranja (CipherSuite)
			socket.startHandshake(); // eksplicitno sprozi SSL Handshake
			
			// connect to the chat server
			in = new DataInputStream(socket.getInputStream()); // create input stream for listening for incoming messages
			out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages
			System.out.println("[system] connected");

			// sporocilo novega uporabnikia
			this.sendMessage("User connected to the chat.", out);

			ChatClientMessageReceiver message_receiver = new ChatClientMessageReceiver(in); // create a separate thread for listening to messages from the chat server
			message_receiver.start(); // run the new thread
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// read from STDIN and send messages to the chat server
		System.out.println("After each message select message mode (P- private, O-public) ");
		String line = std_in.readLine();
		nastaviMode(line);

		String reciever = null;
		if (mode.equals("P")) {
			System.out.print("Ime uporabnika:");
			reciever = std_in.readLine();
		}
		String userInput;

		while ((userInput = std_in.readLine()) != null) { // read a line from the console

			LocalDate d = LocalDate.now();
			LocalTime t = LocalTime.now();
			int h = t.getHour();
			String m = String.valueOf(t.getMinute());
			if (t.getMinute() < 10) {
				m = "0" + m;
			}

			
			if (mode.equals("P")) {
				int lengthOfReciever = reciever.length();

				String segment = mode + d + " " + h + ":" + m + ":" + lengthOfReciever + ":" + reciever + userInput;
				this.sendMessage(segment, out); // send the message to the chat server
			} else {
				String segment = mode + d + " " + h + ":" + m + ":" + userInput;
				this.sendMessage(segment, out); // send the message to the chat server
			}
			line = std_in.readLine();
			nastaviMode(line);
			if (mode.equals("P")) {
				System.out.print("Ime uporabnika:");
				reciever = std_in.readLine();
			}

		}

		// cleanup
		out.close();
		in.close();
		std_in.close();
		socket.close();
	}

	private void sendMessage(String message, DataOutputStream out) {
		try {
			out.writeUTF(message); // send the message to the chat server
			out.flush(); // ensure the message has been sent
		} catch (IOException e) {
			System.err.println("[system] could not send message");
			e.printStackTrace(System.err);
		}
	}

	public String posljiIme() {
		// System.out.println(this.nameOfUser);
		return this.nameOfUser;
	}
}

// wait for messages from the chat server and print the out
class ChatClientMessageReceiver extends Thread {
	private DataInputStream in;

	public ChatClientMessageReceiver(DataInputStream in) {
		this.in = in;
	}

	public void run() {
		try {
			String message;
			while ((message = this.in.readUTF()) != null) { // read new message
				System.out.println(message);
				try {
					if (message.substring(0, 1).equals("U")) {

						System.out.println("[RKchat] " + message);
						continue;
					} else {

						String dolzinaPos = message.substring(1, 2);
						String datum = message.substring(2 + Integer.parseInt(dolzinaPos), 18 + Integer.parseInt(dolzinaPos));
						String posiljatelj = message.substring(2, 2 + Integer.parseInt(dolzinaPos));
						String sporocilo = null;
						
						if (message.substring(0, 1).equals("P")) {
							String dolzPrej = message.substring(19 + Integer.parseInt(dolzinaPos), 20 + Integer.parseInt(dolzinaPos));						
							sporocilo = message.substring(21 + Integer.parseInt(dolzinaPos) + Integer.parseInt(dolzPrej));
						} else {
							sporocilo = message.substring(18 + Integer.parseInt(dolzinaPos));
						}
						System.out.println("[RKchat " + datum + " ] " + posiljatelj + " said: " + sporocilo);
						// System.out.println("Can't decript:" + message);
					}
				} catch (Exception e) {
					System.out.println("Can't decript:" + message);
				}
			}

		} catch (Exception e) {
			System.err.println("[system] could not read message");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
