// Jiale Hu

import java.net.*; // find Socket
import java.io.*; // find Object Stream
import java.util.*; // find Arrays
import javax.swing.*; // find JFrame
import java.awt.*; // find Color and Font
import java.awt.event.*; // find ActionEvent

public class ChatRoomClient implements ActionListener 
{

// Instance Variables
	
	// Connection
	Socket s;
	ObjectInputStream  ois;
	ObjectOutputStream oos;

	// Main GUI objects
	JFrame     	window    			= new JFrame("Lab2 Chat Room Client"); 
	JTextField 	errMsgTextField 	= new JTextField("Client-side error messages will be displayed here.");
	JTextField 	textField			= new JTextField("Enter a PUBLIC message to be sent to ALL clients");
	JTextArea  	textArea  			= new JTextArea("Received chat messages will be shown here.\n");
	JScrollPane messageScrollPane 	= new JScrollPane(textArea);
	
	// Whos-in window GUI objects
	JFrame      	whosInWindow      	= new JFrame("Who's <-IN and OUT->"); // set title bar text
	JTextField  	privateTextField  	= new JTextField("Enter a PRIVATE message to be sent to SELECTED clients");
	JButton     	clearButton       	= new JButton("Clear All Selections of Clients");
	JPanel      	middlePanel       	= new JPanel();
	JList<String> 	whosInList      	= new JList<String>();
	JList<String> 	whosNotInList   	= new JList<String>();
	JScrollPane 	whosInScrollPane  	= new JScrollPane(whosInList);
	JScrollPane 	whosNotScrollPane 	= new JScrollPane(whosNotInList);

	// User
	String chatName;

	
// Constructor
	public ChatRoomClient(String serverAddress, String chatName, String password, String newPassword) 
	{
		// Local Variable
		int serverPort = 5555; 
		// Assign Instance Variable
		this.chatName = chatName;

		if (serverAddress.contains(" ") || chatName.contains(" ")) 
		{
			throw new IllegalArgumentException("Arguments cannot contain embeded blanks");
		}

		try // Connect to server
		{ 
			s = new Socket(serverAddress, serverPort);

			oos = new ObjectOutputStream(s.getOutputStream());

			if (newPassword.length() == 0) // Normal Login
			{
				oos.writeObject(chatName + " " + password);
			}
			else // Request Password Change
			{
				oos.writeObject(chatName + " " + password + " " + newPassword);
			}

			ois = new ObjectInputStream(s.getInputStream());
			String reply = (String) ois.readObject();

			if (reply.startsWith("Welcome")) 
			{
				System.out.println(reply);
			} 
			else 
			{
				throw new IllegalArgumentException(reply);
			}

		} 
		catch(Exception e) 
		{
			System.out.println("Error communicating with the server at " + serverAddress + " " + serverPort);
			String errorMessage = e.getMessage(); 
			throw new IllegalArgumentException(errorMessage);
		}

		// Build Main GUI		
		window.getContentPane().add(errMsgTextField,"North");
		window.getContentPane().add(messageScrollPane, "Center");
		window.getContentPane().add(textField,"South");

		window.setSize(500,400);   // width,height
		window.setLocation(500,100); // x,y (x is "to the right of the left margin, y is "down-from-the-top")
		
		errMsgTextField.setEditable(false); // keep user from changing errMsg
		errMsgTextField.setBackground(Color.white);
		textField.setBackground(Color.yellow);
		textArea.setFont(new Font("default", Font.PLAIN, 15)); // 20 is the font size
		textArea.setEditable(false); // Keep cursor out
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);

		window.setTitle(chatName + "'s CHAT ROOM");
		window.setVisible(true);   // show/hide window
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // terminate the program when close window

		textField.addActionListener(this); // give address of MyFirstGUI program to the textfield program.
		
		// build the whosInWindow
		whosInWindow.getContentPane().add(clearButton,     "North");
		whosInWindow.getContentPane().add(middlePanel,     "Center");
		whosInWindow.getContentPane().add(privateTextField,"South");
		
		whosInWindow.setSize(300,400);   // width,height - same height as chat window
		whosInWindow.setLocation(200,100); // x,y (x is "to the right of the left margin, y is "down-from-the-top")
		
		middlePanel.setLayout(new GridLayout(1,2)); // 1 row, 2 cols
		middlePanel.add(whosInScrollPane);  // left
		middlePanel.add(whosNotScrollPane); // right
		
		clearButton.setBackground(Color.white);
		privateTextField.setBackground(Color.yellow);
		
		whosInWindow.setVisible(true);   // show it
		whosInWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);//user can iconify window to get it off the screen.
		
		privateTextField.addActionListener(this);
		clearButton.addActionListener(this);
		
		// For Mac
		try 
		{
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		} 
		catch(Exception e) 
		{
			System.out.println(e);
			return;
		}

	}

	
