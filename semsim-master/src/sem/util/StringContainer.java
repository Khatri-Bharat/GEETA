package sem.util;

public class StringContainer {

	private String s;

	public StringContainer() {
		this.s = null;
	}	

	public void set(String sNew) {
		this.s = new String(sNew);
	}	

	public String get() {
		return this.s;
	}

	public boolean isNull() {
		return (s == null);
	}
}	
