package my.chatroom.server.exceptions;

/**
 * 
 * @author Jiale Hu
 * @version 1.0
 * @since 05/05/2020
 *
 */

public class FatalDataBaseException extends Exception
{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public FatalDataBaseException()
	{
		// TODO Auto-generated constructor stub
	}

	public FatalDataBaseException(String message)
	{
		super(message);
		// TODO Auto-generated constructor stub
	}

	public FatalDataBaseException(Throwable cause)
	{
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public FatalDataBaseException(String message, Throwable cause)
	{
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public FatalDataBaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