// actionPerformed()
	public void actionPerformed(ActionEvent ae) 
	{
		errMsgTextField.setText("");                // remove last error message
		errMsgTextField.setBackground(Color.white); // from the screen.
		
		if (ae.getSource() == textField)
		{
			if (!whosInList.isSelectionEmpty() || !whosNotInList.isSelectionEmpty()) // Not Empty Recipients Selections
			{
				errMsgTextField.setText("Clear PRIVATE message recipient(s) before sending PUBLIC messages.");
				errMsgTextField.setBackground(Color.pink); 
				return; 
			}
			
			String chatMessageToSend = textField.getText().trim();
			
			if (chatMessageToSend.length() == 0) // Blank Messages.
			{
				errMsgTextField.setText("No PUBLIC message was entered to send.");
				errMsgTextField.setBackground(Color.pink); 
		        return; // ignore blank messages.
			}
			textField.setText(""); // clear input area
			
			try 
			{
				oos.writeObject(chatMessageToSend);
			} 
			catch(Exception e) {} // do nothing here! (ois.readObject() will be getting the same error)
		}
		
		if (ae.getSource() == privateTextField)
		{
			if (whosInList.isSelectionEmpty() && whosNotInList.isSelectionEmpty()) // Empty Recipients Selections
			{
				errMsgTextField.setText("No PRIVATE message recipient selected.");
				errMsgTextField.setBackground(Color.pink); 
				return; 
			}
			
			String privateMessage = privateTextField.getText().trim();
			
			System.out.println("PRIVATE message entered: '" + privateMessage + "'");

			if (privateMessage.length() == 0) // Blank Messages.
			{
				errMsgTextField.setText("No PRIVATE message was entered to send.");
				errMsgTextField.setBackground(Color.pink); 
				return; // ignore blank messages.
			}
			privateTextField.setText(""); // clear input area
			
			// Get and Send Selection List:
			// WhosInList
			String[] privateRecipientsArray = whosInList.getSelectedValuesList().toArray(new String[0]);
			if (privateRecipientsArray.length > 0) //this list had some selections.
			{                                                                   
				String[] privateMessageArray = new String[privateRecipientsArray.length+1]; 
				privateMessageArray[0] = privateMessage;             // put the message in slot 0                             
				for (int n = 1; n < privateMessageArray.length; n++) // add recipient names in remaining slots                
					privateMessageArray[n] = privateRecipientsArray[n-1];                 
				try {oos.writeObject(privateMessageArray);} // send save msg + save recipients array to server
				catch(Exception e) {System.out.println("Error sending saveMessageArray to server");}
				System.out.println("Sending a privateMessageArray to server: " + Arrays.toString(privateMessageArray));
			}       
			// WhosNotInList
			String[] saveRecipientsArray = whosNotInList.getSelectedValuesList().toArray(new String[0]);
			if (saveRecipientsArray.length > 0) //this list had some selections.
			{                                                                   
				String[] saveMessageArray = new String[saveRecipientsArray.length+1]; 
				saveMessageArray[0] = privateMessage;             // put the message in slot 0                             
				for (int n = 1; n < saveMessageArray.length; n++) // add recipient names in remaining slots                
					saveMessageArray[n] = saveRecipientsArray[n-1];                 
				try {oos.writeObject(saveMessageArray);} // send save msg + save recipients array to server
				catch(Exception e) {System.out.println("Error sending saveMessageArray to server");}
				System.out.println("Sending a saveMessageArray to server: " + Arrays.toString(saveMessageArray));
			} 

		}
		
		if (ae.getSource() == clearButton)
		{
			System.out.println("The ClearSelections button was pushed.");
			whosInList.clearSelection();    // remove the HIGHLIGHTING
			whosNotInList.clearSelection(); // not the entries!
		}

	}

	
// receive()
	public void receive() // main (loading) thread enters here!
	{ 

		String newLine = System.lineSeparator(); // new-line character.
		while(true) // "CAPTURE" the main (loading) thread 
		{ 

			try 
			{
				Object messageFromServer = ois.readObject(); // no casting

				if (messageFromServer instanceof String) // it's a chat message
				{
					String chatMessage = (String) messageFromServer; // make a pointer of type String 
					textArea.append(newLine + chatMessage);
					// auto-scroll textArea to bottom line so the last message will be visible.
					textArea.setCaretPosition(textArea.getDocument().getLength()); // really?
				}
				else if (messageFromServer instanceof String[]) // an array of whos-in or whos-not-in
				{
					String[] chatNames = (String[]) messageFromServer; // make a pointer of type String[]
					boolean foundIt = false; 
					
					for (String name : chatNames) // see if this is an IN or OUT list by if it contains THIS client!
					{
						if (name.equalsIgnoreCase(chatName))
						{ 
							System.out.println("Who's-in from server: " + Arrays.toString(chatNames));
							whosInList.setListData(chatNames); // show in the JList on the GUI
							foundIt = true;
							break;
						}
					}
					
					if (foundIt == false) 
					{
						System.out.println("Who's-not-in from server: " + Arrays.toString(chatNames));
						whosNotInList.setListData(chatNames); // show in the JList on the GUI
					}	
				}
				else 
				{
					System.out.println("Unexpected object type received from server: " + messageFromServer);
				}

			} 
			catch(Exception e) // Server Down
			{ 
				textField.setEditable(false);
				privateTextField.setEditable(false); // keep user from trying to send any more private messages.
				clearButton.setEnabled(false); // make button unpushable.
				
				textArea.setBackground(Color.pink);
				textArea.setText("Connection to server has gone Down\nPlease restart client to re-connect");

				return;
				// return is not required, reaching end of this method
			}

		}
	}

	
// main()
	public static void main(String[] args) {

		System.out.println("-----Jiale Hu-----\n");

		try {

			ChatRoomClient client;

			if (args.length == 3) // Normal Login
			{
				client = new ChatRoomClient(args[0], args[1], args[2], "");
			}
			else if (args.length == 4) // Request Password Change
			{
				client = new ChatRoomClient(args[0], args[1], args[2], args[3]);
			}
			else // Invalid Number of Arguments
			{
				System.out.println("Provide three or four command line parameters separated by spaces: ");
				System.out.println("Server Network Address, Chat Name, Password, New Password[Optional]\n");
				System.out.println("(New user please set password through the third argument)");
				System.out.println("(Reset password through the fourth argument)");
				System.out.println("(Password must not contain spaces)\n");
				return;
			}

			client.receive(); // branch main thread into client program object to be the receive thread!

		} catch(Exception e) {
			System.out.println(e);
			return;
		}
	}

}