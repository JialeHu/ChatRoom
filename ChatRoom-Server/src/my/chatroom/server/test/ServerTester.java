package my.chatroom.server.test;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import my.chatroom.data.server.*;
import my.chatroom.data.trans.*;

/**
 * 
 * @author Jiale Hu
 * @version 1.0
 * @since 05/05/2020
 *
 */

public class ServerTester
{

	public static void main(String[] args) throws Exception
	{
		// Generate LastID.ser
//		FileOutputStream fos = new FileOutputStream("LastID.ser");
//		ObjectOutputStream diskOOS = new ObjectOutputStream(fos);
//		diskOOS.writeObject(0); // write the whole collection to disk!
//		diskOOS.close();
		
		// Connect to server
		Socket s = new Socket("localhost", 1111);
		ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
		
		// New User
		String nick_name = "hjl";
		String password = "123456";
		String newStr = nick_name + " " + password;
		Message newMsg = new Message(newStr, 0, MsgType.ADD_USER);
				
		// Join Msg
		int user_id = 1;
		Message joinMsg = new Message(password, user_id, MsgType.JOIN);
		
		oos.writeObject(joinMsg);
		
		ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
		
		Message reply = (Message) ois.readObject();
		
		System.out.println(reply);
		
		Message sendMsg = new Message("Hello!", user_id, (int[]) null);

		oos.writeObject(sendMsg);
		
	}

}
