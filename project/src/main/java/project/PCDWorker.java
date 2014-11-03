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

import java.util.Arrays;
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
		byte[] data = blobstoreService.fetchData(blobKey, 0,  BlobstoreService.MAX_BLOB_FETCH_SIZE-1);
        
		String pcdfile = new String(data);
		String[] pcdlines = pcdfile.split("\n");
		
		int specified_channel_count;
		// This code was translated from c++ to java
		//	https://github.com/PointCloudLibrary/pcl/blob/master/io/src/pcd_io.cpp
		for(int i = 0; i < pcdlines.length; i ++){
			String line = pcdlines[i].trim();
			String tokens[]  = line.split("\t|\r| ");
			String line_type = tokens[0];
			String st[]      = Arrays.copyOfRange(tokens, 1, tokens.length);
			
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
		          //todo: cloud.fields.resize (specified_channel_count);
		          for (int i1 = 0; i1 < specified_channel_count; ++i1)
		          {
		            String col_type = st[i1 + 1];
		            //todo: cloud.fields[i].name = col_type;
		          }

		          // Default the sizes and the types of each field to float32 to avoid crashes while using older PCD files
		          int offset = 0;
		          for (int i1 = 0; i1 < specified_channel_count; ++i1, offset += 4)
		          {
		            //todo: cloud.fields[i1].offset   = offset;
		            //todo: cloud.fields[i1].datatype = pcl::PCLPointField::FLOAT32;
		            //todo: cloud.fields[i1].count    = 1;
		          }
		          //todo: cloud.point_step = offset;
		          continue;
		        }

		        // Get the field sizes
		    if (line_type.substring (0, 4) == "SIZE")
		    {
		          specified_channel_count = st.length - 1;

		          // Allocate enough memory to accommodate all fields
		          //todo: if (specified_channel_count != cloud.fields.size)
		          //  throw new PCDException("The number of elements in <SIZE> differs than the number of elements in <FIELDS>!");

		          // Resize to accommodate the number of values
		          //todo: field_sizes.resize (specified_channel_count);
		          
		          /*
		          int offset = 0;
		          for (int i1 = 0; i1 < specified_channel_count; ++i1)
		          {
		            int col_type ;
		            sstream >> col_type;
		            cloud.fields[i1].offset = offset;                // estimate and save the data offsets
		            offset += col_type;
		            field_sizes[i1] = col_type;                      // save a temporary copy
		          }
		          cloud.point_step = offset;
		          */
		          //if (cloud.width != 0)
		            //cloud.row_step   = cloud.point_step * cloud.width;
		          continue;
		        }

		        // Get the field types
		        if (line_type.substring (0, 4) == "TYPE")
		        {
		        	/*
		          if (field_sizes.empty ())
		        	  new PCDException(throw "TYPE of FIELDS specified before SIZE in header!");

		          specified_channel_count = static_cast<int> (st.size () - 1);

		          // Allocate enough memory to accommodate all fields
		          if (specified_channel_count != static_cast<int> (cloud.fields.size ()))
		            throw new PCDException("The number of elements in <TYPE> differs than the number of elements in <FIELDS>!");

		          // Resize to accommodate the number of values
		          field_types.resize (specified_channel_count);

		          for (int i = 0; i < specified_channel_count; ++i)
		          {
		            field_types[i] = st.at (i + 1).c_str ()[0];
		            cloud.fields[i].datatype = static_cast<uint8_t> (getFieldType (field_sizes[i], field_types[i]));
		          }
		          */
		          continue;
		        }

		        // Get the field counts
		        if (line_type.substring (0, 5) == "COUNT")
		        {
		        	/*
		          if (field_sizes.empty () || field_types.empty ())
		            throw new PCDException("COUNT of FIELDS specified before SIZE or TYPE in header!");

		          specified_channel_count = static_cast<int> (st.size () - 1);

		          // Allocate enough memory to accommodate all fields
		          if (specified_channel_count != static_cast<int> (cloud.fields.size ()))
		            throw new PCDException("The number of elements in <COUNT> differs than the number of elements in <FIELDS>!");

		          field_counts.resize (specified_channel_count);

		          int offset = 0;
		          for (int i = 0; i < specified_channel_count; ++i)
		          {
		            cloud.fields[i].offset = offset;
		            int col_count;
		            sstream >> col_count;
		            cloud.fields[i].count = col_count;
		            offset += col_count * field_sizes[i];
		          }
		          // Adjust the offset for count (number of elements)
		          cloud.point_step = offset;
		          */
		          continue;
		        }

		        // Get the width of the data (organized point cloud dataset)
		        if (line_type.substring (0, 5) == "WIDTH")
		        {
		          /*
		          sstream >> cloud.width;
		          if (cloud.point_step != 0)
		            cloud.row_step = cloud.point_step * cloud.width;      // row_step only makes sense for organized datasets
		          */
		          continue;
		        }

		        // Get the height of the data (organized point cloud dataset)
		        if (line_type.substring (0, 6) == "HEIGHT")
		        {
		          //sstream >> cloud.height;
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
		        	/*
		          sstream >> nr_points;
		          // Need to allocate: N * point_step
		          cloud.data.resize (nr_points * cloud.point_step);
		          */
		          continue;
		        }	
		        /* done reading the header */
		        break;
		}
		
		
		
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity entity = new Entity("PCDBlobs", key);
        entity.setProperty("BlobKey", blobKey);
        datastore.put(entity); 
        
     }
}