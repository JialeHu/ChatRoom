package my.chatroom.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public interface ClientInterface
{
	public void loggedIn(Socket s, ObjectOutputStream oos, ObjectInputStream ois, int user_id);
}
