package my.chatroom.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import my.chatroom.data.trans.*;

public final class Client_Main implements ActionListener, ListSelectionListener, FocusListener, Runnable
{
	// Connection
	@SuppressWarnings("unused") // Socket Unused, Prevent being GC
	private Socket				s;
	private ObjectInputStream	ois;
	private ObjectOutputStream	oos;
	
	// User Info
	private int		user_id;
	private String	nick_name;

	// User List
	private HashMap<Integer, String>	onlineUsers;
	private HashMap<Integer, String>	offlineUsers;
	private List<Integer>				onlineIDs;
	private List<Integer>				offlineIDs;
	
	// Message Queue
	private BlockingQueue<Message>	msgQueue;
	
	// Screen info
	private Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	
	// User List GUI Objects
	private JButton			settingsButton		= new JButton("Settings");
	private JButton			clearButton			= new JButton("Clear Selections");
	private JPanel			leftPanel			= new JPanel();
	private JLabel			onlineLabel			= new JLabel("Online Users");
	private JLabel			offlineLabel		= new JLabel("Offline Users");
	private JPanel			listPanel			= new JPanel();
	private JList<String>	onlineList			= new JList<String>();
	private JList<String>	offlineList			= new JList<String>();
	private JScrollPane		onlineScrollPane	= new JScrollPane(onlineList);
	private JScrollPane		offlineScrollPane	= new JScrollPane(offlineList);
	
	// Chat GUI Objects
	private JPanel		chatPanel			= new JPanel();
	private JTextArea	messageTextArea		= new JTextArea();
	private JScrollPane	messageScrollPane	= new JScrollPane(messageTextArea);
	private String		messageTextFieldStr	= "Send Messages to Everyone";
	private JTextField	messageTextField	= new JTextField(messageTextFieldStr);
	private JButton		sendButton			= new JButton("Send");
	
	// Window GUI Objects
	private JFrame		mainWindow	= new JFrame("Chat Room"); 
	private JSplitPane	splitPane	= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, chatPanel);
	private String		newLine		= System.lineSeparator();

