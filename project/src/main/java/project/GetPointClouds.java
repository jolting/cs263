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
import com.google.appengine.repackaged.com.google.common.collect.Iterables;
import com.google.gson.Gson;
//import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;



public class GetPointClouds extends HttpServlet {
	private static final long serialVersionUID = 1L;
	/**
	 * JSON list of point clouds.
	 * 
	 * @param  request The servlet request object.
	 * @param  response The response object.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		 DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
         Query query = new Query("PointCloudMeta");
         ServletOutputStream out = response.getOutputStream();
	     
	     Iterable<Entity> pointCloudMeta = 
	    		 datastore.prepare(query).asIterable();

	     Gson gson = new Gson();
    	 out.println(gson.toJson(Iterables.toArray(pointCloudMeta, Entity.class)));

//	     for(Entity entity: pointCloudmeta)
//	     {
	    	 
//	     }
	}
}
