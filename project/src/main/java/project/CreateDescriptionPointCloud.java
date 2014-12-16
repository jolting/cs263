package project;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.gson.Gson;
//import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;



public class CreateDescriptionPointCloud extends HttpServlet {
	private static final long serialVersionUID = 1L;
	/**
	 * JSON list of point clouds.
	 * 
	 * @param  request The servlet request object.
	 * @param  response The response object.
	 */
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		 DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
         
	     Gson gson = new Gson();
	     byte[] b = new byte[4096];
	     request.getInputStream().read(b);
	     
    	 PCDescription input = gson.fromJson(b.toString(), PCDescription.class);
    	 
    	 Entity description = new Entity("PointCloudDescription", input.key);
    	 description.setProperty("value", input.description);
    	 
    	 datastore.put(description);
	}
	
}
