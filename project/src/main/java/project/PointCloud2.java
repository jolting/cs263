package project;


public class PointCloud2 {
	Header header;
	int width;
	int height;
	
	PointField [] fields;
	
	Boolean is_bigendian;
	
	int point_step;
	int row_step;
	
	byte[] data;
	
	Boolean is_dense;

}