// Constructor
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
		
		// Log in or Sign up, LoggedIn() will be called after logged in.
		new Client_Login(this, dim, serverAddress, serverPort);
		
		// Build Main Window
		mainWindow.setSize(700,600);
		mainWindow.setMinimumSize(new Dimension(400, 300));
		mainWindow.setLocation(dim.width/2-mainWindow.getSize().width/2, dim.height/2-mainWindow.getSize().height/2);
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainWindow.getContentPane().add(splitPane);
		
		// Build Split Panel
		splitPane.setBackground(Color.LIGHT_GRAY);	
		splitPane.setDividerLocation(300);
		splitPane.setResizeWeight(0.1);
		splitPane.setOneTouchExpandable(false);
		splitPane.setEnabled(false);
		
		// Build Left Panel
		Color leftPanelBackgroundColor = Color.DARK_GRAY;
		Color leftPanelForegroundColor = Color.WHITE;
		
		leftPanel.setLayout(new BorderLayout());
		leftPanel.setMinimumSize(new Dimension(250, 250));
		leftPanel.setBackground(leftPanelBackgroundColor);
		leftPanel.add(settingsButton, BorderLayout.NORTH);
		leftPanel.add(listPanel, BorderLayout.CENTER);
		leftPanel.add(clearButton, BorderLayout.SOUTH);
		
		// Left List Panel
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(leftPanelBackgroundColor);
		listPanel.add(onlineLabel);
		listPanel.add(onlineScrollPane);
		listPanel.add(offlineLabel);
		listPanel.add(offlineScrollPane);
		
		onlineLabel.setBackground(leftPanelBackgroundColor);
		onlineLabel.setForeground(leftPanelForegroundColor);
		onlineLabel.setOpaque(true);
		onlineLabel.setFont(new Font("default", Font.PLAIN, 18));
		
		onlineScrollPane.setBackground(leftPanelBackgroundColor);
		onlineScrollPane.getVerticalScrollBar().setBackground(leftPanelBackgroundColor);
		onlineScrollPane.getHorizontalScrollBar().setBackground(leftPanelBackgroundColor);
		
		onlineList.setBackground(leftPanelBackgroundColor);
		onlineList.setForeground(Color.LIGHT_GRAY);
		onlineList.setFont(new Font("default", Font.PLAIN, 20));
		onlineList.setLayoutOrientation(JList.VERTICAL);
		onlineList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		offlineLabel.setBackground(leftPanelBackgroundColor);
		offlineLabel.setForeground(leftPanelForegroundColor);
		offlineLabel.setOpaque(true);
		offlineLabel.setFont(new Font("default", Font.PLAIN, 18));
		
		offlineScrollPane.setBackground(leftPanelBackgroundColor);
		offlineScrollPane.getVerticalScrollBar().setBackground(leftPanelBackgroundColor);
		offlineScrollPane.getHorizontalScrollBar().setBackground(leftPanelBackgroundColor);
		
		offlineList.setBackground(leftPanelBackgroundColor);
		offlineList.setForeground(Color.LIGHT_GRAY);
		offlineList.setFont(new Font("default", Font.PLAIN, 20));
		offlineList.setLayoutOrientation(JList.VERTICAL);
		offlineList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		// Build Right Panel
		JPanel textFieldPanel = new JPanel();
		
		chatPanel.setBackground(Color.LIGHT_GRAY);
		chatPanel.setLayout(new BorderLayout());
		chatPanel.add(messageScrollPane, BorderLayout.CENTER);
		chatPanel.add(textFieldPanel, BorderLayout.SOUTH);
		
		messageTextArea.setText("Select User(s) to Send Private Messages."+newLine+"Clear Selections to Send Public Messsages."+newLine);
		messageTextArea.setEditable(false);
		messageTextArea.setFont(new Font("default", Font.PLAIN, 15));
		messageTextArea.setLineWrap(true);
		messageTextArea.setWrapStyleWord(true);

		textFieldPanel.setBackground(Color.LIGHT_GRAY);	
		textFieldPanel.setLayout(new BoxLayout(textFieldPanel, BoxLayout.X_AXIS));
		textFieldPanel.add(messageTextField);
		textFieldPanel.add(sendButton);
		
		// Action Listeners
		settingsButton.addActionListener(this);
		clearButton.addActionListener(this);
		sendButton.addActionListener(this);
		messageTextField.addActionListener(this);
		// List Selection Listeners
		onlineList.addListSelectionListener(this);
		offlineList.addListSelectionListener(this);
		// Focus Listeners
		messageTextField.addFocusListener(this);
		
		System.out.println("End of main constructor");
	}
	
// Logged in API
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
	
// Construct after logged in
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
			new Client_Error(dim, e.toString(), user_id);
			return;
		}
		System.out.println("User Lists Received");
		
		// Initialize Message Queue
		msgQueue = new ArrayBlockingQueue<Message>(1);
		
		// Start Receiving from Server
		new Thread(this).start();
		
		// Open Main Window
		mainWindow.setVisible(true);
		System.out.println("client is up");
	}
	
// Action Listener
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getSource() == settingsButton) // Open Settings Module
		{
			new Client_Settings(this, dim, user_id);
		} else if (ae.getSource() == clearButton) // Clear All User List Selections
		{
			onlineList.clearSelection();
			offlineList.clearSelection();
		} else if (ae.getSource() == sendButton || ae.getSource() == messageTextField) // Send Message
		{
			String msgStr = messageTextField.getText();
			if (msgStr.isBlank()) return;
			Message msg;
			if (onlineList.isSelectionEmpty() && offlineList.isSelectionEmpty()) // Public Messages
			{
				msg = new Message(msgStr, user_id, (int[]) null);
			} else // Private Messages
			{
				int[] recipients = getSelectedUsers();
				if (recipients == null || recipients.length == 0) // Error in getSelectedUsers();
				{
					new Client_Error(dim, "Failed to Get Selected Users, Please Try Again.", user_id);
					return;
				}
				if (recipients.length == 1 && recipients[0] == user_id) return; // If only self is selected
				msg = new Message(msgStr, user_id, recipients);
			}
			// Send
			try
			{
				oos.writeObject(msg);
			} catch (IOException e)
			{
				new Client_Error(dim, "Failed to Send Message, Please Try Again." + e.toString(), user_id);
				return;
			}
			messageTextField.setText("");
		}
		
	}
	
