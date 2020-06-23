package my.chatroom.data.json;

import java.util.LinkedList;
import java.util.Queue;

import my.chatroom.data.trans.Message;
import my.chatroom.data.trans.MsgType;

public class JSON_Test
{

	public static void main(String[] args) throws Exception
	{
		Message msg = new Message("test string", 0, new int[]{1,2,3}, MsgType.MESSAGE, System.currentTimeMillis() / 1000L);
		System.out.println(msg);
		String json = JSON_Utility.encodeMSG(msg);
		System.out.println(json);
		
		Message decoded = JSON_Utility.decodeMSG(json);
		System.out.println(decoded);
		
		Message msg2 = new Message("test string2", 1, new int[]{1,2,3}, MsgType.ADD_USER, System.currentTimeMillis() / 1000L);
		Queue<Message> queue = new LinkedList<Message>();
		queue.offer(msg);
		queue.offer(msg2);
		System.out.println(queue);
		String json2 = JSON_Utility.encodeMSGs(queue);
		System.out.println(json2);
		
		Queue<Message> queue_decoded = JSON_Utility.decodeMSGs(json2);
		System.out.println(queue_decoded);
	}

}
