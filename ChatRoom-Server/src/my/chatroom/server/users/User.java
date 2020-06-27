package my.chatroom.server.users;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.SQLException;

import my.chatroom.server.Server_DB;

/**
 * 
 * @author Jiale Hu
 * @version 1.0
 * @since 05/05/2020
 *
 */

public class User implements Serializable
{
	private static final long serialVersionUID = 1L;
	
// Static
	public static User restoreFromDB(int user_id, long time, String nick_name, String password) throws Exception
	{
		return new User(user_id, time, nick_name, password);
	}
	
	private static int lastID; // 0 if not yet initialized from disk
	private static synchronized int getNextID() throws FileNotFoundException, IOException, ClassNotFoundException
	{
		if (lastID == 0)
		{
			// Initialize lastID from disk
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("LastID.ser"));
			lastID = (int) ois.readObject();
			ois.close();
		}
		lastID++;
		// Write the updated lastID to disk
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("LastID.ser"));
		oos.writeObject(lastID);
		oos.close();
		return lastID;
	}
	
// Instance Variables
	private final int user_id;
	private final long signUpTime;
	private String nick_name;
	private transient String password;
	
// Constructors
	protected User() throws FileNotFoundException, ClassNotFoundException, IOException
	{
		this.user_id = User.getNextID();
		this.signUpTime = System.currentTimeMillis();
	}
	
	public User(String nick_name, String password) throws Exception
	{
		this();
		this.nick_name = nick_name;
		this.password = password;
	}
	
	public User(Server_DB dbServer, String nick_name, String password) throws SQLException
	{
		this.user_id = dbServer.getNextUserID();
		this.signUpTime = System.currentTimeMillis();
		this.nick_name = nick_name;
		this.password = password;
	}
	
	// For recovery from DB
	private User(int user_id, long time, String nick_name, String password) throws Exception
	{
		this.user_id = user_id;
		this.signUpTime = time;
		this.nick_name = nick_name;
		this.password = password;
	}

// nick_name
	public final String getNick_name()
	{
		return nick_name;
	}

	public final void setNick_name(String nick_name)
	{
		this.nick_name = nick_name;
	}

// user_id
	public final int getUser_id()
	{
		return user_id;
	}
	
// Signup time
	public final long getSignUpTime()
	{
		return signUpTime;
	}
	
// password
	public final boolean checkPassword(String password)
	{
		return this.password.equals(password);
	}
	
	public final String getPassword()
	{
		return this.password;
	}

	public final boolean setPassword(String oldPassword, String newPassword)
	{
		if (this.checkPassword(oldPassword)) 
		{
			this.password = newPassword;
			return true;
		}
		return false;
	}
	
// toString()
	@Override
	public String toString()
	{
		return getClass().getName() + ", ID: " + this.getUser_id() + ", NickName: " + this.getNick_name();
	}

}
