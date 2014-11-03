package project;

public class PointField {
	
	public static final byte INT8    = 1;
	public static final byte UINT8   = 2;
	public static final byte INT16   = 3;
	public static final byte UINT16  = 4;
	public static final byte INT32   = 5;
	public static final byte UINT32  = 6;
	public static final byte FLOAT32 = 7;
	public static final byte FLOAT64 = 8;

	String name;
	int offset;
	byte datatype;
	int count;
}
