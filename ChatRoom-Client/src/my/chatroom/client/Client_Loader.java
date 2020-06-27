package my.chatroom.client;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Client_Loader implements ActionListener
{

	private JFrame		loaderWindow		= new JFrame("Chat Room - Setting Networks");
	private JTextField	serverAddrTextField	= new JTextField("localhost");
	private JTextField	serverPortTextField	= new JTextField("1111");
	private JButton		nextButton			= new JButton("Next");
	private Dimension	dim = Toolkit.getDefaultToolkit().getScreenSize();

	public Client_Loader()
	{
		JPanel panel = new JPanel();
		JLabel addrLabel = new JLabel("Server Address:");
		JLabel portLabel = new JLabel("Port Number:");
		
		loaderWindow.getContentPane().add(panel, "Center");
		loaderWindow.getContentPane().add(nextButton, "South");
		panel.setLayout(new GridLayout(4,1));
		panel.add(addrLabel);
		panel.add(serverAddrTextField);
		panel.add(portLabel);
		panel.add(serverPortTextField);
		
		nextButton.addActionListener(this);
		
		loaderWindow.setSize(300,300);
		loaderWindow.setResizable(false);
		loaderWindow.setLocation(dim.width/2-loaderWindow.getSize().width/2, dim.height/2-loaderWindow.getSize().height/2);
		loaderWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loaderWindow.setVisible(true);
		loaderWindow.getRootPane().setDefaultButton(nextButton);
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getSource() == nextButton)
		{
			String serverAddress = serverAddrTextField.getText().trim();
			if (serverAddress.isEmpty())
			{
				new Client_Error(dim, "Please Enter Server Address. ");
				return;
			}
			try {
				int portNum = Integer.parseInt(serverPortTextField.getText().trim());
				new Client_Main(serverAddress, portNum);
				loaderWindow.dispatchEvent(new WindowEvent(loaderWindow, WindowEvent.WINDOW_CLOSING));
			} catch (Exception e)
			{
				new Client_Error(dim, "Please Enter Port as Numbers. " + e.getMessage());
				return;
			}
		}
		
	}
	
// main() Loader
	public static void main(String[] args)
	{
		new Client_Loader();
	}

}
