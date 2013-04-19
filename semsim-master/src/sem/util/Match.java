package sem.util;

import java.util.List;
import sem.graph.Edge;
/* The following is essentialy a 'struct' and doesn't have a 'behavior' associated with itself. */

public class Match {
	
	public Integer sentenceId;
	public List<List<Edge>> matchedFragmentList;
	
	public Match(Integer sentenceId, List<List<Edge>> matchedFragmentList)
	{
		this.sentenceId = sentenceId;
		this.matchedFragmentList = matchedFragmentList;
	}	
}
