package my.chatroom.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import my.chatroom.data.trans.Message;
import my.chatroom.data.trans.MsgType;

public class Client_Settings implements ActionListener
{
	private Client_Main mainClient;
	private Dimension dim;
	private int user_id;

	private JFrame 	settingsWindow		= new JFrame("Chat Room - Settings"); 
	private JButton	myInfoButton		= new JButton("Get My Information");
	private JPanel  resetPanel       	= new JPanel();
	private JTextField nickNameTextField = new JTextField();
	private JButton	nickNameButton		= new JButton("Set My Nick Name");
	private JPasswordField oldPwField	= new JPasswordField();
	private JPasswordField newPw1Field	= new JPasswordField();
	private JPasswordField newPw2Field 	= new JPasswordField();
	private JButton passwordButton		= new JButton("Set New Password");
	private JButton removeButton		= new JButton("Delete My Account");
	private JButton removeButton2		= new JButton("Confirm Delete My Account");
	private JButton	backButton			= new JButton("Back to Settings");
	private JTextArea infoTextArea	= new JTextArea();
	
	
	public Client_Settings(Client_Main main, Dimension dim, int user_id)
	{
		this.mainClient = main;
		this.dim = dim;
		this.user_id = user_id;
		
		settingsWindow.setTitle("Chat Room - " + user_id + " - Settings");
		
		resetPanel.setLayout(new GridLayout(10, 1));
		resetPanel.setBackground(Color.LIGHT_GRAY);
		
		resetPanel.add(new JLabel("Set New Nick Name: "));
		resetPanel.add(nickNameTextField);
		resetPanel.add(nickNameButton);
		resetPanel.add(new JLabel("Enter Current Password: "));
		resetPanel.add(oldPwField);
		resetPanel.add(new JLabel("Enter New Password Twice: "));
		resetPanel.add(newPw1Field);
		resetPanel.add(newPw2Field);
		resetPanel.add(passwordButton);
		resetPanel.add(myInfoButton);
		
		infoTextArea.setBackground(Color.LIGHT_GRAY);
		infoTextArea.setEditable(false);
		
//		nickNameTextField.setBackground(Color.LIGHT_GRAY);
//		oldPwField.setBackground(Color.LIGHT_GRAY);
//		newPw1Field.setBackground(Color.LIGHT_GRAY);
//		newPw2Field.setBackground(Color.LIGHT_GRAY);
		
//		myInfoButton.setOpaque(true);
//		myInfoButton.setBackground(Color.DARK_GRAY);
//		myInfoButton.setForeground(Color.WHITE);
//		nickNameButton.setOpaque(true);
//		nickNameButton.setBackground(Color.DARK_GRAY);
//		nickNameButton.setForeground(Color.WHITE);
//		passwordButton.setOpaque(true);
//		passwordButton.setBackground(Color.DARK_GRAY);
//		passwordButton.setForeground(Color.WHITE);
//		removeButton.setOpaque(true);
//		removeButton.setBackground(Color.DARK_GRAY);
//		removeButton.setForeground(Color.WHITE);
//		backButton.setOpaque(true);
//		backButton.setBackground(Color.DARK_GRAY);
//		backButton.setForeground(Color.WHITE);
		
		myInfoButton.addActionListener(this);
		nickNameButton.addActionListener(this);
		passwordButton.addActionListener(this);
		removeButton.addActionListener(this);
		removeButton2.addActionListener(this);
		backButton.addActionListener(this);
		
		// Set Window
		settingsWindow.setSize(300,400);
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
				new Client_Error(dim, "Error Occured When Fetching Data, Please Try Again. " + e.toString(), user_id);
			}
			
		} else if (ae.getSource() == nickNameButton)
		{
			String newNickName = mainClient.checkNickNameFormat(nickNameTextField.getText());
			if (newNickName == null) return;
			try
			{
				Message reply = mainClient.setNickName(newNickName);
				if (reply.getMsgType() == MsgType.DONE)
				{
					loadInfo("Your new nick name: " + newNickName + " has been set.");
					nickNameTextField.setText("");
				} else 
				{
					new Client_Error(dim, "Error Occured When Setting Nick Name, Please Try Again. ", user_id);
				}
			} catch (IOException | InterruptedException e)
			{
				new Client_Error(dim, "Error Occured When Setting Nick Name, Please Try Again. " + e.toString(), user_id);
			}
			
		} else if (ae.getSource() == passwordButton)
		{
			String oldPw = new String(oldPwField.getPassword());
			if (oldPw.isEmpty()) 
			{
				new Client_Error(dim, "You Must Enter Current Password to Set New Password.", user_id);
				return;
			}
			
			String pw1 = new String(newPw1Field.getPassword());
			String pw2 = new String(newPw2Field.getPassword());
			
			String newPw = mainClient.checkPasswordFormat(pw1, pw2);
			if (newPw == null) return;
			
			try
			{
				Message reply = mainClient.setPassword(oldPw, newPw);
				if (reply.getMsgType() == MsgType.DONE)
				{
					loadInfo("Your new password has been set. Please Login with New Password.");
				} else
				{
					new Client_Error(dim, "Error Occured When Setting Password, Please Try Again. " + reply.getMsg(), user_id);
				}
			} catch (IOException | InterruptedException e)
			{
				new Client_Error(dim, "Error Occured When Setting Password, Please Try Again. " + e.toString(), user_id);
			}
			
		} else if (ae.getSource() == removeButton)
		{
			loadRemove();
			
		} else if (ae.getSource() == removeButton2)
		{
			try
			{
				Message reply = mainClient.removeUser();
				if (reply.getMsgType() == MsgType.DONE)
				{
					loadInfo("Your account has been deleted. Welcome to sign up again.");
				} else
				{
					new Client_Error(dim, "Error Occured When Deleting Account, Please Try Again. " + reply.getMsg(), user_id);
				}
			} catch (IOException | InterruptedException e)
			{
				new Client_Error(dim, "Error Occured When Deleting Account, Please Try Again. " + e.toString(), user_id);
			}
			
		}
		
	}
	
	private void loadMain()
	{
		settingsWindow.getContentPane().removeAll();
		
		settingsWindow.getContentPane().add(resetPanel, "Center");
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
	
	private void loadRemove()
	{
		settingsWindow.getContentPane().removeAll();
		
		settingsWindow.getContentPane().add(backButton, "North");
		settingsWindow.getContentPane().add(infoTextArea, "Center");
		settingsWindow.getContentPane().add(removeButton2, "South");
		infoTextArea.setText("Are you sure about deleting your account?");
		infoTextArea.setFont(new Font("default", Font.PLAIN, 20));
		infoTextArea.setLineWrap(true);
		infoTextArea.setWrapStyleWord(true);
		
		settingsWindow.revalidate();
		settingsWindow.getContentPane().repaint();
	}

}
