package my.chatroom.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.Query.Direction;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.FirebaseOptions;
//import com.google.firebase.cloud.FirestoreClient;

import my.chatroom.data.json.JSON_Utility;
import my.chatroom.data.messages.Message;
import my.chatroom.data.users.User;
import my.chatroom.server.exceptions.FatalDataBaseException;
import my.chatroom.server.interfaces.Server_DB_Interface;

public class Server_DB_FireStore implements Server_DB_Interface
{
	
	private final Firestore db;
	private final ConcurrentHashMap<Integer, Queue<Message>> savedMessages;

	public Server_DB_FireStore() throws FatalDataBaseException
	{
		System.out.println("Server_DB: Initializing Firestore ...");
		try
		{
			GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
			FirestoreOptions options = FirestoreOptions.getDefaultInstance().toBuilder()
				    .setCredentials(credentials)
				    .setProjectId("chat-room-282102")
				    .build();
			
			db = options.getService();
		} catch (IOException e)
		{
			throw new FatalDataBaseException(e.getMessage());
		}
		
		// Load Saved Messages
		System.out.println("Server_DB: Loading Messages from DB ...");
		savedMessages = new ConcurrentHashMap<Integer, Queue<Message>>();
		try
		{
			ApiFuture<QuerySnapshot> future = db.collection("messages").get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();
			for (DocumentSnapshot document : documents)
			{
				int user_id = (int) document.get("user_id");
				String msgs = document.getString("messages");
				Queue<Message> queue = JSON_Utility.decodeMSGs(msgs);
				savedMessages.put(user_id, queue);
			}
		} catch (InterruptedException | ExecutionException e)
		{
			throw new FatalDataBaseException(e.getMessage());
		}
		
		System.out.println("-----Server_DB Loaded-----\n");
	}

	@Override
	public ConcurrentHashMap<Integer, Queue<Message>> getSavedMessages()
	{
		return savedMessages;
	}

