package my.chatroom.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.*;
import javax.swing.border.Border;

import my.chatroom.data.trans.*;

public final class Client_Main implements ActionListener, Runnable
{
// Instance Variables

	// Connection
	private Socket s;
	private ObjectInputStream  ois;
	private ObjectOutputStream oos;
	
	// User
	private int user_id;
	private String nick_name;

	// User List
	private HashMap<Integer, String> onlineUsers;
	private HashMap<Integer, String> offlineUsers;
	
	// Message Queue
	private BlockingQueue<Message> msgQueue;
	
	// Screen info
	private Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

	// Main GUI objects
	private JFrame     	mainWindow    		= new JFrame("Chat Room"); 
	private JTextField 	messageTextField	= new JTextField("Enter a message here");
	private JButton     sendButton			= new JButton("Send");
	private JTextArea  	messagTextArea  	= new JTextArea("Received chat messages will be shown here.\n");
	private JScrollPane messageScrollPane 	= new JScrollPane(messagTextArea);
	
	// User List GUI objects
	private JFrame      	settingsWindow      = new JFrame("Chat Room - Settings");
	
	
	private JTextField  	privateTextField  	= new JTextField("Enter a PRIVATE message to be sent to SELECTED clients");
	private JButton     	settingsButton      = new JButton("Settings");
	private JButton     	clearButton       	= new JButton("Clear Selections");
	private JPanel      	leftPanel     	  	= new JPanel();
	private JPanel      	userListPanel       = new JPanel();
	private JList<String> 	onlineList      	= new JList<String>();
	private JList<String> 	offlineList   		= new JList<String>();
	private JScrollPane 	onlineScrollPane  	= new JScrollPane(onlineList);
	private JScrollPane 	offlineScrollPane 	= new JScrollPane(offlineList);
	
	private JPanel			chatPanel = new JPanel();
	
	private JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, chatPanel);

	private Client_Main(String serverAddress, int serverPort)
	{
		// Setup System Info
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e)
		{
			new Client_Error(dim, e.toString());
		}
		
		// Log in or Sign up
		new Client_Login(this, dim, serverAddress, serverPort);
		
		// Build Main Window
		mainWindow.setSize(700,600);
		mainWindow.setMinimumSize(new Dimension(400, 300));
		mainWindow.setLocation(dim.width/2-mainWindow.getSize().width/2, dim.height/2-mainWindow.getSize().height/2);
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		mainWindow.getContentPane().add(splitPane);
		leftPanel.setMinimumSize(new Dimension(250, 250));
//		leftPanel.setMaximumSize(new Dimension(350, dim.height));
		
		
//		userListPanel.setLayout(new GridLayout(2, 1));
//		userListPanel.add(onlineList);
//		userListPanel.add(offlineList);
		
		onlineList.setLayoutOrientation(JList.VERTICAL);
		onlineList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		chatPanel.setBackground(Color.LIGHT_GRAY);
		
		leftPanel.add(settingsButton, "North");
		leftPanel.add(clearButton, "North");
		
		JPanel listPanel = new JPanel();
		JLabel onlineLabel = new JLabel("Online Users:");
		JLabel offlineLabel = new JLabel("Offline Users:");
		listPanel.setLayout(new GridLayout(4, 1));
		leftPanel.add(listPanel, "Center");
		listPanel.add(onlineLabel);
		listPanel.add(onlineList);
		listPanel.add(offlineLabel);
		listPanel.add(offlineList);
		leftPanel.setBackground(Color.GRAY);
		onlineLabel.setBackground(Color.GRAY);
		onlineLabel.setOpaque(true);
		offlineLabel.setBackground(Color.GRAY);
		offlineLabel.setOpaque(true);
		onlineList.setBackground(Color.GRAY);
		offlineList.setBackground(Color.GRAY);
		onlineList.setFont(new Font("default", Font.PLAIN, 20));
		offlineList.setFont(new Font("default", Font.PLAIN, 20));
		
		chatPanel.setLayout(new GridLayout(2,1));
		chatPanel.add(messagTextArea);
		chatPanel.add(messageTextField);
		
		
		splitPane.setDividerLocation(300);
		splitPane.setResizeWeight(0.1);
		splitPane.setOneTouchExpandable(false);
		splitPane.setEnabled(false);
		
		
		
		settingsButton.addActionListener(this);
		// Build Setting Window
