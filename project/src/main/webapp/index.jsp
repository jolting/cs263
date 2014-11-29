<%@ page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreService" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreServiceFactory" %>
<%@ page import="com.google.appengine.api.datastore.Entity" %>
<%@ page import="com.google.appengine.api.datastore.Query" %>

 <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
Query query = new Query("PointCloud2");
Iterable<Entity> pointCloud2 = datastore.prepare(query).asIterable();
String pointcloudlist = new String();
for(Entity entity: pointCloud2)
{
  pointcloudlist = pointcloudlist +
   "<p><a target=\"viewer\" href=\"http://www.pointclouds.org/assets/viewer/pcl_viewer.html?load=http://"
    + request.getLocalName() + "/GetPCD/" + entity.getKey().getName() + "\" >"
    + entity.getKey().getName() + "</a></p>";
}


%>
<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
</head>
<p>Upload a PCD file here. Currently only supports PCD text/binary PCD files with floating point and uint32 data.
   No binary_compressed.
</p>
<form action="<%= blobstoreService.createUploadUrl("/uploadPCDRequest") %>" method="post" enctype="multipart/form-data">
<p>Name:<input type="text" name="myName"></p>
<p><input type="file" name="myFile"></p>
<input type="submit" value="Submit"/>
</form>

<%= pointcloudlist %>

<iframe width="100%" height="800px" src="index.html" name="viewer"> </iframe>

</body>
</html>
