module chat_room.server
{
	requires java.base;
	requires java.sql;
	requires java.desktop;
	requires java.rmi; 
	requires db2j; // Derived from db2j.jar filename
	requires chat_room.data;
}