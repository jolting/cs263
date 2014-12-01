<%@ page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreService" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreServiceFactory" %>
<%@ page import="com.google.appengine.api.memcache.MemcacheService" %>
<%@ page import="com.google.appengine.api.memcache.MemcacheServiceFactory" %>
<%@ page import="com.google.appengine.api.memcache.ErrorHandlers" %>
<%@ page import="com.google.appengine.api.memcache.Expiration" %>
<%@ page import="com.google.appengine.api.datastore.Key" %>
<%@ page import="com.google.appengine.api.datastore.KeyFactory" %>

<%@ page import="java.util.logging.Level" %>


<%@ page import="com.google.appengine.api.datastore.Entity" %>
<%@ page import="com.google.appengine.api.datastore.Query" %>

 <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%

BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
MemcacheService syncCache         = MemcacheServiceFactory.getMemcacheService();
syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));

String memcacheKey = "pcdList";
String pointcloudlist = (String) syncCache.get(memcacheKey);
if(pointcloudlist == null){
  
  pointcloudlist = new String();

  DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  Query query = new Query("PointCloud2");
  Iterable<Entity> pointCloud2 = datastore.prepare(query).asIterable();
  
  for(Entity entity: pointCloud2)
  {
          Key infoKey = KeyFactory.createKey("PointCloudMeta", entity.getKey().getName());

          query = new Query(infoKey);
          Entity meta = datastore.prepare(query).asSingleEntity();

          if(meta == null){
        	  continue;
          }
          
          pointcloudlist = pointcloudlist +
           "<p><a target=\"viewer\" href=\"http://www.pointclouds.org/assets/viewer/pcl_viewer.html?load=http://"
            + request.getServerName() + "/GetPCD/" + entity.getKey().getName() + "\" >"
            + (String) meta.getProperty("name")+ "</a></p>";
  }
  syncCache.put(memcacheKey, pointcloudlist, Expiration.byDeltaSeconds(20));
}


%>
<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
</head>
<p>Upload a PCD file here. Currently only supports ascii/binary PCD files with floating point and uint32 data.
   No binary_compressed.
</p>
<form action="<%= blobstoreService.createUploadUrl("/uploadPCDRequest") %>" method="post" enctype="multipart/form-data">
<p>Name:<input type="text" name="myName"></p>
<p><input type="file" name="myFile"></p>
<input type="submit" value="Submit"/>
</form>

<%= pointcloudlist %>
<p> Viewer provided by pointcloud.org </p>
<iframe width="100%" height="800px" src="view.html" name="viewer"> </iframe>

</body>
</html>
