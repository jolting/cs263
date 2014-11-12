package project;

public class PointField implements java.io.Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	PointField(){
		offset = new Integer(0);
		datatype = new Byte((byte) 0);
		count = new Integer(0);
	}
	
	public static final byte INT8    = 1;
	public static final byte UINT8   = 2;
	public static final byte INT16   = 3;
	public static final byte UINT16  = 4;
	public static final byte INT32   = 5;
	public static final byte UINT32  = 6;
	public static final byte FLOAT32 = 7;
	public static final byte FLOAT64 = 8;

	String name;
	Integer offset;
	Byte datatype;
	Integer count;
}