// List Listener
	@Override
	public void valueChanged(ListSelectionEvent lse)
	{
		if (lse.getSource() == onlineList || lse.getSource() == offlineList) // Set instruction string on messageTextField
		{
			if (onlineList.isSelectionEmpty() && offlineList.isSelectionEmpty()) // Public Messages
			{
				messageTextFieldStr = "Send Messages to Everyone";
			} else // Private Messages
			{
				int[] recipients = getSelectedUsers();
				if (recipients == null || recipients.length == 0) // Error in getSelectedUsers();
				{
					new Client_Error(dim, "Failed to Get Selected Users, Please Try Again.", user_id);
					return;
				}
				if (recipients.length == 1 && recipients[0] == user_id) // If only self is selected
				{
					messageTextFieldStr = "Cannot Send Messages to Yourself";
				} else
				{
					messageTextFieldStr = "Send Messages to: " + Arrays.toString(recipients);
				}
			}
			messageTextField.setText(messageTextFieldStr);
		}
		
	}
	
// Focus Listener
	@Override
	public void focusGained(FocusEvent fe)
	{
		if (fe.getSource() == messageTextField) // Text Field is Focused
		{
			messageTextField.setText("");
		}
		
	}

	@Override
	public void focusLost(FocusEvent fe)
	{
		if (fe.getSource() == messageTextField) // Text Field Lost Focus
		{
			messageTextField.setText(messageTextFieldStr);
		}
		
	}
	
// Receiving from Server
	@Override
	public void run()
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm MMM dd");
		
		while (true)
		{
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
				new Client_Error(dim, "Connection Interrupted, Please Exit and Login Again\n" + e.toString(), user_id);
				mainWindow.setTitle("Chat Room - " + user_id + " - OFFLINE");
				return;
			}
			MsgType msgType = msg.getMsgType();
			switch (msgType)
			{
			case MESSAGE:
				int[] recipients = msg.getRecipients();
				String reciStr = (recipients == null) ? "Everyone" : Arrays.toString(recipients);
				String dateStr = newLine + " -" + dateFormat.format(new Date(msg.getTime())) + "- ";
				String msgStr = newLine + getNickName(msg.getUser_id()) + " (To: " + reciStr + "): " + msg.getMsg() + newLine;
				messageTextArea.append(dateStr);
				messageTextArea.append(msgStr);
				messageTextArea.setCaretPosition(messageTextArea.getDocument().getLength());
				break;
			case USER_LIST:
				this.onlineUsers = msg.getOnlineUsers();
				this.offlineUsers = msg.getOfflineUsers();
				updateUserList();
				break;
			case USER_INFO:
			case DONE:
			case REFUSE:
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
				new Client_Error(dim, msg.toString(), user_id);
				break;
			}
			
		}
	}

