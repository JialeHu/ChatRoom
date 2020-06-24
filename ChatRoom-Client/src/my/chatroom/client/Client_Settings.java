package my.chatroom.client;

import java.awt.Dimension;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class Client_Settings
{
	private ObjectInputStream  ois;
	private ObjectOutputStream oos;

	private JFrame 	settingsWindow		= new JFrame("Chat Room - Settings"); 
	private JButton	myInfoButton		= new JButton("Get My Information");
	private JTextField nickNameTextField = new JTextField();
	private JButton	nickNameButton		= new JButton("Set My Nick Name");
	private JPasswordField	newPw1Field	= new JPasswordField();
	private JPasswordField	newPw2Field = new JPasswordField();
	private JButton passwordButton		= new JButton("Set New Password");
	private JButton removeButton		= new JButton("Delete My Account");
	private JButton	backButton			= new JButton("Back to Settings");
	
	
	public Client_Settings(ObjectInputStream ois, ObjectOutputStream oos, Dimension dim)
	{
		this.ois = ois;
		this.oos = oos;
		
		
		// Set Window
		settingsWindow.setSize(300,300);
		settingsWindow.setResizable(false);
		settingsWindow.setLocation(dim.width/2-settingsWindow.getSize().width/2, dim.height/2-settingsWindow.getSize().height/2);
		settingsWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		settingsWindow.setVisible(true);
	}

}
