package project;

public class TestString{
	String s_;
	public TestString(String s){
		s_ = s;
	}
	public TestString substring(int begin, int end)
	{
		end = Math.min(s_.length(), end);
		return new TestString(s_.substring(begin, end));
	}

	Boolean equals(String str)
	{
		return s_.equals(str);
	}
	
}
