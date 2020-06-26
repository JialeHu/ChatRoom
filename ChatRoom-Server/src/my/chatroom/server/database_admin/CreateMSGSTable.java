package my.chatroom.server.database_admin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CreateMSGSTable
{

	public static void main(String[] args) throws Exception
	{
		Class.forName("com.ibm.db2j.jdbc.DB2jDriver");
		System.out.println("Driver loaded");
		Connection conn = DriverManager.getConnection("jdbc:db2j:" + new File("").getAbsolutePath().concat("/database/QuoteDB"));
		System.out.println("Connection made to Data Base");

		Statement statement = conn.createStatement();
		statement.execute("CREATE TABLE MSGS "
				+ "(USER_ID  INTEGER    	 NOT NULL,"
				+ " MSG_Q    VARCHAR(32704)  NOT NULL,"
				+ " PRIMARY KEY (USER_ID))");
		System.out.println("MSGS table built");
	}

}
