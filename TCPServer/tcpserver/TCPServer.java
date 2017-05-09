package tcpserver;

import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

public class TCPServer implements Runnable {
	private ChatServerThread[] clients = new ChatServerThread[50]; // store
																	// house for
																	// client
	public DataInputStream console = null;
	public String inputfromserver = null;
	private String receive_history = null;
	private String receive_friendreq = null;
	private String show_friend_list = null;
	private ServerSocket server = null;
	private Thread thread = null;
	private int clientCount = 0;
	private int unicast_flag = 0;
	private int unicast_port = 0;
	private int multicast_flag = 0;
	private int multicast_finish = 0;
	private int friendreq_flag = 0;
	private int friendreq_finish = 0;
	private int acceptfriendreq_flag = 0;
	private int privatechat = 0;
	private int portcount = 0;
	private String password = null;
	private int pw = 0;
	private int login = 0;

	ArrayList<Integer> multicast_array = new ArrayList<Integer>();
	ArrayList<Integer> friendreq_array = new ArrayList<Integer>();
	ArrayList<String> temp_array = new ArrayList<String>();
	int[] privatechatport = new int[50];
	String[] temp_offline=new String[50];

	public TCPServer(int port) {
		try {
			System.out.println("Binding to port " + port + ", please wait  ...");
			server = new ServerSocket(port);
			System.out.println("Server started: " + server);
			start();
		} catch (IOException ioe) {
			System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());
		}
	}

	@Override
	public void run() {
		while (thread != null) {
			try {
				System.out.println("Waiting for a client ...");
				addThread(server.accept());
			} // creates socket for each client
			catch (IOException ioe) {
				System.out.println("Server accept error: " + ioe);
				stop();
			}
		}
	}

	public void start() throws IOException {

		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}

		while (true) {
			console = new DataInputStream(System.in);
			inputfromserver = console.readLine();
			console = null;

			if (inputfromserver.contains("kick:") && inputfromserver.indexOf("kick:") == 0) {

				// only taking the numbers from inputfromserver

				for (int i = 5; i < inputfromserver.length(); i++) {
					if (inputfromserver.charAt(i) != 32
							&& (inputfromserver.charAt(i) > 47 && inputfromserver.charAt(i) < 58)) {
						temp_array.add(Character.toString(inputfromserver.charAt(i)));
					}
				}
				if (!temp_array.isEmpty()) {
					String temp = temp_array.toString();
					temp = temp.replace(",", "").replace("[", "").replace("]", "").replaceAll("\\s+", "");
					temp = temp.trim();
					// checking the validity of port numbers

					if (temp.equals(matchid(temp))) {
						System.out.println(temp);
						clients[findClient(Integer.parseInt(temp))].send(".bye");
						remove(Integer.parseInt(temp));
					} else {
						System.out.println("port number did not match. try again ");
					}

				} else {
					System.out.println("port address is not word, it is number. Type Properly. :-|");
				}
			} else {
				System.out.println("Wrong Format! Right is: kick:portnumber");
			}
			temp_array.clear();
		}

	}

	public void stop() {
		if (thread != null) {
			thread.stop();
			thread = null;
		}
	}

	private int findClient(int ID) // find the index number for an client
									// against its port number
	{

		for (int i = 0; i < clientCount; i++)
			if (clients[i].getID() == ID)
				return i;
		return -1;

	}

	private String matchid(String ID) {

		for (int i = 0; i < clientCount; i++)

			if (clients[i].getID() == Integer.parseInt(ID))
				return ID;
		return "-1";

	}

	public synchronized void handle(int ID, String input)

	{
		if (input.equals(".login") || input.equals(".logout")) {
			if (input.equals(".login")) {
				clients[findClient(ID)].send("PASSWORD: ");
				pw = 1;
			}
			else
			{
				clients[findClient(ID)].send("Logging Out ");
				pw=0;
				login=0;
				clients[findClient(ID)].login_status_off();
			}
		} else if (pw == 1 && login == 0) {
			if (input.equals(clients[findClient(ID)].return_passoword())
					&& Integer.toString(ID).equals(clients[findClient(ID)].return_username())) {
				login = 1;
				pw=0;
				clients[findClient(ID)].send("Login is Successful ");
				clients[findClient(ID)].login_status_on();
				clients[findClient(ID)].send(clients[findClient(ID)].return_offline_msg());
			
			} else {
				clients[findClient(ID)].send("Wrong. Try again: ");
			}

		} else if (login == 1 ) {
			if (privatechat == 0) {
				// store every msg
				clients[findClient(ID)].history(input);
			}
			// Remove ID
			if (unicast_flag == 0 && input.equals(".bye") && multicast_flag == 0 && privatechat == 0
					&& friendreq_flag == 0) {
				clients[findClient(ID)].send(".bye");
				remove(ID);
			}

			// show history
			else if (unicast_flag == 0 && input.equals(".show") && multicast_flag == 0 && privatechat == 0
					&& friendreq_flag == 0) {
				receive_history = clients[findClient(ID)].return_history();
				clients[findClient(ID)].send(receive_history);
				unicast_flag = 0;
			}
			// for multicast initialization
			else if (input.equals(".multicast") && unicast_flag == 0 && privatechat == 0 && friendreq_flag == 0) {
				multicast_flag = 1;
				clients[findClient(ID)].send("Input Port Numbers: After that. type .finish");
			}

			// for unicast initialization
			else if (input.equals(".unicast") && multicast_flag == 0 && privatechat == 0 && friendreq_flag == 0) {
				unicast_flag = 1;
				clients[findClient(ID)].send("Input Port Number: ");

			}
			// doing unicast
			else if (unicast_flag == 1 && unicast_port != 0) {
				clients[findClient(unicast_port)].send(ID + "sent:" + input);
				unicast_flag = 0;
				unicast_port = 0;
			}
			// doing multicasting
			else if (multicast_finish == 1 && multicast_flag == 1) {
				for (int i : multicast_array) {
					clients[findClient(i)].send(ID + "sent:" + input);
				}
				multicast_finish = 0;
				multicast_flag = 0;
				multicast_array.clear();

			}
			// for multicast: checking the validity of port number and storing
			// in
			// arraylist
			else if (multicast_flag == 1 && Pattern.matches("[0-9]+", input) == true) {
				if (input.equals(matchid(input))) {
					multicast_array.add(Integer.parseInt(input));
				} else {
					clients[findClient(ID)].send("Wrong One. Wanna exit from Multicast? Type: exit_mul ");
				}

			}

			// for unicast: checking the port whether it is valid or not
			else if (unicast_flag == 1 && input.contains("[a-zA-Z]") == false) {
				if (input.equals(matchid(input))) {
					unicast_port = Integer.parseInt(input);
					clients[findClient(ID)].send("Your SECRET msg: ");
				} else {
					clients[findClient(ID)].send("Wrong One. Wanna exit from Unicast? Type: exit_uni ");
				}
			}
			// exit from multicast
			else if (input.equals("exit_mul") && multicast_flag == 1 && multicast_finish == 0) {
				multicast_array.clear();
				multicast_flag = 0;

			}
			// exit from unicast
			else if (input.equals("exit_uni") && unicast_flag == 1) {
				unicast_flag = 0;
				clients[findClient(ID)].send("You can chat with others again. ");

			}
			// giving corresponding port number for friend request
			else if (input.equals(".req") && unicast_flag == 0 && multicast_flag == 0 && privatechat == 0) {
				friendreq_flag = 1;
				clients[findClient(ID)].send("Which friend your want to send friend request? Type his/her Port Number."
						+ "After That Type .finish ");

			}
			// checking validity of port numbers for friend request and store it
			else if (friendreq_flag == 1 && Pattern.matches("[0-9]+", input) == true) {
				if (input.equals(matchid(input))) {
					friendreq_array.add(Integer.parseInt(input));
					clients[findClient(Integer.parseInt(input))].friend_request(Integer.toString(ID));
				} else {
					clients[findClient(ID)].send("Wrong One. Wanna exit from friendrequest? Type: exit_req ");
				}
			}
			// exit from friend request
			else if (input.equals("exit_req") && friendreq_flag == 1 && friendreq_finish == 0) {
				friendreq_finish = 0;
				friendreq_flag = 0;
				friendreq_array.clear();
				clients[findClient(ID)].send("Closing friend request. You can chat with others: ");

			}
			// steps when input is .finish
			else if (input.equals(".finish") && privatechat == 0) {
				if (multicast_flag == 1 && multicast_finish == 0) {
					multicast_finish = 1;
					clients[findClient(ID)].send("Your SECRET msg: ");
				} else if (friendreq_flag == 1 && friendreq_finish == 0) {
					friendreq_finish = 1;
					acceptfriendreq_flag = 1;
					friendreq_flag = 0;
					clients[findClient(ID)]
							.send("your request is sent. " + "you wull be notified when they will accept you.");
					for (int i : friendreq_array) {
						clients[findClient(i)].send("Notification For Friend Request. To see, Type .showfriendreq");
					}
					friendreq_array.clear();
				} else {
					for (int i = 0; i < clientCount; i++) // sends to each
															// client
					{
						clients[i].send(ID + ": " + input);
						unicast_flag = 0;
						unicast_port = 0;
						multicast_finish = 0;
						multicast_flag = 0;
						friendreq_finish = 0;
						friendreq_flag = 0;
						friendreq_array.clear();
						multicast_array.clear();
					}
				}
			}
			// showing the id who sent friend request to a particular client
			else if (input.equals(".showfriendreq") && unicast_flag == 0 && multicast_flag == 0
					&& friendreq_flag == 0) {
				receive_friendreq = clients[findClient(ID)].return_friend_requestID();
				clients[findClient(ID)].send(
						receive_friendreq + "To accept or reject," + "type: accept:portnumber OR reject:portnumber");
			}
			// accept friend request or reject friend request
			else if (((input.contains("accept:") && input.indexOf("accept:") == 0)
					|| (input.contains("reject:") && input.indexOf("reject:") == 0)) && unicast_flag == 0
					&& multicast_flag == 0 && friendreq_flag == 0 && privatechat == 0) {
				// only taking the numbers from input
				for (int i = 7; i < input.length(); i++) {
					if (input.charAt(i) != 32 && (input.charAt(i) > 47 && input.charAt(i) < 58)) {
						temp_array.add(Character.toString(input.charAt(i)));
					}
				}
				// checking whether input contains any numbers or not
				if (!temp_array.isEmpty()) {
					String temp = temp_array.toString();
					temp = temp.replace(",", "").replace("[", "").replace("]", "").replaceAll("\\s+", "");
					temp = temp.trim();
					// clients[findClient(ID)].send(temp);
					String temp1 = matchid(temp);
					// checking the validity of port numbers
					if (temp1.equals(temp)) {
						if (input.contains("accept:") && input.indexOf("accept:") == 0) {
							clients[findClient(ID)].send("Accepting request. Done");
							clients[findClient(ID)].friend_list(temp);
							clients[findClient(Integer.parseInt(temp))].send(ID + " has accepted your friend request");
							clients[findClient(Integer.parseInt(temp))].friend_list(Integer.toString(ID));
						} else {
							clients[findClient(ID)].send("Rejecting request. Done");
							clients[findClient(ID)].friend_request_remove(temp);

						}
						temp_array.clear();
					} else {
						clients[findClient(ID)].send("Wrong port number. try again.");
						temp_array.clear();
					}

				} else { // broadcasting msg
					for (int i = 0; i < clientCount; i++) // sends to each
															// client
					{
						clients[i].send(ID + ": " + input);
						temp_array.clear();
					}

				}

			}
			// show respective friendlist
			else if (input.equals(".showfriendlist") && unicast_flag == 0 && multicast_flag == 0
					&& friendreq_flag == 0) {
				show_friend_list = clients[findClient(ID)].return_friend_list();
				clients[findClient(ID)].send(show_friend_list);
			}

			else if (input.equals(".privatechat") && unicast_flag == 0 && multicast_flag == 0 && friendreq_flag == 0
					&& privatechat == 0) {
				clients[findClient(ID)].send("Type the port numbers of friends whom you want add in private chat group "
						+ " Like Following\n" + "123:321");
				privatechat = 1;

			} else if (!input.equals(".exit_chat") && privatechat == 1 && portcount != 0) {
				for (int i = 0; i <= portcount; i++) {
					clients[findClient(privatechatport[i])].send(ID + ":" + input);
				}

			} else if (privatechat == 1 && Pattern.matches("[0-9:]+", input) == true && portcount == 0) {
				privatechatport[0] = ID;
				int i = 1;
				for (String s : input.split(":")) {
					if (!s.equals(Integer.toString(ID))) {
						if (s.equals(matchid(s))) {
							privatechatport[i] = Integer.parseInt(s.trim());
							i++;
							portcount++;
						} else {
							clients[findClient(ID)].send("Wrong Port number. It just does not exist. try again");
							privatechatport = new int[privatechatport.length];
						}
					}
				}
				System.out.println(portcount);
			} else if (input.equals(".exit_chat") && privatechat == 1) {
				portcount = 0;
				privatechat = 0;
				privatechatport = new int[privatechatport.length];
				clients[findClient(ID)].send("Exiting from privatechat");
			}

			// broadcasting msg
			else
				for (int i = 0; i < clientCount; i++) // sends to each client
				{
					if(clients[i].return_login_status()==1)
					{
					clients[i].send(ID + ": " + input);
					unicast_flag = 0;
					unicast_port = 0;
					multicast_finish = 0;
					multicast_flag = 0;
					friendreq_finish = 0;
					friendreq_flag = 0;
					privatechat = 0;
					portcount = 0;
					friendreq_array.clear();
					multicast_array.clear();
					temp_array.clear();
					privatechatport = new int[privatechatport.length];
					}
					else{
						clients[i].store_offline_msg(input);
					}
				}
		}
	}

	public synchronized void remove(int ID) {
		int pos = findClient(ID);
		if (pos >= 0) {
			ChatServerThread toTerminate = clients[pos];
			System.out.println("Removing client thread " + ID + " at " + pos);
			if (pos < clientCount - 1)
				for (int i = pos + 1; i < clientCount; i++)
					clients[i - 1] = clients[i];
			clientCount--;
			try {
				toTerminate.close();
			} catch (IOException ioe) {
				System.out.println("Error closing thread: " + ioe);
			}
			toTerminate.stop();
		}
	}

	private void addThread(Socket socket) {
		if (clientCount < clients.length) {
			System.out.println("Client accepted: " + socket);
			clients[clientCount] = new ChatServerThread(this, socket);
			try {
				clients[clientCount].open();
				clients[clientCount].start();
				password = clients[clientCount].password_generator();
				clients[clientCount].username(Integer.toString(clients[clientCount].getID()));
				clients[clientCount].send("your USERNAME is: " + clients[clientCount].return_username()
						+ " and passoword is:" + password);

				clientCount++;

			} catch (IOException ioe) {
				System.out.println("Error opening thread: " + ioe);
			}
		} else
			System.out.println("Client refused: maximum " + clients.length + " reached.");
	}

	public static void main(String args[]) throws IOException {
		TCPServer server = null;
		server = new TCPServer(2000);

	}
}