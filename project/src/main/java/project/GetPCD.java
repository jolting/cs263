/*
 * Software License Agreement (BSD License)
 *
 *  Point Cloud Library (PCL) - www.pointclouds.org
 *  Copyright (c) 2010-2011, Willow Garage, Inc.
 *  Copyright (c) 2014, Hunter Laux
 *
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of Willow Garage, Inc. nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 * $Id$
 *
 */

package project;


import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

// The Worker servlet should be mapped to the "/worker" URL.

public class GetPCD extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
		
		/* skip the first key / */
		String keyStr =request.getPathInfo().substring(1);
		
		/* This allows /$key/filename.pcd where filename can be whatever */
		keyStr = keyStr.split("/")[0];
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Key pclKey = KeyFactory.createKey("PointCloud2", keyStr);
        
        Query query = new Query(pclKey);
        
        try{
          Entity pointCloud2 = datastore.prepare(query).asSingleEntity();
          if(pointCloud2 == null)
          {
              ServletOutputStream out = response.getOutputStream();
              out.print("Can't find point cloud "+ keyStr + "\n");
              out.flush();
        	  return;
          }
          PointCloud2 cloud = new PointCloud2();
        
		  cloud.width = Integer.valueOf(((Long) pointCloud2.getProperty("width")).intValue());	
          cloud.height = Integer.valueOf(((Long) pointCloud2.getProperty("height")).intValue());
          cloud.is_bigendian = (Boolean) pointCloud2.getProperty("is_bigendian");
          cloud.row_step = Integer.valueOf(((Long) pointCloud2.getProperty("row_step")).intValue());	      
          cloud.is_dense = (Boolean) pointCloud2.getProperty("is_dense");

          Filter filter =
        		  new FilterPredicate("cloud",
        		                      FilterOperator.EQUAL,
        		                      keyStr);
          
          Query fieldQuery = new Query("PointField").setFilter(filter);
          PreparedQuery fields = datastore.prepare(fieldQuery);
          List<Entity>lFields = fields.asList(FetchOptions.Builder.withDefaults());
          cloud.fields = new PointField[lFields.size()];
          int i;
          for(i = 0; i < lFields.size(); i++){
        	  int idx = ((Long) lFields.get(i).getProperty("idx")).intValue();
        	  cloud.fields[idx] = new PointField();
        	  cloud.fields[idx].count    = Integer.valueOf(((Long) lFields.get(i).getProperty("count")).intValue());
        	  cloud.fields[idx].offset   = Integer.valueOf(((Long) lFields.get(i).getProperty("offset")).intValue());
        	  cloud.fields[idx].datatype = Byte.valueOf(((Long) lFields.get(i).getProperty("datatype")).byteValue());
        	  cloud.fields[idx].name     = (String)  lFields.get(i).getProperty("name");
          }
          
          
          BlobKey blobKey =(BlobKey) pointCloud2.getProperty("data");
          
          ServletOutputStream out = response.getOutputStream();
          out.print("# .PCD v0.7 - Point Cloud Data file format\n");
          out.print("VERSION 0.7\n");
          out.print(getFieldstr(cloud)+"\n");
          out.print(getSizeStr(cloud)+"\n");
          out.print(getTypeStr(cloud)+"\n");
          out.print(getCountStr(cloud)+"\n");
          out.print("WIDTH " + cloud.width+"\n" );
          out.print("HEIGHT " + cloud.height+"\n");
          out.print("VIEWPOINT 0 0 0 1 0 0 0\n");
          out.print("POINTS " + cloud.width * cloud.height + "\n");
          out.print("DATA binary\n");
          
          BlobstoreService blobstoreService =  BlobstoreServiceFactory.getBlobstoreService();


          BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
		  
          if( blobInfo == null )
        	  return;
  		  
          if( blobInfo.getSize() > Integer.MAX_VALUE )
        	  throw new RuntimeException("This method can only process blobs up to " + Integer.MAX_VALUE + " bytes");
  		  
  		  int blobSize = (int)blobInfo.getSize();
  		  int chunks = (int)Math.ceil(((double)blobSize / BlobstoreService.MAX_BLOB_FETCH_SIZE));
  		  int startPointer = 0;
  		  int endPointer;
  		 
  		  for(i = 0; i < chunks; i++ ){
  		   endPointer = Math.min(blobSize - 1, startPointer + BlobstoreService.MAX_BLOB_FETCH_SIZE - 1);
  		   
  		   byte[] bytes = blobstoreService.fetchData(blobKey, startPointer, endPointer);
   		   out.write(bytes);
   		   startPointer = endPointer + 1;
		   
  		  }

          
//          out.write(cloud.data.getBytes());
          out.flush();
        }
        catch(IndexOutOfBoundsException e)
        {
            ServletOutputStream out = response.getOutputStream();
            out.println("can't fetch pointcloud "+ keyStr);
            out.flush();
        	
        	
        }
    }


	private String getTypeStr(PointCloud2 cloud) {
		String typeStr = new String("TYPE");
		for(PointField field: cloud.fields){
			switch(field.datatype){
			case(1):
			case(3):
			case(5):
				typeStr = typeStr + " I";
				break;
			case(2):
			case(4):
			case(6):
				typeStr = typeStr + " U";
				break;
			case(7):
			case(8):
				typeStr = typeStr + " F";
				break;
			}
		}
		return typeStr;
	}


	private String getCountStr(PointCloud2 cloud) {
		String countStr = new String("COUNT");
		for(PointField field: cloud.fields){
			countStr = countStr + " " + field.count;
		}
		return countStr;
	}
	private String getSizeStr(PointCloud2 cloud)
	{
		String typeStr = new String("SIZE");
		for(PointField field: cloud.fields){
			switch(field.datatype){
			case(1):
			case(2):
				typeStr = typeStr + " 1";
				break;
			case(3):
			case(4):
				typeStr = typeStr + " 2";
				break;
			case(5):
			case(6):
			case(7):
			case(8):
				typeStr = typeStr + " 4";
				break;
			}
		}
		return typeStr;
			
		
	}

	private String getFieldstr(PointCloud2 cloud) {
		String fieldStr = new String("FIELDS");
		for(PointField field: cloud.fields){
			fieldStr = fieldStr + " " + field.name;
		}
		return fieldStr;
	}

}