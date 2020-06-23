package my.chatroom.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import my.chatroom.data.json.JSON_Utility;
import my.chatroom.data.server.*;
import my.chatroom.data.trans.Message;
import my.chatroom.server.exception.FatalDataBaseException;

/**
 * 
 * @author Jiale Hu
 * @version 1.0
 * @since 05/05/2020
 *
 */

public class Server_DB
{

// Instance Variables
	private final Connection connection;
	
	private PreparedStatement insertStmtUSERS;
	private PreparedStatement updateNickNameStmtUSERS;
	private PreparedStatement updatePasswordStmtUSERS;
	private PreparedStatement deleteStmtUSERS;
	
	private PreparedStatement insertStmtMSGS;
	private PreparedStatement deleteStmtMSGS;
	private PreparedStatement deleteAllStmtMSGS;
	
	private Statement selectAllStmt;
	
	private final ConcurrentHashMap<Integer, User> usersMap;
	private final ConcurrentHashMap<Integer, Queue<Message>> savedMessages;
	
// Constructor
	public Server_DB() throws FatalDataBaseException
	{
		System.out.println("Server_DB: Initializing Server_DB ...");
		
		// Load driver & Connect to DB
		System.out.println("Server_DB: Loading Driver & Connecting to DB ...");
		try
		{
			Class.forName("com.ibm.db2j.jdbc.DB2jDriver");
			connection = DriverManager.getConnection("jdbc:db2j:" + new File("").getAbsolutePath().concat("/database/QuoteDB"));
		} catch (ClassNotFoundException e)
		{
			System.err.println("Server_DB: - Failed to load db2j. " + e.getMessage());
			throw new FatalDataBaseException("Failed to load db2");
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to connect DB. " + e.getMessage());
			throw new FatalDataBaseException("Failed to connect DB");
		}
		
		// Prepare SQL statement
		System.out.println("Server_DB: Preparing SQL Statements ...");
		try
		{
			insertStmtUSERS = connection.prepareStatement("INSERT INTO USERS (USER_ID, NICK_NAME, PASSWORD) VALUES (?,?,?)");
			updateNickNameStmtUSERS = connection.prepareStatement("UPDATE USERS SET NICK_NAME = ? WHERE USER_ID = ?");
			updatePasswordStmtUSERS = connection.prepareStatement("UPDATE USERS SET PASSWORD = ? WHERE USER_ID = ?");
			deleteStmtUSERS = connection.prepareStatement("DELETE FROM USERS WHERE USER_ID = ?");
			
			insertStmtMSGS = connection.prepareStatement("INSERT INTO MSGS (USER_ID, MSG_Q) VALUES (?,?)");
			deleteStmtMSGS = connection.prepareStatement("DELETE FROM MSGS WHERE USER_ID = ?");
			deleteAllStmtMSGS = connection.prepareStatement("DELETE FROM MSGS");
			
			selectAllStmt = connection.createStatement();
			
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to prepare SQL statements. " + e.getMessage());
			throw new FatalDataBaseException("Failed to prepare SQL statements");
		}
		
		// Retrieve users from DB
		System.out.println("Server_DB: Loading Users from DB ...");
		usersMap = new ConcurrentHashMap<Integer, User>();
		try
		{
			ResultSet rs = selectAllStmt.executeQuery("SELECT * FROM USERS");
			while (rs.next())
			{
				int user_id = rs.getInt("USER_ID");
				String nick_name = rs.getString("NICK_NAME");
				String password = rs.getString("PASSWORD");
				User user = User.restoreFromDB(user_id, nick_name, password);
				usersMap.put(user_id, user);
			}
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to retrieve data from DB. " + e.getMessage());
			throw new FatalDataBaseException("Failed to retrieve data from DB");
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Failed to load user. " + e.getMessage());
			throw new FatalDataBaseException("Failed to load user");
		}
		System.out.println("Server_DB: Users Successful Loaded: [ID]");
		System.out.println(usersMap.keySet());
		
		// Retrieve savedMessages from DB
		System.out.println("Server_DB: Loading Messages from DB ...");
		savedMessages = new ConcurrentHashMap<Integer, Queue<Message>>();
		try
		{
			ResultSet rs = selectAllStmt.executeQuery("SELECT * FROM MSGS");
			while (rs.next())
			{
				int user_id = rs.getInt("USER_ID");
				String msgs = rs.getString("MSG_Q");
				Queue<Message> queue = JSON_Utility.decodeMSGs(msgs);
				savedMessages.put(user_id, queue);
			}
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to retrieve savedMessages from DB. " + e.getMessage());
			throw new FatalDataBaseException("Failed to retrieve savedMessages from DB");
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Failed to load message. " + e.getMessage());
			throw new FatalDataBaseException("Failed to load message");
		}
		System.out.println("Server_DB: Saved Messages Successful Loaded: {ID=[MSGS]}");
		System.out.println(savedMessages);
		
		System.out.println("-----Server_DB Loaded-----\n");
	}
	
// Get Pointer of savedMessages
	public ConcurrentHashMap<Integer, Queue<Message>> getSavedMessages()
	{
		return savedMessages;
	}
	
// Save All savedMessages
	public boolean saveAllMessages()
	{
		System.out.println("Server_DB: Saving all savedMessages ... (With Delete First)");
		if (!deleteAllMessages()) return false;
			
		for (int user_id : savedMessages.keySet())
		{
			// Queue to JSON string
			String msgs = JSON_Utility.encodeMSGs(savedMessages.get(user_id));
			try
			{
				insertStmtMSGS.setInt(1, user_id);
				insertStmtMSGS.setString(2, msgs);
				insertStmtMSGS.executeUpdate();
			} catch (SQLException e)
			{
				System.err.println("Server_DB: - Failed to save savedMessages to DB for " + user_id + e.getMessage());
				return false;
			}
		}
		System.out.println("Server_DB: All savedMessages saved to DB");
		return true;
	}
	
// Delete All savedMessages
	private boolean deleteAllMessages()
	{
		System.out.println("Server_DB: Deleting all savedMessages ...");
		try
		{
			deleteAllStmtMSGS.execute();
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to delete all savedMessages from DB " + e.getMessage());
			return false;
		}
		System.out.println("Server_DB: All savedMessages deleted froms DB");
		return true;
	}
	
// Delete savedMessages for a User
	public boolean deleteMessages(int user_id)
	{
		try
		{
			deleteStmtMSGS.setInt(1, user_id);
			deleteStmtMSGS.executeUpdate();
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to delete savedMessages from DB for " + user_id + e.getMessage());
			return false;
		}
		System.out.println("Server_DB: savedMessages deleted froms DB for " + user_id);
		return true;
	}
	
// Get All IDs
	public Integer[] getIDs()
	{
		return usersMap.keySet().toArray(new Integer[0]);
	}

// Get User
//	public User getUser(int user_id)
//	{
//		return usersMap.get(user_id);
//	}
	
