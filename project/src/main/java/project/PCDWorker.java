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
import java.nio.ByteBuffer;

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

// The Worker servlet should be mapped to the "/worker" URL.
public class PCDWorker extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	  void
	  copyStringValueFloat (String st, PointCloud2 cloud,
	                   int point_index, int field_idx, int fields_count)
	  {
	    float value;
	    if (st == "nan")
	    {
	      value = Float.NaN;
	      cloud.is_dense = false;
	    }
	    else
	    {
	    	value = Float.parseFloat(st);
	    }

	    ByteBuffer.wrap(cloud.data).putFloat(point_index*cloud.point_step + 
	    									 cloud.fields[field_idx].offset + 
	    									 fields_count * 4,value).array();
	  }
	
	byte getFieldType (int size, char type)
	  {
	    type =  Character.toUpperCase(type);
	    switch (size)
	    {
	      case 1:
	        if (type == 'I')
	          return (PointField.INT8);
	        if (type == 'U')
	          return (PointField.UINT8);

	      case 2:
	        if (type == 'I')
	          return (PointField.INT16);
	        if (type == 'U')
	          return (PointField.UINT16);

	      case 4:
	        if (type == 'I')
	          return (PointField.INT32);
	        if (type == 'U')
	          return (PointField.UINT32);
	        if (type == 'F')
	          return (PointField.FLOAT32);

	      case 8:
	        return (PointField.FLOAT64);

	      default:
	        return (byte) -1;
	    }
	  }
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String key = request.getParameter("blobKey");
        BlobKey blobKey = new BlobKey(key);
		BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
		
		
		
		/* todo: support larger files */
		byte[] data = blobstoreService.fetchData(blobKey, 0,  BlobstoreService.MAX_BLOB_FETCH_SIZE-1);
        
		String pcdfile = new String(data);
		String[] pcdlines = pcdfile.split("\n");
		
		PointCloud2 cloud = new PointCloud2();
		
		int[] field_sizes = null;
		char[] field_types = null;
		
		int nr_points = 0;
		int specified_channel_count;
		// This code was translated from c++ to java
		//	https://github.com/PointCloudLibrary/pcl/blob/master/io/src/pcd_io.cpp
		int lineNo;
		for(lineNo = 0; lineNo < pcdlines.length; lineNo ++){
			String line = pcdlines[lineNo].trim();
			String st[]  = line.split("\t|\r| ");
			String line_type = st[0];
			
		    if (line_type.substring(0, 1) == "#")
		          continue;

		        // Version numbers are not needed for now, but we are checking to see if they're there
		    if (line_type.substring (0, 7) == "VERSION")
		          continue;

		        // Get the field indices (check for COLUMNS too for backwards compatibility)
		    if ( (line_type.substring (0, 6) == "FIELDS") || 
		 		(line_type.substring (0, 7) == "COLUMNS") )
		    {
		          specified_channel_count = (st.length - 1);

		          // Allocate enough memory to accommodate all fields
		          cloud.fields = new PointField[specified_channel_count];
		          for (int i1 = 0; i1 < specified_channel_count; ++i1)
		          {
		            String col_type = st[i1 + 1];
		            cloud.fields[i1].name = col_type;
		          }

		          // Default the sizes and the types of each field to float32 to avoid crashes while using older PCD files
		          int offset = 0;
		          for (int i = 0; i < specified_channel_count; ++i, offset += 4)
		          {
		            cloud.fields[i].offset   = offset;
		            cloud.fields[i].datatype = PointField.FLOAT32;
		            cloud.fields[i].count    = 1;
		          }
		          cloud.point_step = offset;
		          continue;
		        }

		        // Get the field sizes
		    if (line_type.substring (0, 4) == "SIZE")
		    {
		          specified_channel_count = st.length - 1;

		          // Allocate enough memory to accommodate all fields
		          if (specified_channel_count != cloud.fields.length)
		        	  throw new PCDException("The number of elements in <SIZE> differs than the number of elements in <FIELDS>!");

		          // Resize to accommodate the number of values
		          field_sizes = new int[specified_channel_count];
		          
		          
		          int offset = 0;
		          for (int i = 0; i < specified_channel_count; ++i)
		          {
		            int col_type ;
		            col_type = Integer.parseInt(st[i + 1]);
		            cloud.fields[i].offset = offset;                // estimate and save the data offsets
		            offset += col_type;
		            field_sizes[i] = col_type;                      // save a temporary copy
		          }
		          cloud.point_step = offset;
		          
		          if (cloud.width != 0)
		            cloud.row_step   = cloud.point_step * cloud.width;
		          continue;
		        }

		        // Get the field types
		        if (line_type.substring (0, 4) == "TYPE")
		        {
		        
		          if (field_sizes == null)
		        	  throw new PCDException( "TYPE of FIELDS specified before SIZE in header!");

		          specified_channel_count = st.length - 1;

		          // Allocate enough memory to accommodate all fields
		          if (specified_channel_count != cloud.fields.length)
		            throw new PCDException("The number of elements in <TYPE> differs than the number of elements in <FIELDS>!");

		          // Resize to accommodate the number of values
		          field_types = new char[specified_channel_count];

		          for (int i = 0; i < specified_channel_count; ++i)
		          {
		            field_types[i] = st[i + 1].charAt(0);
		            cloud.fields[i].datatype = getFieldType (field_sizes[i], field_types[i]);
		          }
		          
		          continue;
		        }

		        // Get the field counts
		        if (line_type.substring (0, 5) == "COUNT")
		        {
		        
		          if (field_sizes == null|| field_types == null)
		            throw new PCDException("COUNT of FIELDS specified before SIZE or TYPE in header!");

		          specified_channel_count = st.length - 1;

		          // Allocate enough memory to accommodate all fields
		          if (specified_channel_count != cloud.fields.length)
		            throw new PCDException("The number of elements in <COUNT> differs than the number of elements in <FIELDS>!");

		          int offset = 0;
		        
		          for (int i = 0; i < specified_channel_count; ++i)
		          {
		            cloud.fields[i].offset = offset;
		            int col_count;
		            col_count = Integer.parseInt(st[i + 1]);
		            cloud.fields[i].count = col_count;
		            offset += col_count * field_sizes[i];
		          }
		          // Adjust the offset for count (number of elements)
		          cloud.point_step = offset;
		          
		          continue;
		        }

		        // Get the width of the data (organized point cloud dataset)
		        if (line_type.substring (0, 5) == "WIDTH")
		        {
		          
		          cloud.width = Integer.parseInt(st[1]);
		          if (cloud.point_step != 0)
		            cloud.row_step = cloud.point_step * cloud.width;      // row_step only makes sense for organized datasets
		          
		          continue;
		        }

		        // Get the height of the data (organized point cloud dataset)
		        if (line_type.substring (0, 6) == "HEIGHT")
		        {
		        	cloud.height = Integer.parseInt(st[1]);
			        continue;
		        }

		        // Check the format of the acquisition viewpoint
		        if (line_type.substring (0, 9) == "VIEWPOINT")
		        {
		          if (st.length < 8)
		            throw new PCDException("Not enough number of elements in <VIEWPOINT>! Need 7 values (tx ty tz qw qx qy qz).");
		          continue;
		        }

		        // Get the number of points
		        if (line_type.substring (0, 6) == "POINTS")
		        {
		        
		          nr_points = Integer.parseInt(st[1]);
		        
		          // Need to allocate: N * point_step
		          cloud.data = new byte[nr_points * cloud.point_step];
		         
		          continue;
		        }	
		        /* done reading the header */
		        break;
		}
		
		
		for (int idx = 0; idx < nr_points && lineNo < pcdlines.length; lineNo ++)
	    {
	        String line = pcdlines[lineNo];
	        line = line.trim();
	        String[] st = line.split("\t\r ");
	        // Ignore empty lines
	        if (line == "")
	          continue;
	        
	        // Tokenize the line
	        

	        int total = 0;
	        // Copy data
	        for (int d = 0; d < (cloud.fields.length); ++d)
	        {
	          // Ignore invalid pad ded dimensions that are inherited from binary data
	          if (cloud.fields[d].name == "_")
	          {
	            total += cloud.fields[d].count; // jump over this many elements in the string token
	            continue;
	          }
	          for (int c = 0; c < cloud.fields[d].count; ++c)
	          {
	            switch (cloud.fields[d].datatype)
	            {
	            /* only support parsing floats for now */
	            /*
	              case PointField.INT8:
	              {
	                copyStringValue(st[total + c], cloud, idx, d, c);
	                break;
	              }
	              case PointField.UINT8:
	              {
	                copyStringValue<pcl::traits::asType<pcl::PCLPointField::UINT8>::type> (
	                    st.at (total + c), cloud, idx, d, c);
	                break;
	              }
	              case pcl::PCLPointField::INT16:
	              {
	                copyStringValue<pcl::traits::asType<pcl::PCLPointField::INT16>::type> (
	                    st.at (total + c), cloud, idx, d, c);
	                break;
	              }
	              case pcl::PCLPointField::UINT16:
	              {
	                copyStringValue<pcl::traits::asType<pcl::PCLPointField::UINT16>::type> (
	                    st.at (total + c), cloud, idx, d, c);
	                break;
	              }
	              case pcl::PCLPointField::INT32:
	              {
	                copyStringValue<pcl::traits::asType<pcl::PCLPointField::INT32>::type> (
	                    st.at (total + c), cloud, idx, d, c);
	                break;
	              }
	              case pcl::PCLPointField::UINT32:
	              {
	                copyStringValue<pcl::traits::asType<pcl::PCLPointField::UINT32>::type> (
	                    st.at (total + c), cloud, idx, d, c);
	                break;
	              }
	              */
	              case PointField.FLOAT32:
	              {
	                copyStringValueFloat(st[total + c], cloud, idx, d, c);
	                break;
	              }
	              /*
	              case PointField.FLOAT64:
	              {
	                copyStringValue(st[total + c], cloud, idx, d, c);
	                break;
	              }
	              */
	              default:
	                throw new PCDException("[pcl::PCDReader::read] Incorrect field data type specified ("+ cloud.fields[d].datatype +")!\n");
	            }
	          }
	          total += cloud.fields[d].count; // jump over this many elements in the string token
	        }
	        idx++;
	    }

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity entity = new Entity("PCDBlobs", key);
        entity.setProperty("BlobKey", blobKey);
        datastore.put(entity); 
        
     }
}