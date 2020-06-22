package my.chatroom.server.database_admin;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class ListAllTables {
	public static void main(String args[]) throws Exception {
		Class.forName("com.ibm.db2j.jdbc.DB2jDriver");
		System.out.println("Driver loaded");
		Connection connection = DriverManager.getConnection("jdbc:db2j:" + new File("").getAbsolutePath().concat("/database/QuoteDB"));
		System.out.println("Connected to database");
		DatabaseMetaData md = connection.getMetaData();
		ResultSet rs = md.getTables(null, null, "%", null);
		System.out.println("Tables in the current database: ");
		while(rs.next()) {
			System.out.println(rs.getString("TABLE_NAME"));
		}
	}
}