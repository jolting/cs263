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
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.datastore.Key;

// The Worker servlet should be mapped to the "/worker" URL.
@SuppressWarnings("deprecation")
public class PCDWorker extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * This function takes a ascii floating value and inserts it into the correct
	 * position in the point cloud.
	 * 
	 * @param  st    Input string
	 * @param  cloud The cloud to insert the value into
	 * @param  point_index The point index that were are parsing.
	 * @param  field_idx   The field number we are parsing.
	 * @param  field_count The field counter.
	 */
	void
	copyStringValueFloat (String st, PointCloud2 cloud,
			int point_index, int field_idx, int fields_count)
	{
		float value;
		if (st.equals("nan"))
		{
			value = Float.NaN;
			cloud.is_dense = false;
		}
		else
		{
			value = Float.parseFloat(st);
		}

		ByteBuffer.wrap(cloud.data.getBytes()).order(ByteOrder.LITTLE_ENDIAN).putFloat(point_index*cloud.point_step + 
				cloud.fields[field_idx].offset + 
				fields_count * 4,value);

	}

	/**
	 * This function takes a ascii uint32 value and inserts it into the correct
	 * position in the point cloud.
	 * 
	 * @param  st    Input string
	 * @param  cloud The cloud to insert the value into
	 * @param  point_index The point index that were are parsing.
	 * @param  field_idx   The field number we are parsing.
	 * @param  field_count The field counter.
	 */

	void
	copyStringValueUINT32(String st, PointCloud2 cloud,
			int point_index, int field_idx, int fields_count) throws PCDException
	{
		int value;
		if (st.equals("nan"))
		{
			value = 0;
			cloud.is_dense = false;
		}
		else
		{
			value = (int)Long.parseLong(st);
		}
		/* unfortunately we don't have a uint32 version */
		cloud.data.getBytes()[point_index*cloud.point_step + 
		                      cloud.fields[field_idx].offset + 
		                      fields_count * 4] = (byte) (value);
		cloud.data.getBytes()[point_index*cloud.point_step + 
		                      cloud.fields[field_idx].offset + 
		                      fields_count * 4 + 1] = (byte) (value >> 8);
		cloud.data.getBytes()[point_index*cloud.point_step + 
		                      cloud.fields[field_idx].offset + 
		                      fields_count * 4 + 2] = (byte) (value >> 16);
		cloud.data.getBytes()[point_index*cloud.point_step + 
		                      cloud.fields[field_idx].offset + 
		                      fields_count * 4 + 3] = (byte) (value >> 24);

	}

	/**
	 * Returns the Field type 
	 * 
	 * @param  size    Number of bytes for the field.
	 * @param  type    'I' for integer, 'U' for unsigned integer, 'F' for float.
	 * @return type enumeration.
	 */

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

	/**
	 * The method processes a pcd file and puts point clouds into the datastore and blobstore.
	 * 
	 * @param  request The servlet request object.
	 * @param  response The response object.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		String key = request.getParameter("blobKey");
		
		/* really can't do anything if this is null */
		if(key == null)
			return;
		
		try{
			BlobKey blobKey = new BlobKey(key);
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();

			BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);

			if( blobInfo == null )
				return;

			if( blobInfo.getSize() > Integer.MAX_VALUE )
				throw new RuntimeException("This method can only process blobs up to " + Integer.MAX_VALUE + " bytes");

			int blobSize = (int)blobInfo.getSize();
			int chunks = (int)Math.ceil(((double)blobSize / BlobstoreService.MAX_BLOB_FETCH_SIZE));
			int totalBytesRead = 0;
			int startPointer = 0;
			int endPointer;
			byte[] blobBytes = new byte[blobSize];

			for( int i = 0; i < chunks; i++ ){

				endPointer = Math.min(blobSize - 1, startPointer + BlobstoreService.MAX_BLOB_FETCH_SIZE - 1);

				byte[] bytes = blobstoreService.fetchData(blobKey, startPointer, endPointer);


				for( int j = 0; j < bytes.length; j++ )
					blobBytes[j + totalBytesRead] = bytes[j];

				startPointer = endPointer + 1;
				totalBytesRead += bytes.length;

			}



			String pcdfile = new String(blobBytes);
			String[] pcdlines = pcdfile.split("\n");

			PointCloud2 cloud = new PointCloud2();

			cloud.is_bigendian = new Boolean(false);

			int[] field_sizes = null;
			char[] field_types = null;

			int nr_points = 0;
			int specified_channel_count;
			int textMode = 0;
			// This code was translated from c++ to java
			//	https://github.com/PointCloudLibrary/pcl/blob/master/io/src/pcd_io.cpp
			int lineNo;

			for(lineNo = 0; lineNo < pcdlines.length; lineNo ++){
				String line = pcdlines[lineNo].trim();
				String st[]  = line.split("\t|\r| ");
				TestString line_type = new TestString(line);

				if (line_type.substring(0, 1).equals("#"))
					continue;

				// Version numbers are not needed for now, but we are checking to see if they're there
				if (line_type.substring (0, 7).equals("VERSION"))
					continue;

				// Get the field indices (check for COLUMNS too for backwards compatibility)
				if ( (line_type.substring (0, 6).equals("FIELDS")) || 
						(line_type.substring (0, 7).equals("COLUMNS")) )
				{
					specified_channel_count = (st.length - 1);

					// Allocate enough memory to accommodate all fields
					cloud.fields = new PointField[specified_channel_count];
					for (int i = 0; i < specified_channel_count; ++i)
					{
						String col_type = st[i + 1];
						cloud.fields[i] = new PointField();
						cloud.fields[i].name = col_type;
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
				if (line_type.substring (0, 4).equals("SIZE"))
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
				if (line_type.substring (0, 4).equals("TYPE"))
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
				if (line_type.substring (0, 5).equals("COUNT"))
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
				if (line_type.substring (0, 5).equals("WIDTH"))
				{

					cloud.width = Integer.parseInt(st[1]);
					if (cloud.point_step != 0)
						cloud.row_step = cloud.point_step * cloud.width;      // row_step only makes sense for organized datasets

					continue;
				}

				// Get the height of the data (organized point cloud dataset)
				if (line_type.substring (0, 6).equals("HEIGHT"))
				{
					cloud.height = Integer.parseInt(st[1]);
					continue;
				}

				// Check the format of the acquisition viewpoint
				if (line_type.substring (0, 9).equals("VIEWPOINT"))
				{
					if (st.length < 8)
						throw new PCDException("Not enough number of elements in <VIEWPOINT>! Need 7 values (tx ty tz qw qx qy qz).");
					continue;
				}

				// Get the number of points
				if (line_type.substring (0, 6).equals("POINTS"))
				{

					nr_points = Integer.parseInt(st[1]);

					// Need to allocate: N * point_step
					cloud.data = new Blob(new byte[nr_points * cloud.point_step]);
					continue;
				}
				if (line_type.substring (0, 4).equals("DATA"))
				{
					if(st[1].equals("ascii"))
						textMode = 1;
					else if(st[1].equals("binary"))
						textMode = 0;
					else
						throw new PCDException("binary compressed not supported: " + st[1]);
					continue;
				}	
				/* done reading the header */
				break;
			}

			if(textMode == 1)
			{
				for (int idx = 0; idx < nr_points && lineNo < pcdlines.length; lineNo ++)
				{

					String line = pcdlines[lineNo];
					line = line.trim();
					// Ignore empty lines
					if (line.equals(""))
						continue;

					String[] st = line.split("\t|\r| ");

					// Tokenize the line


					int total = 0;
					// Copy data
					for (int d = 0; d < (cloud.fields.length); ++d)
					{
						// Ignore invalid pad ded dimensions that are inherited from binary data
						if (cloud.fields[d].name.equals("_"))
						{
							total += cloud.fields[d].count; // jump over this many elements in the string token
							continue;
						}
						for (int c = 0; c < cloud.fields[d].count; ++c)
						{
							switch (cloud.fields[d].datatype)
							{

							case PointField.UINT32:
							{
								copyStringValueUINT32(st[total + c], cloud, idx, d, c);
								break;
							}

							case PointField.FLOAT32:
							{
								copyStringValueFloat(st[total + c], cloud, idx, d, c);
								break;
							}
							default:
								throw new PCDException("[pcl::PCDReader::read] Incorrect field data type specified ("+ cloud.fields[d].datatype +")!\n");
							}

						}
						total += cloud.fields[d].count; // jump over this many elements in the string token
					}
					idx++;
				}
			}
			//binary mode
			else
			{
				int i;
				int j = 0;
				/* find a bunch of newlines */
				for(i = 0; i < lineNo; i++)
				{
					while(blobBytes[j++] != '\n');
				}
				
				cloud.data = new Blob(Arrays.copyOfRange(blobBytes, j, blobBytes.length)); 	
			}

			Key pclKey = KeyFactory.createKey("PointCloud2", key);
			Entity pointCloud2 = new Entity(pclKey);
			pointCloud2.setProperty("width", cloud.width);
			pointCloud2.setProperty("height", cloud.height);
			pointCloud2.setProperty("is_bigendian", cloud.is_bigendian);
			pointCloud2.setProperty("row_step", cloud.row_step);


			int idx = 0;
			for(PointField field :cloud.fields)
			{
				Entity efield = new Entity("PointField");
				efield.setProperty("cloud",key);
				efield.setProperty("count", field.count); 
				efield.setProperty("name", field.name); 
				efield.setProperty("datatype", field.datatype); 
				efield.setProperty("offset", field.offset);
				efield.setProperty("idx", idx);
				idx++;
				datastore.put(efield);
			}

			pointCloud2.setProperty("is_dense", cloud.is_dense);


			/* too bad this is deprecated */
			FileService fileService = FileServiceFactory.getFileService();

			AppEngineFile file = fileService.createNewBlobFile("UTF8");

			FileWriteChannel writeChannel = fileService.openWriteChannel(file, true);

			writeChannel.write(ByteBuffer.wrap((cloud.data.getBytes())));
			writeChannel.closeFinally();

			// all done delete the original blob
			blobstoreService.delete(blobKey);

			pointCloud2.setProperty("data", fileService.getBlobKey(file));

			datastore.put(pointCloud2);
			Key pclStatusKey = KeyFactory.createKey("PointCloudConversionStatus", key);
			Entity convertStatus = new Entity(pclStatusKey);
			convertStatus.setProperty("failed", 0);
			datastore.put(convertStatus);
		}
		catch(Exception e)
		{
			Key pclStatusKey = KeyFactory.createKey("PointCloudConversionStatus", key);
			Entity convertStatus = new Entity(pclStatusKey);
			convertStatus.setProperty("failed", 1);
			datastore.put(convertStatus);
		}

	}

}
