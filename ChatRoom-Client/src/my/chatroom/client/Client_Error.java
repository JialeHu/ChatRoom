package my.chatroom.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JTextArea;

public class Client_Error
{

	// Error GUI objects
	protected JFrame        errorWindow 	= new JFrame("Chat Room - Error Message"); 
	protected JTextArea 	errorTextArea 	= new JTextArea();
	
	public Client_Error(Dimension dim, String errorMsg)
	{
		errorWindow.add(errorTextArea);
		errorTextArea.setEditable(false);
		errorTextArea.setText(errorMsg);
		errorTextArea.setBackground(Color.pink);
		errorTextArea.setFont(new Font("default", Font.PLAIN, 20));
		errorTextArea.setLineWrap(true);
		errorTextArea.setWrapStyleWord(true);
		errorWindow.setSize(300,200);
		errorWindow.setLocation(dim.width/2-errorWindow.getSize().width/2, dim.height/2-errorWindow.getSize().height/2);
		
		errorWindow.setResizable(false);
		errorWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		errorWindow.setVisible(true);
	}

}
