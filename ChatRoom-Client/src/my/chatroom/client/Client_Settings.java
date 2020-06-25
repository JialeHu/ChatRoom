package my.chatroom.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Client_Settings implements ActionListener
{
	private Client_Main mainClient;
	private Dimension dim;

	private JFrame 	settingsWindow		= new JFrame("Chat Room - Settings"); 
	private JButton	myInfoButton		= new JButton("Get My Information");
	private JPanel  resetPanel       	= new JPanel();
	private JTextField nickNameTextField = new JTextField();
	private JButton	nickNameButton		= new JButton("Set My Nick Name");
	private JPasswordField newPw1Field	= new JPasswordField();
	private JPasswordField newPw2Field 	= new JPasswordField();
	private JButton passwordButton		= new JButton("Set New Password");
	private JButton removeButton		= new JButton("Delete My Account");
	private JButton	backButton			= new JButton("Back to Settings");
	private JTextArea infoTextArea	= new JTextArea();
	
	
	public Client_Settings(Client_Main main, Dimension dim)
	{
		this.mainClient = main;
		this.dim = dim;
		
		resetPanel.setLayout(new GridLayout(5, 1));
		resetPanel.setBackground(Color.DARK_GRAY);
		
		infoTextArea.setBackground(Color.LIGHT_GRAY);
		
		nickNameTextField.setBackground(Color.LIGHT_GRAY);
		newPw1Field.setBackground(Color.LIGHT_GRAY);
		newPw2Field.setBackground(Color.LIGHT_GRAY);
		
		myInfoButton.setOpaque(true);
		myInfoButton.setBackground(Color.DARK_GRAY);
		myInfoButton.setForeground(Color.WHITE);
		nickNameButton.setOpaque(true);
		nickNameButton.setBackground(Color.DARK_GRAY);
		nickNameButton.setForeground(Color.WHITE);
		passwordButton.setOpaque(true);
		passwordButton.setBackground(Color.DARK_GRAY);
		passwordButton.setForeground(Color.WHITE);
		removeButton.setOpaque(true);
		removeButton.setBackground(Color.DARK_GRAY);
		removeButton.setForeground(Color.WHITE);
		backButton.setOpaque(true);
		backButton.setBackground(Color.DARK_GRAY);
		backButton.setForeground(Color.WHITE);
		
		myInfoButton.addActionListener(this);
		nickNameButton.addActionListener(this);
		passwordButton.addActionListener(this);
		removeButton.addActionListener(this);
		backButton.addActionListener(this);
		
		// Set Window
		settingsWindow.setSize(300,300);
		settingsWindow.setResizable(false);
		settingsWindow.setLocation(dim.width/2-settingsWindow.getSize().width/2, dim.height/2-settingsWindow.getSize().height/2);
		settingsWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		loadMain();
		
		settingsWindow.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getSource() == backButton) 
		{
			loadMain();
		} else if (ae.getSource() == myInfoButton)
		{
			try
			{
				String myInfo = mainClient.getMyInfo();
				loadInfo(myInfo);
			} catch (IOException | InterruptedException e)
			{
				new Client_Error(dim, "Error Occured When Fetching Data, Please Try Again. " + e.toString());
			}
		} else if (ae.getSource() == nickNameButton)
		{
			loadInfo("nick name button pushed");
		}
		
	}
	
	private void loadMain()
	{
		settingsWindow.getContentPane().removeAll();
		
		resetPanel.add(nickNameTextField);
		resetPanel.add(nickNameButton);
		resetPanel.add(newPw1Field);
		resetPanel.add(newPw2Field);
		resetPanel.add(passwordButton);
		
		settingsWindow.getContentPane().add(resetPanel, "North");
		settingsWindow.getContentPane().add(myInfoButton, "Center");
		settingsWindow.getContentPane().add(removeButton, "South");
		
		settingsWindow.revalidate();
		settingsWindow.getContentPane().repaint();
	}
	
	private void loadInfo(String info)
	{
		settingsWindow.getContentPane().removeAll();
		
		settingsWindow.getContentPane().add(backButton, "North");
		settingsWindow.getContentPane().add(infoTextArea, "Center");
		infoTextArea.setText(info);
		infoTextArea.setFont(new Font("default", Font.PLAIN, 20));
		infoTextArea.setLineWrap(true);
		infoTextArea.setWrapStyleWord(true);
		
		settingsWindow.revalidate();
		settingsWindow.getContentPane().repaint();
	}

}
