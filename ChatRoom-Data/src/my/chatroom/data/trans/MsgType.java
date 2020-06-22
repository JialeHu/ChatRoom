package my.chatroom.data.trans;

/**
 * 
 * @author Jiale Hu
 * @version 1.0
 * @since 05/05/2020
 *
 */

public enum MsgType
{
	MESSAGE,
	FILE,
	
	// From Client
	JOIN,
	LEAVE,
	
	USER_INFO,
	ADD_USER,
	RM_USER,
	SET_NICKNAME,
	SET_PASSWORD,
	
	// From Server
	USER_LIST,
	REFUSE,
	DONE
}
