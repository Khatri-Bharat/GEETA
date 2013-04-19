package sem.graph;

/**
 * A directed edge from head to dependent, connecting two nodes.
 *
 */

public class Edge {
	private String label;
	private Node head;
	private Node dep;
	private Integer headLoc;
	private Integer depLoc;
	private boolean useMarkers;
	/**
	 * Create a new edge between two nodes.
	 * @param label The relation type.
	 * @param head Head node.
	 * @param dep Dependent node.
	 */
	public Edge(String label, Node head, Node dep) {
		this.label = label;
		this.head = head;
		this.dep = dep;
		this.useMarkers = false;
		this.headLoc = null;
		this.depLoc = null;
	}

	public Edge(String label, Node head, Node dep, Integer headLoc, Integer depLoc) {
		this.label = label;
		this.head = head;
		this.dep = dep;
		this.useMarkers = true;
		this.headLoc = headLoc;
	       	this.depLoc = depLoc;
	}	

	public Integer getHeadLoc()
	{
		if (this.useMarkers)
			return this.headLoc;
		return null;	
	}	

	public Integer getDepLoc()
	{
		if (this.useMarkers)
			return this.depLoc;
		return null;
	}	

	public boolean hasMarker()
	{
		return this.useMarkers;
	}	

	public boolean setHeadLoc(Integer headLoc)
	{
		if (!this.useMarkers)
			return false;	
		this.headLoc = headLoc;
		return true;
	}	
	
	public boolean setDepLoc(Integer depLoc)
	{
		if (!this.useMarkers)
			return false;	
		this.depLoc = depLoc;
		return true;
	}

	/**
	 * Get the head node.
	 * @return Head node.
	 */
	public Node getHead() {
		return head;
	}
	
	/**
	 * Get the dependent node.
	 * @return Dependent node.
	 */
	public Node getDep() {
		return dep;
	}
	
	/**
	 * Get the relation type of this edge.
	 * @return Relation type.
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Set the relation type of this edge.
	 * @param label Relation type.
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	
	/**
	 * Print the edge information to System.out.
	 */
	public void print(){
		System.out.println(this.toString());
	}
	
	/**
	 * Create a string representation of the edge. Includes the label of the edge, the head and the dependent.
	 */
	@Override
	public String toString(){
		return this.getLabel() + " " + head.getLabel() +  " " + dep.getLabel();
	}
}
