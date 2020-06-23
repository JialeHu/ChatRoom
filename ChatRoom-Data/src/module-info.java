module chat_room.data
{
	exports my.chatroom.data.trans;
	exports my.chatroom.data.json;
	exports my.chatroom.data.server to chat_room.server;
	requires org.json;
	requires json.simple;
}