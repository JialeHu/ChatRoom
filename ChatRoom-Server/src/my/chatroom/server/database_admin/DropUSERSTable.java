package my.chatroom.server.database_admin;


import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DropUSERSTable
{
	public static void main(String[] args) throws Exception
	{
		Class.forName("com.ibm.db2j.jdbc.DB2jDriver");
		System.out.println("Driver loaded");
		Connection connection = DriverManager.getConnection("jdbc:db2j:" + new File("").getAbsolutePath().concat("/database/QuoteDB"));
		System.out.println("Connected to database");
		Statement statement = connection.createStatement();
		statement.execute("DROP TABLE USERS");
		System.out.println("USERS Table deleted.");
	}
}
