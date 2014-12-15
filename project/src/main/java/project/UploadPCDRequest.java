package project;

//import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

public class UploadPCDRequest extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * The method queues the blob for processing
	 * 
	 * @param  request The servlet request object.
	 * @param  response The response object.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		MemcacheService syncCache         = MemcacheServiceFactory.getMemcacheService();
		syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));

		try{

			@SuppressWarnings("deprecation")
			Map<String, BlobKey> blobs = blobstoreService.getUploadedBlobs(request);
			BlobKey blobKey = blobs.get("myFile");

			Queue queue = QueueFactory.getDefaultQueue();
			queue.add(withUrl("/PCDWorker").param("blobKey", blobKey.getKeyString()));

			Key metaKey = KeyFactory.createKey("PointCloudMeta", blobKey.getKeyString());
			Entity meta = new Entity(metaKey);
			meta.setProperty("name",request.getParameter("myName"));

			String memcacheKey = "pcdList";
			datastore.put(meta);

			//invalidate the cached list
			String pointcloudlist =  PointCloudDescription.descriptionHtmlString(request.getServerName(), request.getServerPort());
			syncCache.put(memcacheKey, pointcloudlist, Expiration.byDeltaSeconds(1));

			response.sendRedirect("/processing.html");
			response.flushBuffer();
			return;

		}
		catch(Exception e)
		{
			response.sendRedirect("/processingFailed.html");	
			response.flushBuffer();
			return;
		}
	}
}