// Update User List
	private void updateUserList()
	{
		if (onlineUsers == null || offlineUsers == null)
		{
			new Client_Error(dim, "Failed Updating User List", user_id);
			return;
		}
		
		// Convert IDs to list for JList indexing
		onlineIDs = new ArrayList<Integer>(onlineUsers.keySet());
		offlineIDs = new ArrayList<Integer>(offlineUsers.keySet());
		// Sort ID list based on NickName
		Collections.sort(onlineIDs, (k1, k2) -> {return onlineUsers.get(k1).compareTo(onlineUsers.get(k2));});
		Collections.sort(offlineIDs, (k1, k2) -> {return offlineUsers.get(k1).compareTo(offlineUsers.get(k2));});
		// String[] to be added to JList
		String[] onlineArray = new String[onlineIDs.size()];
		String[] offlineArray = new String[offlineIDs.size()];
		int i = 0;
		for (int key : onlineIDs)
		{
			if (key == user_id) 
			{
				String myNickName = onlineUsers.get(key);
				nick_name = myNickName;
				mainWindow.setTitle("Chat Room - " + user_id + ": " + nick_name);
				onlineArray[i] = myNickName + " (ID: " + key + ", me)";
			}
			else onlineArray[i] = onlineUsers.get(key) + " (ID: " + key + ")";
			i++;
		}
		i = 0;
		for (int key : offlineIDs)
		{
			offlineArray[i] = offlineUsers.get(key) + " (ID: " + key + ")";
			i++;
		}
		// Update JList
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

// Get Selected Users on Lists
	private int[] getSelectedUsers()
	{		
		int[] onlineIdx = onlineList.getSelectedIndices();
		int[] offlineIdx = offlineList.getSelectedIndices();
		int[] selectedIDs = new int[onlineIdx.length + offlineIdx.length];
		int i = 0;
		try {
			for (int idx : onlineIdx) selectedIDs[i++] = onlineIDs.get(idx);
			for (int idx : offlineIdx) selectedIDs[i++] = offlineIDs.get(idx);
		} catch (Exception e)
		{
			return null;
		}
		return selectedIDs;
	}
	
// Get nick name API
	public String getNickName(int user_id)
	{
		String nickName = onlineUsers.get(user_id);
		if (nickName == null) nickName = offlineUsers.get(user_id);
		return nickName;
	}
	
// Get my info API
	public String getMyInfo() throws IOException, InterruptedException
	{
		oos.writeObject(new Message(null, user_id, MsgType.USER_INFO));
		Message reply = msgQueue.take();
		return reply.getMsg();
	}
	
// Set my nick name API
	public Message setNickName(String newNickName) throws IOException, InterruptedException
	{
		oos.writeObject(new Message(newNickName, user_id, MsgType.SET_NICKNAME));
		Message reply = msgQueue.take();
		return reply;
	}
	
// Set my password API
	public Message setPassword(String oldPw, String newPw) throws IOException, InterruptedException
	{
		oos.writeObject(new Message(oldPw + " " + newPw, user_id, MsgType.SET_PASSWORD));
		Message reply = msgQueue.take();
		return reply;
	}
	
// Delete my account API
	public Message removeUser() throws IOException, InterruptedException
	{
		oos.writeObject(new Message(MsgType.RM_USER, null));
		Message reply = msgQueue.take();
		return reply;
	}

// Check Format of new nick name API
	public String checkNickNameFormat(String nickName)
	{
		nickName = nickName.trim();
		if (nickName.length() == 0)
		{
			new Client_Error(dim, "Please Enter Nick Name", user_id);
			return null;
		} else if (nickName.length() > 30)
		{
			new Client_Error(dim, "Nick Name Cannot Exceed 30 Characters", user_id);
			return null;
		}
		return nickName;
	}
	
// Check format of new password API
	public String checkPasswordFormat(String pw1, String pw2)
	{
		if (pw1.length() < 6 || pw1.length() > 30)
		{
			new Client_Error(dim, "Password Length Must Be 6-30 Characters", user_id);
			return null;
		} else if (!pw1.equals(pw2))
		{
			new Client_Error(dim, "Passwords are Not Consistent", user_id);
			return null;
		}
		return pw1;
	}
	
	
// main() Loader
	public static void main(String[] args)
	{
		new Client_Main("localhost", 1111);
		System.out.println("End of main loader");
	}

}
