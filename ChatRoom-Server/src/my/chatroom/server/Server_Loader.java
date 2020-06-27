package my.chatroom.server;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import my.chatroom.server.exception.FatalDataBaseException;

public class Server_Loader implements ActionListener
{
	public class JTextAreaOutputStream extends OutputStream
	{
		private final JTextArea textArea;

		public JTextAreaOutputStream (JTextArea textArea)
		{
			if (textArea == null) throw new IllegalArgumentException ("Destination is null");
			this.textArea = textArea;
		}

		@Override
		public void write(byte[] buffer, int offset, int length) throws IOException
		{
			final String text = new String (buffer, offset, length);
			SwingUtilities.invokeLater(new Runnable ()
			{
				@Override
				public void run() 
				{
					textArea.append (text);
				}
			});
		}

		@Override
		public void write(int b) throws IOException
		{
			write (new byte [] {(byte)b}, 0, 1);
		}
	}
	
	private Dimension	dim = Toolkit.getDefaultToolkit().getScreenSize();
	private JFrame		serverWindow	= new JFrame("Chat Room Server");
	
	private JTextField	DBpathTextField	= new JTextField("/Users/hjl/git/ChatRoom/ChatRoom-Server/database/QuoteDB");
	private JTextField 	portNumField	= new JTextField("1111");
	private JButton		launchButton	= new JButton("Launch Server");
	
	private JTextArea	textArea		= new JTextArea();
	private JScrollPane	textAreaScroll	= new JScrollPane(textArea);
	private JTextArea	errorArea		= new JTextArea();
	private JScrollPane	errorAreaScroll	= new JScrollPane(errorArea);
	private JButton		shutdownButton	= new JButton("Shutdown");
	
	private Server_Main mainServer;
	private String		DBpath;
	private int			portNum;
	
	public Server_Loader()
	{
		serverWindow.setSize(500,300);
		serverWindow.setMinimumSize(new Dimension(300, 300));
		serverWindow.setLocation(dim.width/2-serverWindow.getSize().width/2, dim.height/2-serverWindow.getSize().height/2);
		serverWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JTextAreaOutputStream out = new JTextAreaOutputStream(textArea);
		JTextAreaOutputStream err = new JTextAreaOutputStream(errorArea);
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
        
        JPanel panel = new JPanel();
        JLabel DBpathLabel = new JLabel("DataBase Path:");
        JLabel portLabel = new JLabel("Port Number:");
        panel.setLayout(new GridLayout(4,1));
        panel.add(DBpathLabel);
        panel.add(DBpathTextField);
        panel.add(portLabel);
        panel.add(portNumField);
        
        launchButton.addActionListener(this);
        
        serverWindow.getContentPane().add(panel, "Center");
        serverWindow.getContentPane().add(launchButton, "South");
        serverWindow.getRootPane().setDefaultButton(launchButton);
		serverWindow.setVisible(true);
	}
	
	private void loadServerWindow()
	{
		serverWindow.getContentPane().removeAll();
		serverWindow.revalidate();
		serverWindow.getContentPane().repaint();
		serverWindow.setSize(600,600);
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2,1));
		panel.add(textAreaScroll);
		panel.add(errorAreaScroll);
		
		serverWindow.getContentPane().add(panel, "Center");
		serverWindow.getContentPane().add(shutdownButton, "South");
		
		textAreaScroll.setAutoscrolls(true);
		textArea.setEditable(false);
		errorAreaScroll.setAutoscrolls(true);
		errorArea.setEditable(false);
		errorArea.setForeground(Color.RED);
		
		shutdownButton.addActionListener(this);
		
		serverWindow.revalidate();
		serverWindow.getContentPane().repaint();
		
		try
		{
			mainServer = new Server_Main(DBpath, portNum);
		} catch (FatalDataBaseException e)
		{
			e.printStackTrace();
			shutdownButton.setText("Error Occurred");
			shutdownButton.setEnabled(false);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getSource() == launchButton)
		{
			DBpath = DBpathTextField.getText().trim();
			if (DBpath.isEmpty())
			{
				DBpathTextField.setText("Please Enter DataBase Path");
				return;
			}
			try {
				portNum = Integer.parseInt(portNumField.getText().trim());
			} catch (Exception e)
			{
				portNumField.setText("Please Enter Port Number");
				return;
			}
			loadServerWindow();
		} else if (ae.getSource() == shutdownButton)
		{
			System.out.println("Main Loader: is Shutdown Normally: " + mainServer.serverShutdown(1));
			shutdownButton.setText("Shutted Down");
			shutdownButton.setEnabled(false);
		}
		
	}

	public static void main(String[] args)
	{
		new Server_Loader();
	}

}
