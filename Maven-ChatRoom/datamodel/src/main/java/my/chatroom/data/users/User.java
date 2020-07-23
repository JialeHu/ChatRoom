package my.chatroom.data.users;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * 
 * @author Jiale Hu
 * @version 1.0
 * @since 05/05/2020
 *
 */

public class User implements Serializable {
    private static final long serialVersionUID = 2L;

// Instance Variables
    private int user_id;
    private long signup_time;
    private String nick_name;
    private transient String password;
    private String messages;

    // Constructors
    public User() {
    }

    public User(int user_id, String nick_name, String password) throws SQLException {
	this.user_id = user_id;
	this.signup_time = System.currentTimeMillis();
	this.nick_name = nick_name;
	this.password = password;
	this.messages = "";
    }

    // For recovery from DB
    public User(int user_id, long time, String nick_name, String password, String messages) throws Exception {
	this.user_id = user_id;
	this.signup_time = time;
	this.nick_name = nick_name;
	this.password = password;
	this.messages = messages;
    }

// nick_name
    public final String getNick_name() {
	return nick_name;
    }

    public final void setNick_name(String nick_name) {
	this.nick_name = nick_name;
    }

// user_id
    public final int getUser_id() {
	return user_id;
    }

// Signup time
    public final long getSignup_time() {
	return signup_time;
    }

// password
    public final boolean checkPassword(String password) {
	return this.password.equals(password);
    }

    public final String getPassword() {
	return this.password;
    }

    public final boolean setPassword(String oldPassword, String newPassword) {
	if (this.checkPassword(oldPassword)) {
	    this.password = newPassword;
	    return true;
	}
	return false;
    }

// messages
    public String getMessages() {
	return messages;
    }

    public void setMessages(String messages) {
	this.messages = messages;
    }

// toString()
    @Override
    public String toString() {
	return getClass().getName() + ", ID: " + this.getUser_id() + ", NickName: " + this.getNick_name();
    }

}
