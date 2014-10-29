<%@ page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>
 <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
%>
<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
</head>
<p>type some data in here</p>
<form action="<%= blobstoreService.createUploadUrl("/uploadPCDRequest") %>" method="post" enctype="multipart/form-data">
<input type="file" name="myFile">
<input type="submit" value="Submit"/>
</form>

<p>${fn:escapeXml(value)}</p>
</body>
</html>
