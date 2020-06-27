package my.chatroom.client;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.*;

import my.chatroom.data.messages.*;

public final class Client_Login implements ActionListener
{
	// Connection
	private Client_Main			mainClient;
	private Dimension			dim;
	private Socket				s;
	private ObjectOutputStream	oos;
	private ObjectInputStream	ois;
	
	// Login User Info
	private int		user_id;
	private String	password;
	
	// Login GUI Objects
	private JFrame loginWindow = new JFrame("Chat Room - Welcome");
	
	private JTextArea		logInTextArea		= new JTextArea("Welcome to Chat Room");
	private JPanel			loginPanel			= new JPanel();
	private JTextField		user_idTextField	= new JTextField();
	private JPasswordField	passwordTextField	= new JPasswordField();
	private JPanel			buttonPanel			= new JPanel();
	private JButton			loginButton			= new JButton("Log In");
	private JButton			newUserButton		= new JButton("New User? Sign Up");
	
	private JButton			backButton			= new JButton("Back to Log In");
	private JPanel			signUpPanel			= new JPanel();
	private JTextField		nickNameTextField	= new JTextField();
	private JPasswordField	newPw1Field			= new JPasswordField();
	private JPasswordField	newPw2Field			= new JPasswordField();
	private JButton			signUpButton		= new JButton("Sign Up");
	private JTextArea		newUserTextArea		= new JTextArea();
	
// Constructor
	public Client_Login(Client_Main mainClient, Dimension dim, String serverAddress, int serverPort)
	{
		this.mainClient = mainClient;
		this.dim = dim;
		// Connect to Server
		try
		{
			s = new Socket(serverAddress, serverPort);
			oos = new ObjectOutputStream(s.getOutputStream());
		} catch (IOException e)
		{
			new Client_Error(dim, e.getMessage());
			return;
		}
		
		// Setup Panels
		loginPanel.setLayout(new GridLayout(4, 1));
		loginPanel.add(new JLabel("Enter Your User ID: "));
		loginPanel.add(user_idTextField);
		loginPanel.add(new JLabel("Enter Your Password: "));
		loginPanel.add(passwordTextField);
		
		buttonPanel.setLayout(new GridLayout(2, 1));
		buttonPanel.add(loginButton);
		buttonPanel.add(newUserButton);
		
		signUpPanel.setLayout(new GridLayout(5, 1));
		signUpPanel.add(new JLabel("Enter Your Nick Name: "));
		signUpPanel.add(nickNameTextField);
		signUpPanel.add(new JLabel("Enter Your Password Twice: "));
		signUpPanel.add(newPw1Field);
		signUpPanel.add(newPw2Field);
		
		// Setup Text Area
		logInTextArea.setEditable(false);
		logInTextArea.setEnabled(false);
		newUserTextArea.setEditable(false);
		
		// Add Listener
		loginButton.addActionListener(this);
		newUserButton.addActionListener(this);
		backButton.addActionListener(this);
		signUpButton.addActionListener(this);
		
		// Load Main Login Window
		loadLogin();
		
		// Set Main Window
		loginWindow.setSize(300,300);
		loginWindow.setResizable(false);
		loginWindow.setLocation(dim.width/2-loginWindow.getSize().width/2, dim.height/2-loginWindow.getSize().height/2);
		loginWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loginWindow.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getSource() == loginButton) // Log In Processing
		{
			if (user_idTextField.getText().isBlank() || passwordTextField.getPassword().length == 0)
			{
				new Client_Error(dim, "Please Enter Your User ID and Password");
				return;
			}
			try
			{
				user_id = Integer.parseInt(user_idTextField.getText().trim());
			} catch (Exception e)
			{
				new Client_Error(dim, "Invalid User ID " + e.getMessage() + " ID Must be Numbers");
				return;
			}
			password = new String(passwordTextField.getPassword());
			
			System.out.println(user_id + " " + password);
			
			// Send JOIN Message to Server
			String nickName;
			Message joinMsg = new Message(password, user_id, MsgType.JOIN);
			try
			{
				oos.writeObject(joinMsg);
				if (ois == null) ois = new ObjectInputStream(s.getInputStream());
				Object reply = ois.readObject();
				if (reply instanceof Message)
				{
					MsgType msgType = ((Message) reply).getMsgType();
					switch (msgType) 
					{
					case DONE:
						System.out.println("Log In Succeeded");
						nickName = ((Message) reply).getMsg();
						break;
					case REFUSE:
						new Client_Error(dim, "Invalid User ID or Password");
						return;
					default:
						new Client_Error(dim, "Wrong Message Enum Type: " + msgType + " from server");
						return;
					}
				} else
				{
					new Client_Error(dim, "Wrong Message Type from server");
					return;
				}
			} catch (Exception e)
			{
				new Client_Error(dim, e.getMessage());
				return;
			}
			
			// Load Main Chat Room
			mainClient.loggedIn(s, oos, ois, user_id, nickName);
			// Close Self
			loginWindow.dispatchEvent(new WindowEvent(loginWindow, WindowEvent.WINDOW_CLOSING));
			
		} else if (ae.getSource() == newUserButton) // Load Sign Up Panel
		{
			loadSignUp();
		} else if (ae.getSource() == backButton) // Load Login Panel
		{
			loadLogin();
		} else if (ae.getSource() == signUpButton) // Sign Up processing
		{
			String nick_name = nickNameTextField.getText().trim();
			
			nick_name = mainClient.checkNickNameFormat(nick_name);
			if (nick_name == null) return;
			
			String pw1 = new String(newPw1Field.getPassword());
			String pw2 = new String(newPw2Field.getPassword());
			
			String pw = mainClient.checkPasswordFormat(pw1, pw2);
			if (pw == null) return;
			
			System.out.println(nick_name + " " + pw1 + " " + pw2);
			
			// Send ADD_USER Message to Server
			Message joinMsg = new Message(nick_name + " " + pw, 0, MsgType.ADD_USER);
			try
			{
				oos.writeObject(joinMsg);
				if (ois == null) ois = new ObjectInputStream(s.getInputStream());
				Object reply = ois.readObject();
				if (reply instanceof Message)
				{
					MsgType msgType = ((Message) reply).getMsgType();
					switch (msgType) 
					{
					case DONE:
						System.out.println("Sign Up Succeeded");
						user_id = ((Message) reply).getUser_id();
						nickNameTextField.setText("");
						newPw1Field.setText("");
						newPw2Field.setText("");
						loadNewUser();
						break;
					case REFUSE:
						new Client_Error(dim, "Sign Up Failed, Please Try Again Later");
						return;
					default:
						new Client_Error(dim, "Wrong Message Enum Type: " + msgType + " from server");
						return;
					}
				} else
				{
					new Client_Error(dim, "Wrong Message Type from server");
					return;
				}
			} catch (Exception e)
			{
				new Client_Error(dim, e.getMessage());
				return;
			}
		} 
		
	}
	
