package my.chatroom.data.messages;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 
 * @author Jiale Hu
 * @version 1.0
 * @since 05/05/2020
 *
 */

public class Message implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

// Instance Variables
    private final String msg;
    private final int user_id;
    private final long time;
    private final int[] recipients;
    private HashMap<Integer, String> onlineUsers = null;
    private HashMap<Integer, String> offlineUsers = null;
    private MsgType msgType = MsgType.MESSAGE;

// Constructor
    // For Chat
    public Message(String msg, int sender_id, int[] recipients) {
	this.msg = msg;
	this.user_id = sender_id;
	this.recipients = recipients;
	this.time = System.currentTimeMillis();
    }

    // For Non-chat
    public Message(String data, int user_id, MsgType msgType) {
	this.msg = data;
	this.user_id = user_id;
	this.recipients = null;
	this.time = System.currentTimeMillis();
	this.msgType = msgType;
    }

    // Update User List
    public Message(HashMap<Integer, String> onlineUsers, HashMap<Integer, String> offlineUsers) {
	this.msg = null;
	this.user_id = 0;
	this.recipients = null;
	this.time = System.currentTimeMillis();
	this.onlineUsers = onlineUsers;
	this.offlineUsers = offlineUsers;
	this.msgType = MsgType.USER_LIST;
    }

    // Server Reply
    public Message(MsgType msgType, String data) {
	this.msg = data;
	this.user_id = 0;
	this.recipients = null;
	this.time = System.currentTimeMillis();
	this.msgType = msgType;
    }

    // De-serialize
    public Message(String data, int sender_id, int[] recipients, MsgType msgType, long time) {
	this.msg = data;
	this.user_id = sender_id;
	this.recipients = recipients;
	this.time = time;
	this.msgType = msgType;
    }

    public String getMsg() {
	return msg;
    }

    public int getUser_id() {
	return user_id;
    }

    public int[] getRecipients() {
	return recipients;
    }

    public long getTime() {
	return time;
    }

    public MsgType getMsgType() {
	return msgType;
    }

    public HashMap<Integer, String> getOnlineUsers() {
	return onlineUsers;
    }

    public HashMap<Integer, String> getOfflineUsers() {
	return offlineUsers;
    }

    @Override
    public String toString() {
	return msg + " " + user_id + " " + time + " " + msgType + " " + Arrays.toString(recipients) + onlineUsers
		+ offlineUsers;
    }

}
