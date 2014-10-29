package project;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

import java.util.Calendar;
import java.util.Date;

// The Worker servlet should be mapped to the "/worker" URL.
public class PCDWorker extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String key = request.getParameter("blobKey");
        BlobKey blobKey = new BlobKey(key);
		BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
		
		
		
		/* todo: support larger files */
      //  byte[] data = blobstoreService.fetchData(blobKey, 0,  BlobstoreService.MAX_BLOB_FETCH_SIZE-1);
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity entity = new Entity("PCDBlobs", key);
        entity.setProperty("BlobKey", blobKey);
        datastore.put(entity); 
        
     }
}