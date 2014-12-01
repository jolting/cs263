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
<%@ page import="project.PointCloudDescription" %>

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
  pointcloudlist =  PointCloudDescription.descriptionHtmlString(request.getServerName(), request.getServerPort());
  syncCache.put(memcacheKey, pointcloudlist, Expiration.byDeltaSeconds(1));
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
