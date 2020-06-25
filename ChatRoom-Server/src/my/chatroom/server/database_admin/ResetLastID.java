package my.chatroom.server.database_admin;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ResetLastID
{

	public static void main(String[] args) throws Exception
	{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream("LastID.ser"));
		int lastID = (int) ois.readObject();
		ois.close();
		
		lastID = 0;
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("LastID.ser"));
		oos.writeObject(lastID);
		oos.close();
		
		System.out.println("Reset Last ID to " + lastID);
	}

}