	public String getNickName(int user_id)
	{
		return usersMap.get(user_id).getNick_name();
	}
	
	public String getUserInfo(int user_id)
	{
		return usersMap.get(user_id).toString();
	}

// Check User
	public boolean isExist(int user_id)
	{
		return usersMap.containsKey(user_id);
	}
	
// Add User
	public synchronized boolean addUser(User user)
	{
		try
		{
			insertStmtUSERS.setInt(1, user.getUser_id());
			insertStmtUSERS.setString(2, user.getNick_name());
			insertStmtUSERS.setString(3, user.getPassword());
			insertStmtUSERS.executeUpdate();
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to add user to DB. " + user + e.getMessage());
			return false;
		}
		
		usersMap.put(user.getUser_id(), user);
		System.out.println("Server_DB: User Added to DB: " + user);
		return true;
	}

// Remove User
	public synchronized boolean removeUser(int user_id)
	{
		User user = usersMap.get(user_id);
		try
		{
			deleteStmtUSERS.setInt(1, user_id);
			deleteStmtUSERS.executeUpdate();
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to remove user from DB. " + user + e.getMessage());
			return false;
		}
		usersMap.remove(user_id);
		System.out.println("Server_DB: User Removed from DB: " + user);
		return true;
	}
	
// Set Nick Name
	public boolean setNickName(int user_id, String newName)
	{
		User user = usersMap.get(user_id);
		if (newName.equals(usersMap.get(user_id).getNick_name()))
		{
			System.out.println("Server_DB: - NickName unchanged: " + user);
			return false;
		}
		user.setNick_name(newName);
		try
		{
			updateNickNameStmtUSERS.setString(1, user.getNick_name());
			updateNickNameStmtUSERS.setInt(2, user_id);
			updateNickNameStmtUSERS.executeUpdate();
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to update user nick_name to DB. " + user + e.getMessage());
			return false;
		}
		usersMap.replace(user_id, user);
		System.out.println("Server_DB: User nick_name updated to DB: " + user);
		return true;
	}

// Check Password
	public boolean checkPassword(int user_id, String password)
	{
		User user = usersMap.get(user_id);
		return user.checkPassword(password);
	}

// Set Password
	public boolean setPassword(int user_id, String oldPassword, String newPassword)
	{
		User user = usersMap.get(user_id);
		if (!user.checkPassword(oldPassword))
		{
			System.out.println("Server_DB: - Wrong password: " + user);
			return false;
		}
		user.setPassword(oldPassword, newPassword);
		try
		{
			updatePasswordStmtUSERS.setString(1, user.getPassword());
			updatePasswordStmtUSERS.setInt(2, user_id);
			updatePasswordStmtUSERS.executeUpdate();
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to update password to DB. " + user + e.getMessage());
			return false;
		}
		usersMap.replace(user_id, user);
		System.out.println("Server_DB: User password updated to DB: " + user);
		return true;
	}

}
