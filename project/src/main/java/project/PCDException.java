package project;

import javax.servlet.ServletException;

//import com.google.api.server.spi.ServiceException;

public class PCDException extends ServletException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PCDException(String message){
		     super(message);
	}
	
}
