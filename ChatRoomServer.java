// Jiale Hu

import java.net.*; // ss
import java.io.*; // ois, oos
import java.util.*; // Arrays
import java.util.concurrent.*; // keyed collection

public class ChatRoomServer implements Runnable
{
	
// Instance Variables
	private ServerSocket ss;
	private ConcurrentHashMap<String,ObjectOutputStream> 	whosIn = 
							new ConcurrentHashMap<String,ObjectOutputStream>();
	private ConcurrentHashMap<String,String> 				passwords = 
							new ConcurrentHashMap<String,String>();
	private ConcurrentHashMap<String, Vector<Object>>  		savedMessages = 
		    				new ConcurrentHashMap<String, Vector<Object>>(); 
	
// Constructor
	@SuppressWarnings("unchecked") // Suppress warning for unchecked cast type at diskOIS.readObject()
	public ChatRoomServer() throws Exception
	{
		int serverPort = 5555; 
		
	// Load password.ser on the disk (same directory)
		try 
		{
			FileInputStream   fis     = new FileInputStream("passwords.ser"); // .ser file type means serialized object(s)  
			ObjectInputStream diskOIS = new ObjectInputStream(fis);
			passwords = (ConcurrentHashMap<String,String>) diskOIS.readObject(); // read the whole collection from disk (Unchecked Type)	
			diskOIS.close(); // the first line of the method opens the file, this closes it.
			System.out.println("password.ser Loaded");
		}                
		catch(FileNotFoundException fnfe) // File will not be there first time you test...
		{
			System.out.println("A passwords.ser file was not found on disk, so an empty passwords collection will be used.");
		}
		catch(Exception e) // some other problem reading the file...
		{
			System.out.println(e); // print the Exception object as the error message.
			throw new IllegalArgumentException(e.getMessage()); // tell loading program 
		}
		System.out.println("Saved passwords: " + passwords); // show passwords collection to tester!

	// Load SavedMessages.ser on the disk (same directory)
		try 
		{
			FileInputStream   fis     = new FileInputStream("SavedMessages.ser"); // .ser file type means serialized object(s)  
			ObjectInputStream diskOIS = new ObjectInputStream(fis);
			savedMessages = (ConcurrentHashMap<String, Vector<Object>>) diskOIS.readObject(); // read the whole collection from disk (Unchecked Type)	
			diskOIS.close(); // the first line of the method opens the file, this closes it.
			System.out.println("SavedMessages.ser Loaded");
		}                
		catch(FileNotFoundException fnfe) // File will not be there first time you test...
		{
			System.out.println("A SavedMessages.ser file was not found on disk, so an empty message collection will be used.");
		}
		catch(Exception e) // some other problem reading the file...
		{
			System.out.println(e); // print the Exception object as the error message.
			throw new IllegalArgumentException(e.getMessage()); // tell loading program 
		}
		System.out.println("Saved messages: " + savedMessages); // show collection to tester!
		
	// Establish Socket Connection
		try 
		{
			ss = new ServerSocket(serverPort);
		} 
		catch(Exception e)
		{
			throw new IllegalArgumentException("Port " + Integer.toString(serverPort) + 
					" is NOT available to accept connections");
		}
		System.out.println("ChatRoomServer is up at " 
				+ InetAddress.getLocalHost().getHostAddress() 
				+ " on port " + ss.getLocalPort() + "\n");

		new Thread(this).start(); // Not expecting calling this thread later
		
	}

// run()
	@Override
	public void run() // client threads enter here
	{
	// Local Variables
		Socket				s				= null;
		ObjectInputStream	ois				= null;
		ObjectOutputStream	oos				= null;
		String				chatName		= null;
		String				clientAddress	= null;
		
		String joinMessage     = null; // the first message received
		String enteredPassword = null;
		String storedPassword  = null;
		String replacePassword = null;
		
		try 
		{
			s = ss.accept(); // wait for next client to connect
			clientAddress = s.getInetAddress().getHostAddress();
			System.out.println("New client connecting from " + clientAddress);
			ois = new ObjectInputStream(s.getInputStream());  
			joinMessage = (String) ois.readObject();    
			oos = new ObjectOutputStream(s.getOutputStream());
		}
		catch(Exception e) // connecting client may not be using oos,
		{              // or firstMessage was not a String
			System.out.println("Connect/join failed from " + clientAddress);
			return; // return to the Thread object to terminate this client thread. 
		}
		finally // create a next-client thread whether catch was entered or not. 
		{
			new Thread(this).start(); // Start new run(). new client in communication buffer times out in 30 sec
		}
		
		
	// Continue this thread
		
	// Check joinMessage (chatName password)
		joinMessage = joinMessage.trim();
		int blankOffset = joinMessage.indexOf(" "); // Returns first blank index, or -1 if no blank
		if (blankOffset < 0)
		{
			try 
			{
				oos.writeObject("Invalid message format."); // send vague err msg as join reply.
				oos.close(); // drop connection
			}
			catch(Exception e) {}
			
			System.out.println("Invalid join message received: '" + joinMessage + "' from " + clientAddress);
			return; // this client thread returns to it's Thread object which terminates it.
		} // Valid join msg
		
		// Get chatName
		chatName = joinMessage.substring(0,blankOffset).toUpperCase(); // extract the chatName from join message.
		// Get entered password
		enteredPassword = joinMessage.substring(blankOffset).trim(); // remove leading blanks
		
		// check for replacement password (old new)
		int passwordBlankOffset = enteredPassword.indexOf(" ");
		if (passwordBlankOffset > 0) // can't be 0 because we trim()ed enteredPassword.
		{                         // A replacePasssord has been provided!
			replacePassword = enteredPassword.substring(passwordBlankOffset).trim();// Order of these
			enteredPassword = enteredPassword.substring(0,passwordBlankOffset);     // statements is important!
		}
		else 
		{
			replacePassword = ""; // not null but not a blank.
		}
		
		// Test chatName and passwords
		if (chatName.contains(" ") || enteredPassword.contains(" ") || replacePassword.contains(" "))
		{
			try 
			{
				oos.writeObject("Invalid chatName/Password(s) format."); // send err msg as join reply.
				oos.close(); // drop connection
			}
			catch(Exception e) {}
			
			System.out.println("Invalid chatName/Password(s) received: '" + chatName + "'/(" +
							enteredPassword + ")/(" + replacePassword + ") from " + clientAddress);
			return; // this client thread returns to it's Thread object which terminates it.
		} // Valid chatName/Password(s)
		
		
	// Check Stored password
		if (passwords.containsKey(chatName))
		{   
			storedPassword = passwords.get(chatName);
			if (!enteredPassword.equals(storedPassword)) // case-sensitive compare
			{
				try 
				{
					oos.writeObject("Incorrect password for " + chatName); // send err msg as join reply.
					oos.close(); // drop connection
				}
				catch(Exception e) {}

				System.out.println("Invalid password (" + enteredPassword + ") received for " + chatName
						+ " should be (" + storedPassword + ")");
				return; // this client thread returns to it's Thread object which terminates it.
			}
			else // the providedPassword DID match the storedPassword
			{
				System.out.println("Correct Password from " + chatName);
				
				if (replacePassword.length() > 0) // a password change was requested
				{
					passwords.replace(chatName, replacePassword); 
					System.out.println("Password changed successfully for " + chatName);
					
					savePasswords();
					System.out.println("Password Saved on Disk: " + passwords);
				}
				else
				{
					System.out.println("Password unchanged for " + chatName);
				}	
			}
		} // Password verified
		else
		{
			System.out.println("No password check, New user: " + chatName);
		}
		
		
	// Welcome and Save password for new user
		try 
		{                                                               
			oos.writeObject("Welcome to the chat room " + chatName + " !"); // send "join is successful" 
			// And, if they are a never-before client, add their pw to the collection:
			if (!passwords.containsKey(chatName))    
			{
				if (replacePassword.length() == 0)
				{
					passwords.put(chatName, enteredPassword); 
				}
				else
				{
					passwords.put(chatName, replacePassword);
				}
				System.out.println("Initial password saved for " + chatName);
				
				savePasswords();
				System.out.println("Password Saved on Disk: " + passwords);
			}
		}                                                               
		catch(Exception e)
		{
			System.out.println("Error sending join reply to client " + chatName);
			return; // terminate this new client thread.
		}

		
	// Joining Processing 
		if (whosIn.containsKey(chatName)) // Check if Rejoin from same user (but different client), then dump previous session
		{
			ObjectOutputStream previousSessionOOS = whosIn.get(chatName);
			try 
			{
				previousSessionOOS.writeObject("Session terminated due to join of client at another location.");
				previousSessionOOS.close(); // terminate the connection to Bubba's previous location
				// Throw previous thread in Leaving Processing, which terminates that thread.
			}
			catch(Exception e) {}
			
			whosIn.replace(chatName, oos); // replace client's old oos with new oos in whosIn list.
			System.out.println(chatName + " is re-joining with password " + passwords.get(chatName));
			
			String[] inChatNames        = whosIn.keySet().toArray(new String[0]);
			String[] passwordsChatNames = passwords.keySet().toArray(new String[0]);
			Vector<String> whosNotInVector = new Vector<String>();
			for (String name : passwordsChatNames) // initialize Vector with passwordsChatNames
				whosNotInVector.add(name);
			for (String name : inChatNames)        // subtract whosIn names from passwords names
				whosNotInVector.remove(name);     // to leave whosNotIn names in Vector.
			String[] notInChatNames = whosNotInVector.toArray(new String[0]);
			Arrays.sort(inChatNames);
			Arrays.sort(passwordsChatNames);
			Arrays.sort(notInChatNames);
			
			System.out.println("Currently in the chat room: " + Arrays.toString(inChatNames)); // show who's in on server console.
			sendToAll(inChatNames);  
			sendToAll(notInChatNames);
			
			/*
			try 
			{
				oos.writeObject(); // Only send to rejoining client
			}
			catch(Exception e) {}
			*/
		}
		else // Do normal join processing.
		{
			
			whosIn.put(chatName, oos); // add new client to whosIn list.
			sendToAll("Say hello to " + chatName + " who just joined the chat room!");
			System.out.println(chatName + " is joining with password " + passwords.get(chatName));
			
			String[] inChatNames        = whosIn.keySet().toArray(new String[0]);
			String[] passwordsChatNames = passwords.keySet().toArray(new String[0]);
			Vector<String> whosNotInVector = new Vector<String>();
			for (String name : passwordsChatNames) // initialize Vector with passwordsChatNames
				whosNotInVector.add(name);
			for (String name : inChatNames)        // subtract whosIn names from passwords names
				whosNotInVector.remove(name);     // to leave whosNotIn names in Vector.
			String[] notInChatNames = whosNotInVector.toArray(new String[0]);
			Arrays.sort(inChatNames);
			Arrays.sort(passwordsChatNames);
			Arrays.sort(notInChatNames);
			
			System.out.println("Currently in the chat room: " + Arrays.toString(inChatNames)); // show who's in on server console.
			sendToAll(inChatNames);  
			sendToAll(notInChatNames); 
			
		} // Join Processing Ends
		
		
	// Check for any saved messages for this client
		Vector<Object> savedMessageList = savedMessages.get(chatName);

		if (savedMessageList != null) // Is there even a list for this user
		{
			while (!savedMessageList.isEmpty()) // any messages left?
			{
				Object savedMessage = savedMessageList.remove(0); // show & remove oldest message first
				try {
					oos.writeObject(savedMessage);
					System.out.println("- Sent SAVED message '" + savedMessage + "' to " + chatName);
					
					saveMessagesCollection(); // save removed savedMessageList
					System.out.println("Saved messages: " + savedMessages);
				}
				catch(Exception e) // joiner has suddenly left
				{
					break; // so stop showing
				}
			}
		}

		
	// Receive/Send processing
		try
		{
			while(true) 
			{                         
				Object messageFromClient = ois.readObject(); //wait for client to end object (String, String[])
				//System.out.println("Object Received: '" + messageFromClient + "' from " + chatName); // (debug trace)
				
				if (messageFromClient instanceof String)
				{
					String chatString = (String) messageFromClient; // make a pointer of type String
					System.out.println("- Received PUBLIC msg '" + chatString + "' from " + chatName); // call toString()
					
					if (chatString.length() > 0)
					{
						sendToAll(chatName + " says: " + chatString); // broadcast chat message to all clients.
						System.out.println("- Sent '" + messageFromClient + "' to all clients");
					}
					else
					{
						System.out.println("Invalid message from " + chatName);
					}
				}
				else if (messageFromClient instanceof String[]) // an array of Strings
				{
					String[] messageArray = (String[]) messageFromClient; // make a pointer of type String[]
					String recipientChatName = messageArray[1]; // First recipient in array (has at least 1)
					
					if (whosIn.containsKey(recipientChatName)) // WhosInList Array
					{
						System.out.println("- Received privateMessageArray " + Arrays.toString(messageArray) + " from " + chatName);
						sendPrivateMessage(chatName, messageArray);
					}
					else // WhosNotInList Array
					{
						System.out.println("- Received saveMessageArray " + Arrays.toString(messageArray) + " from " + chatName);
						saveMessage(chatName, messageArray);
					}
				}
				else 
				{
					System.out.println("Unexpected object type received from client: " + messageFromClient); // call toString() on the object
				}

			}     
		}
		catch(Exception e) // catch ois.readObject() error, start Leaving Processing
		{
			if (whosIn.get(chatName) != oos) // Rejoining situation
			{
				System.out.println("Re-joining, previous thread for " + chatName + " terminated");
				return;
			}
			
			// Normal Leaving Processing
			whosIn.remove(chatName); // remove client from collection
			System.out.println(chatName + " is leaving the chat room");
			
			sendToAll("Goodbye to " + chatName + " who just left the chat room.");
			
			String[] inChatNames        = whosIn.keySet().toArray(new String[0]);
			String[] passwordsChatNames = passwords.keySet().toArray(new String[0]);
			Vector<String> whosNotInVector = new Vector<String>();
			for (String name : passwordsChatNames) // initialize Vector with passwordsChatNames
				whosNotInVector.add(name);
			for (String name : inChatNames)        // subtract whosIn names from passwords names
				whosNotInVector.remove(name);     // to leave whosNotIn names in Vector.
			String[] notInChatNames = whosNotInVector.toArray(new String[0]);
			Arrays.sort(inChatNames);
			Arrays.sort(passwordsChatNames);
			Arrays.sort(notInChatNames);
			
			System.out.println("Currently in the chat room: " + Arrays.toString(inChatNames)); // show who's in on server console.
			sendToAll(inChatNames);  
			sendToAll(notInChatNames);
			
			return; // Optional return; No need to call close(), which may throw more error
		}
	}

// sendToAll() with lock
	private synchronized void sendToAll(Object message) // force client threads to enter one-at-a-time
	{
		ObjectOutputStream[] oosArray = whosIn.values().toArray(new ObjectOutputStream[0]);
		
		for (ObjectOutputStream clientOOS : oosArray)
		{
			try {clientOOS.writeObject(message);}
			catch (IOException e) {} // do nothing if send error
			// ois.readObject() will take care of errors
		}
	}
	
// savePasswords with lock
	private synchronized void savePasswords() // make calling client threads enter one-at-a-time
	{
		try 
		{
			FileOutputStream   fos     = new FileOutputStream("passwords.ser"); // .ser file type means serialized object(s)  
			ObjectOutputStream diskOOS = new ObjectOutputStream(fos);
			diskOOS.writeObject(passwords); // write the whole collection to disk!
			diskOOS.close(); // the first line of the method opens the file, this closes it.
			
		}                // (It was not opened in "append" mode, so this overwrites the existing file.)
		catch(Exception e)
		{
			System.out.println("Error saving passwords file on disk. " + e);
		}
	}
	
// saveMessagesCollection with lock
	private synchronized void saveMessagesCollection() 
	{
		try 
		{
			FileOutputStream   fos     = new FileOutputStream("SavedMessages.ser"); // .ser file type means serialized object(s)  
			ObjectOutputStream diskOOS = new ObjectOutputStream(fos);
			diskOOS.writeObject(savedMessages); // write the whole collection to disk!
			diskOOS.close(); // the first line of the method opens the file, this closes it.
		
		}                // (It was not opened in "append" mode, so this overwrites the existing file.)
		catch(Exception e)
		{
			System.out.println("Error saving savedMessages file on disk. " + e);
		}
	}
	
// sendPrivateMessage()
	private synchronized void sendPrivateMessage(String chatName, String[] recipientList)
	{
		String actualRecipients = " "; // Recipients sent successfully

		// Send to each recipient(s)
		for (int i = 1; i < recipientList.length; i++) // Loop thru each recipient in array
		{
			String otherRecipients = " "; // who else is recipient(s)
			
			for (int n = 1;  n < recipientList.length; n++)
			{
				if (!recipientList[n].equalsIgnoreCase(recipientList[i]))
					otherRecipients += recipientList[n] + " ";
			}
			
			String privateMessage;
			if (otherRecipients.trim().length() == 0) // If only one private recipient
				privateMessage = chatName + " sends you PRIVATE message: '" + recipientList[0] + "'";
			else // there are other recipients. 
				privateMessage = chatName + " sends PRIVATE message: '" + recipientList[0] 
						   			+ "' to: [you and " + otherRecipients + "]";
			
			ObjectOutputStream recipientOOS = whosIn.get(recipientList[i]); // Get private recipient(s) oos
			
			if (recipientOOS != null) // perhaps this recipient just left the chat room! 
			{ 
				try 
				{
					recipientOOS.writeObject(privateMessage);
					actualRecipients += recipientList[i] + " ";
				}
				catch(Exception e) {} // skip it if failure
			}
			else
			{
				System.out.println("Failed sending PRIVATE message to " + recipientList[i]);
			}
			
		}
		
		System.out.println("- Sent Private msg '" + recipientList[0] + "' to: [" + actualRecipients + "]");
		
		// Confirm with Sender
		ObjectOutputStream senderOOS = whosIn.get(chatName);
		if (senderOOS != null) // perhaps the sender just left the chat room! 
		{
			String confirmationMessage = "(Your PRIVATE message '" + recipientList[0] 
					+ "' was successfully sent to [" + actualRecipients + "])";  
			try {senderOOS.writeObject(confirmationMessage);}
			catch(Exception e) {} // skip it if failure
		}
		
	}

// saveMessage() (private msg not in situation)
	private synchronized void saveMessage(String chatName, String[] recipientList)
	{
		
		Date date = new Date(); // Current time stamp
		String actualRecipients = " "; // Recipients sent successfully
		
		// Loop thru each offline recipient
		for (int i = 1; i < recipientList.length; i++) // Loop thru each recipient in array
		{
			String otherRecipients = " "; // who else is recipient(s)
			
			for (int n = 1;  n < recipientList.length; n++)
			{
				if (!recipientList[n].equalsIgnoreCase(recipientList[i]))
					otherRecipients += recipientList[n] + " ";
			}
			
			String messageToBeSaved;
			if (otherRecipients.trim().length() == 0) // If only one private recipient
				messageToBeSaved = chatName + " sent you PRIVATE message: '" + recipientList[0]
									+ "' at: " + date;
			else // there are other recipients. 
				messageToBeSaved = chatName + " sent PRIVATE message: '" + recipientList[0] 
									+ "' to: [you and " + otherRecipients + "] at: " + date;
			
			Vector<Object> recipientVector = savedMessages.get(recipientList[i]); // Check if this recipientVector saved
			
			if (recipientVector == null) // this recipient has never had a message saved for them 
			{
				recipientVector = new Vector<Object>();              // so make them an empty Vector
				savedMessages.put(recipientList[i], recipientVector);// and add this recipient to the 
			}                                                     // savedMessages collection. 
			
			recipientVector.add(messageToBeSaved); // add this message to this recipient's Vector. 
			
			saveMessagesCollection();
			actualRecipients += recipientList[i] + " ";
		}
		
		System.out.println("- Saved Private msg '" + recipientList[0] + "' for: [" + actualRecipients + "]");
		System.out.println("Saved messages: " + savedMessages);
		
		// Confirm with Sender

		ObjectOutputStream senderOOS = whosIn.get(chatName);
		if (senderOOS != null) // perhaps the sender just left the chat room! 
		{
			String confirmationMessage = "(Your PRIVATE message '" + recipientList[0] 
					+ "' was successfully saved for [" + actualRecipients + "] at: " + date + ")";  
			try {senderOOS.writeObject(confirmationMessage);}
			catch(Exception e) {} // skip it if failure
		}
		
	}
	
	
// main()
	public static void main(String[] args)
	{
		System.out.println("-----Jiale Hu-----\n");
		
		if (args.length != 0)
		{
			System.out.println("Cmd Line Argument(s) Ignored");
		}
		
		try
		{
			new ChatRoomServer(); // Indicates not expecting calling the object in main
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}

}