package my.chatroom.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import my.chatroom.data.server.*;
import my.chatroom.data.trans.*;
import my.chatroom.server.exception.*;

/**
 * {@summary }
 * 
 * @author Jiale Hu
 * @version 1.0
 * @since 05/05/2020
 *
 */

public class Server_Main implements Runnable
{

// Instance Variables
	private Server_DB dbServer;
	private ServerSocket ss;
	private LinkedList<Integer> offlineUsers;
	private ConcurrentHashMap<Integer, ObjectOutputStream> onlineUsers = new ConcurrentHashMap<Integer, ObjectOutputStream>();
	private ConcurrentHashMap<Integer, Queue<Message>> savedMessages;
	
// Constructor
	public Server_Main(int socketPort)
	{
		// Load DB Server
		System.out.println("Server_Main: Loading Server_DB ...");
		try
		{
			this.dbServer = new Server_DB();
		} catch (FetalDataBaseException e)
		{
			e.printStackTrace();
			return;
		}
		
		// Update Users Lists
		System.out.println("Server_Main: Loading Users ...");
		offlineUsers = new LinkedList<Integer>(Arrays.asList(dbServer.getIDs()));
		System.out.println("Server_Main: Offline & Online Users: [ID]");
		System.out.println(offlineUsers);
		System.out.println(onlineUsers.keySet());
		
		// Update Saved Messages
		System.out.println("Server_Main: Loading Saved Messages ... (UNIMPLEMENTED, loading new one)");
		savedMessages = new ConcurrentHashMap<Integer, Queue<Message>>();
		for (int id : offlineUsers)
		{
			savedMessages.put(id, (Queue<Message>) new LinkedList<Message>());
		}
		System.out.println("Server_Main: Saved Messages for Each User: {ID=[MsgQueue]}");
		System.out.println(savedMessages);
		
		// Load Socket
		try 
		{
			ss = new ServerSocket(socketPort);
			System.out.println("Server_Main: Server_Main is up at " 
					+ InetAddress.getLocalHost().getHostAddress() 
					+ " on port " + ss.getLocalPort());
		} 
		catch(Exception e)
		{
			throw new IllegalArgumentException("Port " + Integer.toString(socketPort) + 
					" is NOT available to accept connections");
		}
		
		// Start thread for each client
		new Thread(this).start();
		
		System.out.println("-----Server_Main Loaded-----\n");
	}

// run()
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
		try 
		{
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
		} finally
		{
			new Thread(this).start();
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
						} catch (Exception e)
						{
							System.out.println("Server_Main: Connection Terminated: " + e.toString());
							// Leaving
							leave(user_id, oos);
							return;
							// Session terminated
						}
					} else
					{
						System.out.println("Server_Main: - LogIn failed from " + clientAddress);
					}
					break;
				case ADD_USER:
					if (user_id != 0)
					{
						System.out.println("Server_Main: - Wrong first message enum type: ADD_USER from " + clientAddress + " Session terminated");
						return;
					}
					String msg = ((Message) joinMsg).getMsg();
					int blankOffset = msg.indexOf(" ");
					if (blankOffset < 0 || blankOffset >= 50)
					{
						// Invalid nick_name password format
						try
						{
							oos.writeObject(new Message(MsgType.REFUSE));
						} catch (IOException e)
						{
							e.printStackTrace();
						}
						System.out.println("Server_Main: - Invalid Nick Name from " + clientAddress);
						return;
					} else if (msg.length()-blankOffset < 6 || msg.length()-blankOffset >= 50)
					{
						// Invalid password
						System.out.println("Server_Main: - Invalid Password from " + clientAddress);
						try
						{
							oos.writeObject(new Message(MsgType.REFUSE));
						} catch (IOException e)
						{
							e.printStackTrace();
						}
						return;
					}
					String nick_name = msg.substring(0,blankOffset);
					String password = msg.substring(blankOffset+1);
					user_id = addUser(nick_name, password);
					if (user_id == 0)
					{
						System.out.println("Server_Main: - Create user failed for " + clientAddress);
						try
						{
							oos.writeObject(new Message(MsgType.REFUSE));
						} catch (IOException e)
						{
							e.printStackTrace();
						}
						return;
					}
					// Update Users Lists
					offlineUsers.add(user_id);
					System.out.println("Server_Main: Offline & Online Users: [ID]");
					System.out.println(offlineUsers);
					System.out.println(onlineUsers.keySet());
					try
					{
						oos.writeObject(new Message("", user_id, MsgType.DONE));
					} catch (IOException e)
					{
						e.printStackTrace();
					}
					System.out.println("Server_Main: Add User Succeed for: " + user_id + " from " + clientAddress);
					break;
				default:
					System.out.println("Server_Main: - Unexpected message enum type: " + msgType + " from " 
										+ clientAddress + " Session terminated");
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
			System.out.println("Server_Main: - Join failed from " + clientAddress + " " + e.toString());
			e.printStackTrace();
			return;
		}
	}
	
