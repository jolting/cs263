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


import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;


// The Worker servlet should be mapped to the "/worker" URL.

public class GetPCD extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response){
		String pclkey = request.getParameter("key");
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Query query = new Query("PointCloud2", pclkey);
        List<Entity> pointclouds = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
        
		Entity pointCloud2 = pointclouds.get(0);
        PointCloud2 cloud = new PointCloud2();
        
		cloud.width = (Integer) pointCloud2.getProperty("width");	
        cloud.height = (Integer) pointCloud2.getProperty("height");
        cloud.is_bigendian = (Boolean) pointCloud2.getProperty("is_bigendian");
        cloud.row_step = (Integer) pointCloud2.getProperty("row_step");
        cloud.is_dense = (Boolean) pointCloud2.getProperty("is_dense");
        
        ServletOutputStream out = response.getOutputStream();
        out.println("# .PCD v0.7 - Point Cloud Data file format");
        out.println(getFieldstr(cloud));
        out.println(getTypeStr(cloud));
        out.println(getTypeStr(cloud));
        out.println(getCountStr(cloud));
        out.println("WIDTH " + cloud.width);
        out.println("HEIGHT " + cloud.height);
        out.println("VIEWPOINT 0 0 0 0 0 0 0");
        out.println("POINTS " + cloud.width * cloud.height);
        out.println("DATA binary");
        out.write(cloud.data.getBytes());
        out.flush();
    }


	private String getTypeStr(PointCloud2 cloud) {
		// TODO Auto-generated method stub
		return null;
	}


	private String getCountStr(PointCloud2 cloud) {
		// TODO Auto-generated method stub
		return null;
	}


	private String getFieldstr(PointCloud2 cloud) {
		// TODO Auto-generated method stub
		return null;
	}

}