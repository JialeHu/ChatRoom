package my.chatroom.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

import my.chatroom.data.server.*;

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
	private Connection connection;
	private PreparedStatement insertStatement;
	private PreparedStatement updateNickNameStatement;
	private PreparedStatement updatePasswordStatement;
	private PreparedStatement deleteStatement;
	private Statement selectAllStatement;
	
	private ConcurrentHashMap<Integer, User> usersMap = new ConcurrentHashMap<Integer, User>();
	
// Constructor
	public Server_DB() throws FetalDataBaseException
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
			throw new FetalDataBaseException("Failed to load db2");
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to connect DB. " + e.getMessage());
			throw new FetalDataBaseException("Failed to connect DB");
		}
		
		// Prepare SQL statement
		System.out.println("Server_DB: Preparing SQL Statements ...");
		try
		{
			insertStatement = connection.prepareStatement("INSERT INTO USERS (USER_ID, NICK_NAME, PASSWORD) VALUES (?,?,?)");
			updateNickNameStatement = connection.prepareStatement("UPDATE USERS SET NICK_NAME = ? WHERE USER_ID = ?");
			updatePasswordStatement = connection.prepareStatement("UPDATE USERS SET PASSWORD = ? WHERE USER_ID = ?");
			deleteStatement = connection.prepareStatement("DELETE FROM USERS WHERE USER_ID = ?");
			selectAllStatement = connection.createStatement();
			
		} catch (SQLException e)
		{
			System.err.println("Server_DB: - Failed to prepare SQL statements. " + e.getMessage());
			return;
		}
		
		// Retrieve users from DB
		System.out.println("Server_DB: Loading Users from DB ...");
		try
		{
			ResultSet rs = selectAllStatement.executeQuery("SELECT * FROM USERS");
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
			return;
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Failed to load user. " + e.getMessage());
			return;
		}
		
		System.out.println("Server_DB: Users Successful Loaded: [ID]");
		System.out.println(usersMap.keySet());
		System.out.println("Server_DB: Server_DB is Up");
		
		System.out.println("-----Server_DB Loaded-----\n");
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
	public boolean addUser(User user)
	{
		try
		{
			insertStatement.setInt(1, user.getUser_id());
			insertStatement.setString(2, user.getNick_name());
			insertStatement.setString(3, user.getPassword());
			insertStatement.executeUpdate();
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
	public boolean removeUser(int user_id)
	{
		User user = usersMap.get(user_id);
		try
		{
			deleteStatement.setInt(1, user_id);
			deleteStatement.executeUpdate();
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
			updateNickNameStatement.setString(1, user.getNick_name());
			updateNickNameStatement.setInt(2, user_id);
			updateNickNameStatement.executeUpdate();
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
			updatePasswordStatement.setString(1, user.getPassword());
			updatePasswordStatement.setInt(2, user_id);
			updatePasswordStatement.executeUpdate();
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
