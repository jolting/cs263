package project;

import java.nio.ByteBuffer;

import com.google.appengine.api.datastore.Blob;


public class PointCloud2 implements java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	PointCloud2()
	{
		width = new Integer(0);
		height = new Integer(0);
		
	}
	Header header;
	Integer width;
	Integer height;
	
	PointField [] fields;
	
	Boolean is_bigendian;
	
	Integer point_step;
	Integer row_step;
	
	Blob data;
	
	Boolean is_dense;

}
