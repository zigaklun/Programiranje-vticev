import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

	protected int serverPort = 1234;
	//protected List<Socket> clients = new ArrayList<Socket>(); // list of clients

	protected Map<Socket,String> clients = new HashMap<Socket,String>();



	public static void main(String[] args) throws Exception {
		new ChatServer();
	}

	public ChatServer() {
		ServerSocket serverSocket = null;
		// create socket
		try {
			serverSocket = new ServerSocket(this.serverPort); // create the ServerSocket
		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		try {
			while (true) {
				Socket newClientSocket = serverSocket.accept(); // wait for a new client connection
				String newClientName = null;
				
				synchronized(this) {
					clients.put(newClientSocket,newClientName); // add client to the list of clients
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
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	// send a message to all clients connected to the server
	public void sendToAllClients(String message) throws Exception {
		
		Set <Socket> allClientSockets = clients.keySet();
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

	public void removeClient(Socket socket) {
		synchronized(this) {
			clients.remove(socket);
		}
	}
}

class ChatServerConnector extends Thread {
	private ChatServer server;
	private Socket socket;
	private String name;
	private boolean firstMessage=true;

	public ChatServerConnector(ChatServer server, Socket socket, String name) {
		this.server = server;
		this.socket = socket;
		this.name = name;
	}

	public String getCName(){
		return this.name;
	}
	public void setCName(String ime){
		//System.out.println(ime);
		this.name = ime;
		System.out.println(this.name);
	}

	public void run() {
		System.out.println("[system] connected with " + this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort() + ":"+ this.name);

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
			try{
				if(firstMessage && msg_received.substring(0,4).equals("Chat ")){
					firstMessage = false;
					int indeks = 5;
					for(int i = 5; i< msg_received.length()-4;i++){
						if(msg_received.charAt(i)==' ') break;
						else indeks ++;
					}
					
					setCName(msg_received.substring(5, indeks));
				}
			}catch(Exception e){
				firstMessage = false;
			}



			if (msg_received.length() == 0) // invalid message
				continue;

			System.out.println( "[ " + this.name +"- "+ this.socket.getPort() + " ]: " + msg_received); // print the incoming message in the console

			String msg_send = msg_received.toUpperCase(); // TODO

			try {
				this.server.sendToAllClients(msg_send); // send message to all clients
			} catch (Exception e) {
				System.err.println("[system] there was a problem while sending the message to all clients");
				e.printStackTrace(System.err);
				continue;
			}
		}
	}
}