	@Override
	public boolean saveAllMessages()
	{
		// Save All
		for (int user_id : savedMessages.keySet())
		{
			// Queue to JSON string
			String msgs = JSON_Utility.encodeMSGs(savedMessages.get(user_id));
			try
			{
				String docID = getDocumentID(user_id);
				DocumentReference docRef = db.collection("users").document(docID);
				ApiFuture<WriteResult> future = docRef.update("messages", msgs);
				System.out.println("Server_DB: Messages Saved: " + future.get());
			} catch (Exception e)
			{
				System.err.println("Server_DB: - Failed to save savedMessages to DB for " + user_id + e.getMessage());
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean saveMessages(int user_id)
	{
		String msgs = JSON_Utility.encodeMSGs(savedMessages.get(user_id));
		try
		{
			String docID = getDocumentID(user_id);
			DocumentReference docRef = db.collection("users").document(docID);
			ApiFuture<WriteResult> future = docRef.update("messages", msgs);
			System.out.println("Server_DB: Messages Saved: " + future.get());
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Failed to save savedMessages to DB for " + user_id + e.getMessage());
			return false;
		}
		return true;
	}
	
	@Override
	public boolean deleteMessages(int user_id)
	{
		try
		{
			String docID = getDocumentID(user_id);
			DocumentReference docRef = db.collection("users").document(docID);
			ApiFuture<WriteResult> future = docRef.update("messages", "");
			System.out.println("Server_DB: Messages Deleted: " + future.get());
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Failed to save savedMessages to DB for " + user_id + e.getMessage());
			return false;
		}
		return false;
	}

	@Override
	public Integer[] getIDs()
	{
		List<Integer> IDs = new ArrayList<>();
		try
		{
			ApiFuture<QuerySnapshot> future = db.collection("users").get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();
			for (DocumentSnapshot document : documents)
			{
				IDs.add(((Long) document.get("user_id")).intValue());
			}
		} catch (InterruptedException | ExecutionException e)
		{
			System.err.println("Server_DB: - Failed to Load User IDs from DB");
			e.printStackTrace();
			return null;
		}
		return IDs.toArray(new Integer[0]);
	}

	@Override
	public String getNickName(int user_id)
	{
		String nick_name = null;
		try
		{
			String docID = getDocumentID(user_id);
			ApiFuture<DocumentSnapshot> future = db.collection("users").document(docID).get();
			nick_name = (String) future.get().get("nick_name");
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Error Retrieving Nick Name for User ID: " + user_id);
			e.printStackTrace();
			return null;
		}
		return nick_name;
	}

	@Override
	public String getUserInfo(int user_id)
	{
		User user = null;
		try
		{
			String docID = getDocumentID(user_id);
			ApiFuture<DocumentSnapshot> future = db.collection("users").document(docID).get();
			DocumentSnapshot document = future.get();
			if (document.exists()) user = document.toObject(User.class);
			else return "No Such User Found in DB.";
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Error Retrieving Nick Name for User ID: " + user_id);
			e.printStackTrace();
			return null;
		}
		return "User ID: " + user_id + " Nick Name: " + user.getNick_name() + " User Since: " + new Date(user.getSignup_time());
	}

	@Override
	public boolean isExist(int user_id)
	{
		try
		{
			String docID = getDocumentID(user_id);
			ApiFuture<DocumentSnapshot> future = db.collection("users").document(docID).get();
			DocumentSnapshot document = future.get();
			if (document.exists()) return true;
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Error Retrieving Nick Name for User ID: " + user_id);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public int getNextUserID() throws Exception
	{
		Query query = db.collection("users").orderBy("user_id", Direction.DESCENDING).limit(1);
		
		int lastID = (int) query.get().get().getDocuments().get(0).get("user_id");
		
		return lastID + 1;
	}

	@Override
	public User addUser(String nick_name, String password)
	{
		User user = null;
		try
		{
			int newID = getNextUserID();
			user = new User(newID, nick_name, password);
			ApiFuture<DocumentReference> result = db.collection("users").add(user);
			System.out.println("Server_DB: New User Saved: " + result.get().getId());
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Failed to add user to DB. " + e.getMessage());
			e.printStackTrace();
			return null;
		}
		
		return user;
	}

	@Override
	public boolean removeUser(int user_id)
	{
		try
		{
			String docID = getDocumentID(user_id);
			ApiFuture<WriteResult> future = db.collection("users").document(docID).delete();
			System.out.println("Server_DB: User Deleted at: " + future.get().getUpdateTime());
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Error Deleting User ID: " + user_id);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private String getDocumentID(int user_id) throws Exception
	{
		ApiFuture<QuerySnapshot> future = db.collection("users").whereEqualTo("user_id", user_id).get();
		List<QueryDocumentSnapshot> documents = future.get().getDocuments();
		if (documents.size() > 1) throw new Exception("Got Multiple Documents for User ID: " + user_id);
		return documents.get(0).getId();
	}

	@Override
	public boolean setNickName(int user_id, String newName)
	{
		try
		{
			String docID = getDocumentID(user_id);
			DocumentReference docRef = db.collection("users").document(docID);
			ApiFuture<WriteResult> future = docRef.update("nick_name", newName);
			System.out.println("Server_DB: Nick Name Changed: " + future.get());
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Error Changing Nick Name for User ID: " + user_id);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean checkPassword(int user_id, String password)
	{
		try
		{
			String docID = getDocumentID(user_id);
			ApiFuture<DocumentSnapshot> future = db.collection("users").document(docID).get();
			String truePassword = (String) future.get().get("password");
			if (password.equals(truePassword)) return true;
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Error Retrieving Password for User ID: " + user_id);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean setPassword(int user_id, String oldPassword, String newPassword)
	{
		if (!checkPassword(user_id, oldPassword)) 
		{
			System.out.println("Server_DB: - Wrong password: " + user_id);
			return false;
		}
		try
		{
			String docID = getDocumentID(user_id);
			DocumentReference docRef = db.collection("users").document(docID);
			ApiFuture<WriteResult> future = docRef.update("password", newPassword);
			System.out.println("Server_DB: Password Changed: " + future.get());
		} catch (Exception e)
		{
			System.err.println("Server_DB: - Error Changing Password for User ID: " + user_id);
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