//		settingsWindow.setSize(300, mainWindow.getSize().height);
//		settingsWindow.setLocation(mainWindow.getLocation().x - settingsWindow.getSize().width, mainWindow.getLocation().y);
//		settingsWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		System.out.println("Windows are set");
	}
	
	public void loggedIn(Socket s, ObjectOutputStream oos, ObjectInputStream ois, int user_id, String nickName)
	{
		this.s = s;
		this.oos = oos;
		this.ois = ois;
		this.user_id = user_id;
		this.nick_name = nickName;
		System.out.println(user_id + " " + nickName);
		this.initializeClient();
	}
	
	private void initializeClient()
	{
		// Receive User Lists
		try
		{
			Message lists = (Message) ois.readObject();
			this.onlineUsers = lists.getOnlineUsers();
			this.offlineUsers = lists.getOfflineUsers();
			updateUserList();
		} catch (ClassNotFoundException | IOException e)
		{
			new Client_Error(dim, e.toString());
			return;
		}
		System.out.println("User Lists Received");
		
		// Initialize Message Queue
		msgQueue = new ArrayBlockingQueue<Message>(1);
		
		mainWindow.setTitle("Chat Room - " + user_id);
		
		// Start Receiving from Server
		new Thread(this).start();
		
		// Open Main Window
		mainWindow.setVisible(true);
		System.out.println("client is up");
	}
	
	private void updateUserList()
	{
		if (onlineUsers == null || offlineUsers == null)
		{
			new Client_Error(dim, "Failed Updating User List");
			return;
		}
		
		Set<Integer> onlineKeys = onlineUsers.keySet();
		Set<Integer> offlineKeys = offlineUsers.keySet();
		String[] onlineArray = new String[onlineKeys.size()];
		String[] offlineArray = new String[offlineKeys.size()];
		int i = 0;
		for (int key : onlineKeys)
		{
			if (key == user_id) onlineArray[i] = onlineUsers.get(key) + " (ID: " + key + ", me)";
			else onlineArray[i] = onlineUsers.get(key) + " (ID: " + key + ")";
			i++;
		}
		i = 0;
		for (int key : offlineKeys)
		{
			offlineArray[i] = offlineUsers.get(key) + " (ID: " + key + ")";
			i++;
		}
		Arrays.sort(onlineArray);
		Arrays.sort(offlineArray);
		onlineList.setListData(onlineArray);
		offlineList.setListData(offlineArray);
	}
	
	// Check Message Type
	/**
	 * Check incoming {@code Object} type, return {@code Message} type if it is in this type.
	 * @param msg {@code Object} to be checked
	 * @return same object in {@code Message} type
	 * @throws ClassNotFoundException if the object is not {@code Message} type
	 */
	private Message checkMsgType(Object msg) throws ClassNotFoundException
	{
		if (msg instanceof Message)
		{
			return (Message) msg;
		} else 
		{
			throw new ClassNotFoundException("Invalid Object Type from Server");
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getSource() == settingsButton)
		{
			new Client_Settings(this, dim);
		} else if (ae.getSource() == clearButton)
		{
			onlineList.clearSelection();
			offlineList.clearSelection();
		}
		
	}
	
	// Receiving from Server
	@Override
	public void run()
	{

		while (true)
		{
			String newLine = System.lineSeparator();
			Message msg;
			try
			{
				Object obj = ois.readObject();
				msg = checkMsgType(obj);
			} catch (ClassNotFoundException e)
			{
				new Client_Error(dim, e.toString());
				continue;
			} catch (IOException e) // Connection Error
			{
				new Client_Error(dim, "Connection Error, Please Exit and Login Again\n" + e.toString());
				mainWindow.setTitle("Chat Room - " + user_id + " - OFFLINE");
				return;
			}
			MsgType msgType = msg.getMsgType();
			switch (msgType)
			{
			case MESSAGE:
				messagTextArea.append(newLine + msg.getMsg() + " -" + new Date(msg.getTime()));
				messagTextArea.setCaretPosition(messagTextArea.getDocument().getLength());
				break;
			case USER_LIST:
				this.onlineUsers = msg.getOnlineUsers();
				this.offlineUsers = msg.getOfflineUsers();
				updateUserList();
				break;
			case USER_INFO:
				try
				{
					msgQueue.put(msg);
				} catch (InterruptedException e)
				{
					new Client_Error(dim, e.toString());
				}
				break;
			case LOGOUT:
				System.out.println("LOG OUT by Server: " + msg.getMsg());
				break;
			default:
				new Client_Error(dim, msg.toString());
				break;
			}
			
			
		}
	}
	
	public String getMyInfo() throws IOException, InterruptedException
	{
		oos.writeObject(new Message(null, user_id, MsgType.USER_INFO));
		Message reply = msgQueue.take();
		return reply.getMsg();
	}
	
	
// main() Loader
	public static void main(String[] args)
	{
		new Client_Main("localhost", 1111);
		System.out.println("End of main loader");
	}

}
