
package tcpserver;

import java.net.*;
import java.util.*;
import java.io.*;

public class ChatServerThread extends Thread {
	private TCPServer server = null;
	ArrayList<String> store_history = new ArrayList<String>();
	ArrayList<String> store_friendreq = new ArrayList<String>();
	ArrayList<String> store_friend_list = new ArrayList<String>();
	ArrayList<String> privatechatport = new ArrayList<String>();
	ArrayList<String> passuser = new ArrayList<String>();
	ArrayList<String> offline_msg= new ArrayList<String>();
	private Socket socket = null;
	private int ID = -1;
	int j = 0;
	int i=0;
	int count=0;
	private DataInputStream streamIn = null;
	private DataOutputStream streamOut = null;
	private String password=null;
	private String us=null;
	private int login_status=0;

	public ChatServerThread(TCPServer _server, Socket _socket) {
		super();
		server = _server;

		socket = _socket;
		ID = socket.getPort();

	}

	public void send(String msg) // client ke kono msg pathanor function server
									// theke
	{
		try {
			streamOut.writeUTF(msg);
			streamOut.flush();
		} catch (IOException ioe) {
			System.out.println(ID + " ERROR sending: " + ioe.getMessage());
			server.remove(ID);
			stop();
		}
	}

	public synchronized void history(String msg) {

		if (!(msg.equals(".show"))) {
			store_history.add(msg);
		}

	}

	public synchronized void friend_request(String ID) {

		store_friendreq.add(ID);
	}

	public synchronized void friend_request_remove(String ID) {

		store_friendreq.remove(ID);
	}

	public synchronized String return_friend_requestID() {

		return store_friendreq.toString();
	}

	public synchronized void friend_list(String ID) {

		store_friend_list.add(ID);
	}

	public synchronized String return_friend_list() {

		return store_friend_list.toString();
	}
	public synchronized String return_history() {
		// store_history.trimToSize();
		// String res = String.join("-->\n", store_history);
		// return res;
		// String res = Arrays.toString(store_history.toArray());
		// return res;
		return store_history.toString();
	}
	public synchronized String password_generator() {

		 String pw = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
	        StringBuilder str = new StringBuilder();
	        Random rnd = new Random();
	        while (str.length() < 5) {
	            int index = (int) (rnd.nextFloat() * pw.length());
	            str.append(pw.charAt(index));
	        }
	        password = str.toString();
	        return password;
	}
	public synchronized String return_passoword() {

		return password;
	}
	public synchronized void username(String username) {
		us=username;
	}
	public synchronized String return_username() {
		return us;
	}
	public synchronized void login_status_on() {
		login_status=1;
	}
	public synchronized void login_status_off() {
		login_status=0;
	}
	public synchronized int return_login_status() {
		return login_status;
	}
	public synchronized void store_offline_msg(String input) {
		offline_msg.add(input);
	}
	public synchronized String return_offline_msg() {
		
		Collections.reverse(offline_msg);
		return offline_msg.toString();
	}
	public synchronized void null_offline_msg() {
		//privatechatport = new int[privatechatport.length];
		//offline_msg=new String [offline_msg.length];
	}

	/*public synchronized void private_port_chatgroup(String ID) {

		privatechatport.add(ID);
	}
	
	public synchronized String return_private_port_chatgroup() {

		return privatechatport.toString();
	}*/
	

	

	public int getID() // client er id return kore
	{
		return ID;
	}

	public void run() {
		System.out.println("Server Thread " + ID + " running.");
		System.out.println("To kick any client, type- kick:portnumber ");
		while (true) {
			try {
				server.handle(ID, streamIn.readUTF());  // goes to server's
														// handle function
			} catch (IOException ioe) {
				System.out.println(ID + " ERROR reading: " + ioe.getMessage());
				server.remove(ID);
				stop();
			}
		}
	}

	public void open() throws IOException {
		streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

	}

	public void close() throws IOException {
		if (socket != null)
			socket.close();
		if (streamIn != null)
			streamIn.close();
		if (streamOut != null)
			streamOut.close();
	}
}