package my.chatroom.data.json;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Queue;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import my.chatroom.data.messages.Message;
import my.chatroom.data.messages.MsgType;

public class JSON_Utility {
    public static String encodeMSG(Message msg) {
	org.json.JSONObject json = new org.json.JSONObject();

	json.put("msg", msg.getMsg());
	json.put("user_id", msg.getUser_id());
	json.put("time", msg.getTime());
	json.put("recipients", msg.getRecipients());
	json.put("msgType", msg.getMsgType());

	StringWriter out = new StringWriter();
	json.write(out);

	return out.toString();
    }

    public static Message decodeMSG(String json) throws ParseException {
	JSONParser parser = new JSONParser();
	JSONObject obj = (JSONObject) parser.parse(json);

	String msg = (String) obj.get("msg");
	int user_id = ((Long) obj.get("user_id")).intValue();

	JSONArray array = (JSONArray) obj.get("recipients");
	int[] recipients;
	if (array == null)
	    recipients = null;
	else {
	    recipients = new int[array.size()];
	    int i = 0;
	    for (Object ele : array)
		recipients[i++] = ((Long) ele).intValue();
	}

	MsgType msgType = MsgType.valueOf((String) obj.get("msgType"));
	long time = (long) obj.get("time");

	Message message = new Message(msg, user_id, recipients, msgType, time);
	return message;
    }

    public static String encodeMSGs(Queue<Message> msgs) {
	org.json.JSONArray array = new org.json.JSONArray();
	for (Message msg : msgs)
	    array.put(encodeMSG(msg));
	StringWriter out = new StringWriter();
	array.write(out);
	return out.toString();
    }

    public static Queue<Message> decodeMSGs(String json) {
	Queue<Message> queue = new LinkedList<Message>();

	JSONParser parser = new JSONParser();
	try {
	    JSONArray array = (JSONArray) parser.parse(json);
	    for (Object ele : array)
		queue.offer(decodeMSG((String) ele));
	} catch (Exception e) {
	    System.err.println("JSON Decode Failure, Input: " + json);
	    e.printStackTrace();
	}

	return queue;
    }
}
