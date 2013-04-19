package sem.exception;

/**
 * Exception for unexpected errors when reading in the graph.
 *
 */
public class GraphFormatException extends Exception{
	private String message;
	private String line;
	private Integer type;

	public GraphFormatException(String message) {
		super(message);
		this.message = message;
		this.line = null;
		this.type = null;
    }
	
	public GraphFormatException(String message, String line) {
        super(message + " : " + line);
        this.message = message;
        this.line = line;
	this.type = null;
    }
	
    	public GraphFormatException(String message, Integer type) {
        super(message);
        this.message = message;
        this.line = null;
	this.type = type;
    }

	public GraphFormatException(String message, String line, Integer type) {
        super(message + " : " + line);
        this.message = message;
        this.line = line;
	this.type = type;
    }

	public Integer getType() {
		return this.type;
	}	

	public String getLine(){
		return this.line;
	}

	public void printLine(){
		System.out.println(this.getLine());
	}
}