// Check Message Type
	private MsgType checkMsgType(Object msg) throws MessageTypeException
	{
		if (msg instanceof Message)
		{
			return ((Message) msg).getMsgType();
		} else
		{
			throw new MessageTypeException("Not int Type Message");
		}
	}
	
// Join Processing
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
					oos.writeObject(new Message(MsgType.REFUSE));
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				return false;
			} else
			{
				// Close previous oos
				ObjectOutputStream pre_oos = onlineUsers.get(user_id);
				try
				{
					pre_oos.writeObject(new Message(MsgType.REFUSE));
					pre_oos.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				// Update this oos
				onlineUsers.replace(user_id, oos);
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
						oos.writeObject(new Message(MsgType.DONE));
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
						oos.writeObject(new Message(MsgType.REFUSE));
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
					oos.writeObject(new Message(MsgType.REFUSE));
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				return false;
			}
		}
		
		// Send Saved Messages when client is offline
		Queue<Message> queue = savedMessages.get(user_id);
		if (queue == null) System.out.println("Server_Main: - Saved Msg Queue Unfound for: " + user_id);
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
	private void chat(int user_id, ObjectInputStream ois, ObjectOutputStream oos) throws Exception
	{
		// Keep Receiving
		while (true)
		{
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
						sendTo((Message) msg, recipients);
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
						oos.writeObject(new Message(MsgType.DONE));
						oos.close();
						return;
					} else 
					{
						System.out.println("Server_Main: - Remove user failed for " + user_id);
						oos.writeObject(new Message(MsgType.REFUSE));
					}
					break;
				case SET_NICKNAME:
					String newName = ((Message) msg).getMsg();
					if (dbServer.setNickName(user_id, newName))
					{
						oos.writeObject(new Message(MsgType.DONE));
					} else 
					{
						System.out.println("Server_Main: - Set NickName failed for " + user_id);
						oos.writeObject(new Message(MsgType.REFUSE));
					}
					break;
				case SET_PASSWORD:
					String passwords = ((Message) msg).getMsg();
					int blankOffset = passwords.indexOf(" ");
					if (blankOffset < 0)
					{
						oos.writeObject(new Message(MsgType.REFUSE));
						continue;
					}
					String oldPassword = passwords.substring(0, blankOffset);
					String newPassword = passwords.substring(blankOffset + 1);
					if (dbServer.setPassword(user_id, oldPassword, newPassword))
					{
						oos.writeObject(new Message(MsgType.DONE));
					} else 
					{
						// Invalid password
						System.out.println("Server_Main: Invalid password from: " + user_id);
						oos.writeObject(new Message(MsgType.REFUSE));
					}
					break;
				default:
					System.out.println("Server_Main: - Wrong message enum type from:" + user_id + "Session continued");
				}
			} catch(MessageTypeException e)
			{
				System.out.println("Server_Main: - Wrong message type from: " + user_id + "Session continued");
			}
		}
	}
	
// Leave Processing
	private void leave(int user_id, ObjectOutputStream oos)
	{
		// Re-Join Situation
		if (onlineUsers.get(user_id) != oos)
		{
			System.out.println("Server_Main: Re-joining, previous session for " + user_id + " terminated");
			return;
		}
		// Update Users Lists
		onlineUsers.remove(user_id);
		offlineUsers.add(user_id);
		System.out.println("Server_Main: Offline & Online Users: [ID]");
		System.out.println(offlineUsers);
		System.out.println(onlineUsers.keySet());
		sendUserList();
		System.out.println("Server_Main: Leave succeed: " + user_id);
	}
	
// Add User
	private int addUser(String nick_name, String password)
	{
		try
		{
			User user = new User(nick_name, password);
			if (dbServer.addUser(user))
			{
				int user_id = user.getUser_id();
				savedMessages.put(user_id, (Queue<Message>) new LinkedList<Message>());
				return user_id;
			}
		} catch(Exception e)
		{
			System.out.println("Server_Main: - Error creating new user. " + e.getMessage());
		}
		return 0;
	}
// Send User List
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
		for (int id : offlineUsers)
		{
			savedMessages.get(id).add(msg);
		}
		System.out.println("Server_Main: Saved Messages for Each User: {ID=[MsgQueue]}");
		System.out.println(savedMessages);
	}
	
// Send To
	private synchronized void sendTo(Message msg, int[] recipient_id)
	{
		for (int id : recipient_id)
		{
			if (onlineUsers.containsKey(id))
			{
				ObjectOutputStream oos = onlineUsers.get(id);
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
				System.out.println("Server_Main: - recipient " + id + " is not a user.");
			}
		}
		System.out.println("Server_Main: Saved Messages for Each User: {ID=[MsgQueue]}");
		System.out.println(savedMessages);
	}

// main()
	public static void main(String[] args)
	{
		System.out.println("-----" + new Date() + "-----\n");
		if (args.length != 0)
		{
			System.out.println("Cmd Line Argument(s) Ignored");
		}
		try
		{
			System.out.println("Server_Main: Initializing Server_Main ...");
			new Server_Main(1111);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

}
