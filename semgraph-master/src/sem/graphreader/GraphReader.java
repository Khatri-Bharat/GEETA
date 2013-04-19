package sem.graphreader;

import java.util.ArrayList;

import sem.exception.GraphFormatException;
import sem.graph.Graph;

/* Khatri */
import sem.util.StringContainer;
/* Khatri */

/**
 * The general interface for a graph reader.
 *
 */
public interface GraphReader {
	public boolean hasNext();
	public Graph next() throws GraphFormatException;
	public Graph next(StringContainer sContainer) throws GraphFormatException;
	public ArrayList<Graph> nextSentence() throws GraphFormatException;
    public void reset() throws GraphFormatException;
    public void close();
}
