package project;

//import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

public class GetUploadURL extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Gets an upload url for programatically uploading a PCD file.
	 * 
	 * @param  request The servlet request object.
	 * @param  response The response object.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
		response.getWriter().println(blobstoreService.createUploadUrl("/uploadPCDRequest"));
    }
}
