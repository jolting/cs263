package project;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;

//import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;



public class GetPointClouds extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		 DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
         Query query = new Query("PointCloud2");
         ServletOutputStream out = response.getOutputStream();
	     
	     Iterable<Entity> pointCloud2 = datastore.prepare(query).asIterable();
	     for(Entity entity: pointCloud2)
	     {
	       out.println(entity.getKey().getName());
	     }
		  
	}
}
