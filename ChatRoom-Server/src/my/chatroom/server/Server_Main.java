package my.chatroom.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import my.chatroom.data.messages.*;
import my.chatroom.server.exception.*;
import my.chatroom.server.users.*;

/**
 * Main server program of ChatRoom server,
 * loaded by calling constructor, shutdown by calling {@code serverShutdown()} method.
 * 
 * @author Jiale Hu
 * @version 1.0
 * @since 05/05/2020
 */

public class Server_Main implements Runnable
{

// Instance Variables
	// Service
	private final Server_DB dbServer;
	private final ServerSocket ss;
	private final ExecutorService threadPool;
	
	// Data
	private final ArrayList<Integer> offlineUsers; // [user_id]
	private final ConcurrentHashMap<Integer, ObjectOutputStream> onlineUsers; // {user_id=oos}
	private final ConcurrentHashMap<Integer, Queue<Message>> savedMessages; // {recipient=[messages]}
	
// Constructor
	/**
	 * Constructs main server program, loads required services: Server_DB, ServerSocket, Thread Pool.
	 * @param socketPort four-digit socket port number
	 * @throws FatalDataBaseException if Server_DB encounters fatal error when loading
	 */
	public Server_Main(String DBpath, int socketPort) throws FatalDataBaseException
	{
		// Load Server_DB
		System.out.println("Server_Main: Loading Server_DB ...");
		this.dbServer = new Server_DB(DBpath);
		
		// Load Users Lists from DB
		System.out.println("Server_Main: Loading Users ...");
		onlineUsers = new ConcurrentHashMap<Integer, ObjectOutputStream>();
		offlineUsers = new ArrayList<Integer>(Arrays.asList(dbServer.getIDs()));
		System.out.println("Server_Main: Offline & Online Users: [ID]");
		System.out.println(offlineUsers);
		System.out.println(onlineUsers.keySet());
		
		// Load Saved Messages from DB
		System.out.println("Server_Main: Loading Saved Messages ...");
		savedMessages = dbServer.getSavedMessages(); // Pointed to savedMessages in Server_DB
		System.out.println("Server_Main: Saved Messages for Each User: {ID=[MsgQueue]}");
		System.out.println(savedMessages);
		
		// Load Socket
		try 
		{
			ss = new ServerSocket(socketPort);
			System.out.println("Server_Main: ServerSocket is set at " 
					+ InetAddress.getLocalHost().getHostAddress() 
					+ " on port " + ss.getLocalPort());
		} 
		catch(Exception e)
		{
			throw new IllegalArgumentException("Port " + Integer.toString(socketPort) + 
					" is NOT available to accept connections");
		}
		
		// Load ThreadPool
		int numThreads = Runtime.getRuntime().availableProcessors() * (100);
		threadPool = Executors.newFixedThreadPool(numThreads);
		System.out.println("Server_Main: Thread Pool Loaded with " + numThreads + " Threads");
		
		// Start thread for each client
		threadPool.execute(this);
		
		System.out.println("-----Server_Main Loaded-----\n");
		System.out.println("Ready for Connections ... (Press Enter Key in CMD Window to Shutdown)\n");
	}

// serverShutdown() API
	/**
	 * Server shutdown API for server loader to call.
	 * @param waitTime number of seconds to wait, if {@code waitTime <= 0} proceed force shutdown
	 * @return {@code true} if shutdown finished normally
	 */
	public boolean serverShutdown(int waitTime)
	{
		System.out.println("-----Server_Main Shutting Down-----");
		// Force Shutdown
		if (waitTime <= 0)
		{
			System.out.println("Server_Main: Force Shutting Down");
			System.exit(0);
		}
		
		// Normal Shutdown
		System.out.println("Server_Main: Shutting Down with Waiting Time of: " + waitTime + " seconds");
		threadPool.shutdown(); // Disable new tasks from being submitted
		// Cut all connected sessions
		try
		{
			// Stop all oos
			for (ObjectOutputStream oos : onlineUsers.values()) oos.close();
			// Stop ServerSocket
			ss.close();
		} catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		// Cleanup before shutdown (Blocking)
		System.out.println("Server_Main: Cleaning Up ...");
		if (!shutdownCleanup()) return false;
		// Wait for thread terminations
		List<Runnable> taskList = null;
		try {
			// Wait for existing tasks to terminate
			if (!threadPool.awaitTermination(waitTime, TimeUnit.SECONDS))
			{
				taskList = threadPool.shutdownNow(); // Cancel tasks
				// Wait for tasks to respond to being cancelled
				if (!threadPool.awaitTermination(waitTime, TimeUnit.SECONDS)) return false;
			} 
		} catch (InterruptedException ie)
		{
			// (Re-)Cancel if current thread also interrupted
			taskList = threadPool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
		System.out.print("Server_Main: Halted Tasks: ");
		System.out.println(taskList);
		return true;
	}
	
// shutdownCleanup()
	/**
	 * Wraps up right before normal server shutdown invoked by calling {@code serverShutdown(int waitTime)}.
	 * Save savedMessages to DB. If failed, try saving "savedMessages.ser".
	 * @return {@code true} if wrap up is successful
	 */
	private boolean shutdownCleanup()
	{
		// Try save to DB
		if (dbServer.saveAllMessages()) return true;
		// Save backup instead as a .ser file
		System.err.println("Server_Main: - Failed to Save to DB, Saving as \"savedMessages.ser\" Instead.");
		try
		{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("savedMessages.ser"));
			oos.writeObject(savedMessages);
			oos.close();
		} catch (IOException e)
		{
			System.err.println("Server_Main: - Failed to Save Back Up File as \"savedMessages.ser\".");
			e.printStackTrace();
		}
		System.out.println("Server_Main: \"savedMessages.ser\" Saved.");
		return false;
	}

// run()
	/**
	 * {@code run()} method for each thread. (One user per thread)
	 * Accepts input stream from clients, initiates new thread when current thread has been assigned to a user.
	 * Life cycle of a thread for a user: 
	 * 	- check incoming object type
	 * 	- check {@code Message} type
	 * 	- log user in or add new user depending on {@code Message} type
	 * 	- leaving or re-joining processing if caught exception while in chat processing
	 * 	- thread terminates when user leave or unexpected exceptions are caught
	 */
	@Override
	public void run()
	{
		// Stack Variables
		Socket			   	s				= null;
		String				clientAddress	= null;
		ObjectInputStream	ois				= null;
		ObjectOutputStream	oos				= null;
		Object				joinMsg			= null;
		int				    user_id			= 0;
		
		// Connect to client
		try {
			s = ss.accept();
			clientAddress = s.getInetAddress().getHostAddress();
			System.out.println("Server_Main: New client connecting from " + clientAddress);
			ois = new ObjectInputStream(s.getInputStream());  
			joinMsg = ois.readObject();    
			oos = new ObjectOutputStream(s.getOutputStream());
		} catch(Exception e) 
		{
			System.out.println("Server_Main: - Connect failed from " + clientAddress + " " + e.toString());
			return;
		} finally // Initiate thread for new client
		{
			try {
				threadPool.execute(this);
			} catch(RejectedExecutionException e)
			{
				System.out.println("Server_Main: - New Thread Rejected by Thread Pool " + e.toString());
				return;
			}
		}
		
		// Joining
		try
		{
			// Keep check join message until joined or added
			while (true)
			{
				MsgType msgType = checkMsgType(joinMsg);
				user_id = ((Message) joinMsg).getUser_id();
				System.out.println("Server_Main: JoinMsg: " + ((Message) joinMsg));
				switch (msgType)
				{
				case JOIN:
					if (join((Message) joinMsg, oos))
					{
						try
						{
							chat(user_id, ois, oos);
							System.out.println("Server_Main: Connection Terminated by Server."); // return from chat()
						} catch (Exception e)
						{
							System.out.println("Server_Main: Connection Terminated: " + e.toString());
						} finally
						{
							// Leaving
							oos.close();
							leave(user_id, oos);
							// Session terminated
						}
						return;
					} else
					{
						System.out.println("Server_Main: - LogIn failed from " + clientAddress);
					}
					break;
				case ADD_USER:
					if (user_id > 0)
					{
						System.out.println("Server_Main: - Wrong first message enum type: ADD_USER from " 
											+ clientAddress + " Session terminated");
						oos.close();
						return;
					}
					String msg = ((Message) joinMsg).getMsg();
					int splitOffset = msg.indexOf("|");
					if (splitOffset < 0 || splitOffset >= 50)
					{
						// Invalid nick_name password format
						try
						{
							oos.writeObject(new Message(MsgType.REFUSE, null));
						} catch (IOException e)
						{
							e.printStackTrace();
						}
						System.out.println("Server_Main: - Invalid Nick Name from " + clientAddress);
						break;
					} else if (msg.length()-splitOffset < 6 || msg.length()-splitOffset >= 50)
					{
						// Invalid password
						System.out.println("Server_Main: - Invalid Password from " + clientAddress);
						try
						{
							oos.writeObject(new Message(MsgType.REFUSE, null));
						} catch (IOException e)
						{
							e.printStackTrace();
						}
						break;
					}
					String nick_name = msg.substring(0,splitOffset);
					String password = msg.substring(splitOffset+1);
					user_id = addUser(nick_name, password);
					if (user_id < 0)
					{
						System.out.println("Server_Main: - Create user failed for " + clientAddress);
						try
						{
							oos.writeObject(new Message(MsgType.REFUSE, null));
						} catch (IOException e)
						{
							e.printStackTrace();
						}
						break;
					}
					// Update Users Lists
					offlineUsers.add(user_id);
					sendUserList();
					System.out.println("Server_Main: Offline & Online Users: [ID]");
					System.out.println(offlineUsers);
					System.out.println(onlineUsers.keySet());
					try
					{
						oos.writeObject(new Message(null, user_id, MsgType.DONE));
					} catch (IOException e)
					{
						e.printStackTrace();
					}
					System.out.println("Server_Main: Add User Succeed for: " + user_id + " from " + clientAddress);
					break;
				default:
					System.out.println("Server_Main: - Unexpected message enum type: " + msgType + " from " 
										+ clientAddress + " Session terminated");
					oos.close();
					return;
				}
				// Wait for join message again
				joinMsg = ois.readObject();
			}
		} catch (MessageTypeException e)
		{
			System.out.println("Server_Main: - Wrong message type from " + clientAddress + " Session terminated");
			return;
		} catch (Exception e)
		{
			System.out.println("Server_Main: - Join terminated from " + clientAddress + " " + e.toString());
			return;
		}
	}
	
// Join Processing
	/**
	 * Does joining processing for existing users, including id check, password check, user list update.
	 * @param joinMsg joining {@code Message}
	 * @param oos {@code ObjectOutputStream} of this user
	 * @return {@code true} if join succeed
	 */
	private boolean join(Message joinMsg, ObjectOutputStream oos)
	{
		int user_id = joinMsg.getUser_id();
		if (onlineUsers.containsKey(user_id))
		{
			// Re-Join
			String password = joinMsg.getMsg();
			if (!dbServer.checkPassword(user_id, password))
			{
				// Invalid Password
				System.out.println("Server_Main: Invalid password from: " + user_id);
				try
				{
					oos.writeObject(new Message(MsgType.REFUSE, null));
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				return false;
			} else
			{
				// Close previous oos
				ObjectOutputStream pre_oos = onlineUsers.get(user_id);
				onlineUsers.replace(user_id, oos);
				try
				{
					pre_oos.writeObject(new Message(MsgType.LOGOUT, "ReJoin"));
					pre_oos.close();
					oos.writeObject(new Message(MsgType.DONE, dbServer.getNickName(user_id)));
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				// Update this oos
				sendUserList();
				System.out.println("Server_Main: Re-Join succeed: " + user_id);
			}
		} else
		{
			// Join
			if (dbServer.isExist(user_id))
			{
				String password = joinMsg.getMsg();
				if (dbServer.checkPassword(user_id, password))
				{
					try
					{
						oos.writeObject(new Message(MsgType.DONE, dbServer.getNickName(user_id)));
					} catch (IOException e)
					{
						e.printStackTrace();
					}
				} else
				{
					// Invalid Password
					System.out.println("Server_Main: - Invalid password from: " + user_id);
					try
					{
						oos.writeObject(new Message(MsgType.REFUSE, null));
					} catch (IOException e)
					{
						e.printStackTrace();
					}
					return false;
				}
				// Update Users Lists
				offlineUsers.remove((Integer) user_id);
				onlineUsers.put(user_id, oos);
				System.out.println("Server_Main: Offline & Online Users: [ID]");
				System.out.println(offlineUsers);
				System.out.println(onlineUsers.keySet());
				sendUserList();
				System.out.println("Server_Main: Join succeed: " + user_id);
			} else
			{
				// Invalid User ID
				System.out.println("Server_Main: - Invalid user ID: " + user_id);
				try
				{
					oos.writeObject(new Message(MsgType.REFUSE, null));
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				return false;
			}
		}
		
		// Send Saved Messages when client is offline
		if (!savedMessages.containsKey(user_id)) savedMessages.put(user_id, (Queue<Message>) new LinkedList<Message>());
		Queue<Message> queue = savedMessages.get(user_id);
		if (queue == null) System.err.println("Server_Main: - Saved Msg Queue Unfound for: " + user_id);
		while (!queue.isEmpty())
		{
			try
			{
				oos.writeObject(queue.poll());
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		System.out.println("Server_Main: Saved Messages (if any) sent to: " + user_id);
		return true;
	}
	
// Chat Processing
	/**
	 * Does chat processing for a online user, including send/receive message, user info request, user removal, user settings.
	 * @param user_id user id of this user
	 * @param ois {@code ObjectInputStream} of this user
	 * @param oos {@code ObjectOutputStream} of this user
	 * @throws Exception {@code ObjectInputStream} and {@code ObjectOutputStream} Exceptions for caller to handle
	 */
	private void chat(int user_id, ObjectInputStream ois, ObjectOutputStream oos) throws Exception
	{
		// Keep Receiving
		while (true)
		{
			// Significant Blocking
			Object msg = ois.readObject();
			try
			{
				MsgType msgType = checkMsgType(msg);
				
				switch(msgType)
				{
				case MESSAGE:
					int[] recipients = ((Message) msg).getRecipients();
					if (recipients == null)
					{
						sendToAll((Message) msg);
					} else
					{
						sendTo((Message) msg, user_id, recipients);
					}
					break;
				case USER_INFO:
					int forUser = ((Message) msg).getUser_id();
					String info = dbServer.getUserInfo(forUser);
					oos.writeObject(new Message(info, forUser, MsgType.USER_INFO));
					break;
				case RM_USER:
					if (dbServer.removeUser(user_id))
					{
						oos.writeObject(new Message(MsgType.DONE, null));
						oos.writeObject(new Message(MsgType.LOGOUT, "User Deleted"));
						savedMessages.remove(user_id);
						onlineUsers.remove(user_id);
						sendUserList();
						return;
					} else 
					{
						System.out.println("Server_Main: - Remove user failed for " + user_id);
						oos.writeObject(new Message(MsgType.REFUSE, null));
					}
					break;
				case SET_NICKNAME:
					String newName = ((Message) msg).getMsg();
					if (dbServer.setNickName(user_id, newName))
					{
						oos.writeObject(new Message(MsgType.DONE, null));
						sendUserList();
					} else 
					{
						System.out.println("Server_Main: - Set NickName failed for " + user_id);
						oos.writeObject(new Message(MsgType.REFUSE, null));
					}
					break;
				case SET_PASSWORD:
					String passwords = ((Message) msg).getMsg();
					int blankOffset = passwords.indexOf(" ");
					if (blankOffset < 0)
					{
						oos.writeObject(new Message(MsgType.REFUSE, "Wrong Format"));
						break;
					}
					String oldPassword = passwords.substring(0, blankOffset);
					String newPassword = passwords.substring(blankOffset + 1);
					if (dbServer.setPassword(user_id, oldPassword, newPassword))
					{
						oos.writeObject(new Message(MsgType.DONE, null));
						oos.writeObject(new Message(MsgType.LOGOUT, "ReJoin"));
						return;
					} else 
					{
						// Invalid password
						System.out.println("Server_Main: Invalid password from: " + user_id);
						oos.writeObject(new Message(MsgType.REFUSE, "Wrong Old Password"));
					}
					break;
				default:
					System.out.println("Server_Main: - Wrong message enum type from:" + user_id + "Session continued");
				}
			} catch(MessageTypeException e)
			{
				System.out.println("Server_Main: - Wrong message type from: " + user_id + "Session continued");
			} catch(NullPointerException e)
			{
				e.printStackTrace();
			}
		}
	}
	
// Leave Processing
	/**
	 * Does leaving processing when a user leaves, including re-joining processing and user list updating.
	 * @param user_id user id
	 * @param oos current {@code ObjectOutputStream} for this user
	 */
	private void leave(int user_id, ObjectOutputStream oos)
	{
		// Re-Join Situation
		if (onlineUsers.get(user_id) != oos)
		{
			System.out.println("Server_Main: Re-joining, previous session for " + user_id + " terminated");
			return;
		}
		// Update Users Lists
		if (onlineUsers.containsKey(user_id)) offlineUsers.add(user_id);
		onlineUsers.remove(user_id);
		System.out.println("Server_Main: Offline & Online Users: [ID]");
		System.out.println(offlineUsers);
		System.out.println(onlineUsers.keySet());
		sendUserList();
		System.out.println("Server_Main: Leave succeed: " + user_id);
	}

// Check Message Type
	/**
	 * Check incoming {@code Object} type, return {@code Message} type if it is in this type.
	 * @param msg {@code Object} to be checked
	 * @return same object in {@code Message} type
	 * @throws MessageTypeException if incoming {@code Object} is not in {@code Message} type
	 */
	private MsgType checkMsgType(Object msg) throws MessageTypeException
	{
		if (msg instanceof Message)
		{
			return ((Message) msg).getMsgType();
		} else
		{
			throw new MessageTypeException("Not in Type Message");
		}
	}
	
// Add User
	/**
	 * Add a new user to server and database.
	 * @param nick_name nick name of user
	 * @param password password for login
	 * @return new user id, or -1 if failed adding user
	 */
	private int addUser(String nick_name, String password)
	{
		User user = dbServer.addUser(nick_name, password);
		if (user == null) 
		{
			System.out.println("Server_Main: - Error creating new user.");
			return -1;
		}
		int user_id = user.getUser_id();
		savedMessages.put(user_id, (Queue<Message>) new LinkedList<Message>());
		dbServer.saveAllMessages();
		return user_id;
	}
// Send User List
	/**
	 * Send current user list (online and offline) to all online users.
	 */
	private synchronized void sendUserList()
	{
		// <Integer,String>
		HashMap<Integer, String> online = new HashMap<Integer, String>();
		HashMap<Integer, String> offline = new HashMap<Integer, String>();
		for (int id : offlineUsers)
		{
			offline.put(id, dbServer.getNickName(id));
		}
		for (int id : onlineUsers.keySet())
		{
			online.put(id, dbServer.getNickName(id));
		}
		sendToAll(new Message(online, offline));
		System.out.println("Server_Main: User List Sent.");
	}
	
// Send To All
	/**
	 * Send/save {@code Message} to all recipient(s),
	 * only {@code Message} of {@code MsgType.MESSAGE} are saved.
	 * @param msg {@code Message} to be sent/saved
	 */
	private synchronized void sendToAll(Message msg)
	{
		// Send to online users
		ObjectOutputStream[] oosArray = onlineUsers.values().toArray(new ObjectOutputStream[0]);
		for (ObjectOutputStream oos : oosArray)
		{
			try
			{
				oos.writeObject(msg);
			}
			catch (IOException e) {} // do nothing if send error
			// ois.readObject() will take care of errors
		}
		
		// Save Only messages for offline users
		if (msg.getMsgType() != MsgType.MESSAGE) return;
		System.out.println("Server_Main: Saving Messages for Each User: [ID]");
		System.out.print("[");
		for (int id : offlineUsers)
		{
			savedMessages.get(id).add(msg);
			System.out.print(id + " ");
		}
		System.out.println("] Saved");
//		System.out.println("Server_Main: Saved Messages for Each User: {ID=[MsgQueue]}");
//		System.out.println(savedMessages);
	}
	
// Send To
	/**
	 * Send/save {@code Message} to specified recipient(s). Always copy message to sender.
	 * @param msg {@code Message} to be sent/saved
	 * @param recipient_id an array of recipient id
	 */
	private synchronized void sendTo(Message msg, int user_id, int[] recipient_id)
	{
		// Copy message to sender
		ObjectOutputStream oos = onlineUsers.get(user_id);
		try
		{
			oos.writeObject(msg);
		}
		catch (IOException e) {}
		// Send to recipients
		for (int id : recipient_id)
		{	
			if (id == user_id) continue; // Send message to sender separately
			if (onlineUsers.containsKey(id))
			{
				oos = onlineUsers.get(id);
				try
				{
					oos.writeObject(msg);
				}
				catch (IOException e) {} // do nothing if send error
				// ois.readObject() will take care of errors
			} else if (offlineUsers.contains(id))
			{
				savedMessages.get(id).add(msg);
			} else
			{
				System.err.println("Server_Main: - recipient " + id + " is not a user.");
			}
		}
		System.out.println("Server_Main: Saved Messages for Each User: {ID=[MsgQueue]}");
		System.out.println(savedMessages);
	}

// main() Loader
	/**
	 * Temporary loader of server programs.
	 * @param args is ignored
	 */
	public static void main(String[] args)
	{
		System.out.println("-----" + new Date() + "-----\n");
		
		if (args.length != 0)
		{
			System.out.println("Main Loader: Cmd Line Argument(s) Ignored");
		}
		try
		{
			System.out.println("Main Loader: Loading Server_Main ...");
			Server_Main server = new Server_Main(new File("").getAbsolutePath().concat("/database/QuoteDB"), 1111);
			
			// Assign lower priority to Main thread (Checking shutdown request only)
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			// Wait for CMD input for shutting down (Press Enter Key)
			InputStreamReader isr = new InputStreamReader(System.in);
			BufferedReader keyboard = new BufferedReader(isr);
			keyboard.readLine();
			System.out.println("Main Loader: is Shutdown Normally: " + server.serverShutdown(1)); // Wait for 1 second in shutdown
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("-----" + new Date() + "-----");
	}

}
