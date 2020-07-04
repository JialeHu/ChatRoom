package my.chatroom.server.interfaces;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import my.chatroom.data.messages.Message;
import my.chatroom.data.users.User;

public interface Server_DB_Interface
{

	// Get Pointer of savedMessages
	/**
	 * Get {@code Message}s to be sent from DB as a {@code ConcurrentHashMap<Integer, Queue<Message>>}
	 * @return {@code ConcurrentHashMap<Integer, Queue<Message>>}: recipients ID, message queues
	 */
	ConcurrentHashMap<Integer, Queue<Message>> getSavedMessages();

	// Save All savedMessages
	/**
	 * Save all {@code Message} from memory to DB via JSON serialization.
	 * @return {@code true} if saved successfully
	 */
	boolean saveAllMessages();
	
	// Save Messages for a User
	boolean saveMessages(int user_id);

	// Delete savedMessages for a User
	/**
	 * Delete {@code Message} for a user saved on DB.
	 * @param user_id recipient user ID of the {@code Message} to be deleted
	 * @return {@code true} if deleted successfully
	 */
	boolean deleteMessages(int user_id);

	// Get All IDs
	/**
	 * Get all user IDs from DB.
	 * @return {@code Integer[]} of user IDs
	 */
	Integer[] getIDs();

	/**
	 * Get nick name of a user.
	 * @param user_id user ID of the nick name to be found
	 * @return nick name as {@code String}
	 */
	String getNickName(int user_id);

	/**
	 * Get general information of a user.
	 * @param user_id user ID of the information to be found
	 * @return user information as {@code String}
	 */
	String getUserInfo(int user_id);

	// Check User
	/**
	 * Check if a user exists on DB.
	 * @param user_id user ID to be check
	 * @return {@code true} if the user exists
	 */
	boolean isExist(int user_id);

	// Get Next User ID
	/**
	 * Get next available user id from DB.
	 * @return next user id, or -1 if failed
	 * @throws SQLException if DB query failed
	 */
	int getNextUserID() throws Exception;

	// Add User
	/**
	 * Add a new user to DB.
	 * @param user {@code User} to be added
	 * @return {@code true} if added successfully
	 */
	User addUser(String nick_name, String password);

	// Remove User
	/**
	 * Remove an existing user from DB.
	 * @param user_id user ID of the user to be removed
	 * @return {@code true} if removed successfully 
	 */
	boolean removeUser(int user_id);

	// Set Nick Name
	/**
	 * Set nick name for a user.
	 * @param user_id user ID of user to be set
	 * @param newName new nick name
	 * @return {@code true} if set successfully
	 */
	boolean setNickName(int user_id, String newName);

	// Check Password
	/**
	 * Check password of a user.
	 * @param user_id user ID to be checked
	 * @param password password to be checked
	 * @return {@code true} if password is correct
	 */
	boolean checkPassword(int user_id, String password);

	// Set Password
	/**
	 * Set new password for a user.
	 * @param user_id user ID to be set
	 * @param oldPassword old password of the user
	 * @param newPassword new password to be set
	 * @return {@code true} if set successfully
	 */
	boolean setPassword(int user_id, String oldPassword, String newPassword);

}