// Load Login Window
	private void loadLogin()
	{
		loginWindow.getContentPane().removeAll();
		
		loginWindow.getContentPane().add(logInTextArea, "North");
		loginWindow.getContentPane().add(loginPanel, "Center");
		loginWindow.getContentPane().add(buttonPanel,"South");
		// Preload user_id
		if (user_id != 0) user_idTextField.setText(String.valueOf(user_id));
		
		loginWindow.getRootPane().setDefaultButton(loginButton);
		
		loginWindow.revalidate();
		loginWindow.getContentPane().repaint();
	}
	
// Load Sign Up Window
	private void loadSignUp()
	{
		loginWindow.getContentPane().removeAll();
		
		loginWindow.getContentPane().add(backButton,"North");
		loginWindow.getContentPane().add(signUpPanel, "Center");
		loginWindow.getContentPane().add(signUpButton,"South");
		
		loginWindow.getRootPane().setDefaultButton(signUpButton);
		
		loginWindow.revalidate();
		loginWindow.getContentPane().repaint();
	}
	
// Load New User Window
	private void loadNewUser()
	{
		loginWindow.getContentPane().removeAll();
		
		String newLine = System.lineSeparator();
		newUserTextArea.setText("Sign Up Successful!" + newLine + "Your User ID is: " + user_id);
		newUserTextArea.setFont(new Font("default", Font.PLAIN, 20));
		loginWindow.getContentPane().add(backButton,"North");
		loginWindow.getContentPane().add(newUserTextArea, "Center");
		
		loginWindow.revalidate();
		loginWindow.getContentPane().repaint();
	}
	
// Save Login info on disk
//	private void saveToDisk()
//	{
//		// Save User ID and Password
//	}

}
