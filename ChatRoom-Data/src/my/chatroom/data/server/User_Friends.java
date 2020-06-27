package my.chatroom.data.server;

import java.util.*;

public class User_Friends extends User
{
	private static final long serialVersionUID = 1L;
	
	private Vector<Integer> friends;

	public User_Friends() throws Exception
	{
		super();
	}

	public User_Friends(String nick_name, String password) throws Exception
	{
		super(nick_name, password);
	}

	public void addFriend(int user_id)
	{
		friends.add(user_id);
	}
	
	public void removeFriend(Integer user_id)
	{
		friends.remove(user_id);
	}
	
	public void removerAllFriends()
	{
		friends.clear();
	}
	
	public final Vector<Integer> getFriends()
	{
		return friends;
	}

	public final void setFriends(Vector<Integer> friends)
	{
		this.friends = friends;
	}

}
