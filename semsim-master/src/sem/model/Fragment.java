package sem.model;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;


import sem.graph.Graph;
import sem.graph.Edge;
import sem.util.Index;
import sem.exception.GraphFormatException;

class Enumerator {

	private List<Edge> edges;
	private List<List<Edge>> completeEdgeReps;
	private Index nodeIndex;
	
	public Enumerator(List<Edge> edges, Index nodeIndex) throws GraphFormatException
	{
		this.edges = edges;
		this.nodeIndex = nodeIndex;
		completeEdgeReps = new ArrayList<List<Edge>>();
		init();
	}

	private void init() throws GraphFormatException
	{	
		List<Edge> left = new ArrayList<Edge>(edges);
		List<Edge> selected = new ArrayList<Edge>();
		enumerate(left, selected);
	}

	/* Checks whether a given set of edges is 'COMPLETE' or not. Not every edges representation can represent a node subgraph. We need to filter out
	 * the edges representations which cannot represent any node subgraphs. 
	 * Let A and B be logical statements denoting the following:
	 *	A = the edges representation represents a dependency graph.
	 *	B = the nodes (PART OF ANY EDGE of the edges representation) must be 'FULLY' connected.
	 * Now, A -> B <=> B' -> A' where X' denotes the complement of X. Hence, a violation of B would imply a violation of A. But if B holds true, then 
	 * we can find atleast one node subgraph (one with the set of nodes we're working on) represented by our edges representation.
	 */	
	private boolean checkCompleteEdgeRep(List<Edge> selected) throws GraphFormatException
	{
		/* First find the set of nodes contained within these edges. */
		Set<Integer> nodeSet = new HashSet<Integer>();
		List<Edge> completeEdgeList = new ArrayList<Edge>();
		for (Iterator it = selected.iterator(); it.hasNext();)
		{
			Edge e = (Edge) it.next();
			//nodeSet.add(nodeIndex.getId(e.getHead().getLabel()));
			//nodeSet.add(nodeIndex.getId(e.getDep().getLabel()));
			if (!e.hasMarker())
				throw new GraphFormatException("Fragment.java:checkCompleteEdgeRep() Encountered an unmarked edge", e.toString());
			nodeSet.add(e.getHeadLoc());
			nodeSet.add(e.getDepLoc());
		}			
		/* nodeSet contains the set of nodes associated with any of the edges of the edges representation. */ 			
		/* Now, we need to check if these nodes are fully connected or not (the validity of 'B'). */
		for (Iterator it = edges.iterator(); it.hasNext();)
		{
			Edge e = (Edge) it.next();
			if (!e.hasMarker())
				throw new GraphFormatException("Fragment.java:checkCompleteEdgeRep() Encountered an unmarked edge", e.toString());
			//Integer e1 = nodeIndex.getId(e.getHead().getLabel());
			//Integer e2 = nodeIndex.getId(e.getDep().getLabel());
			if (nodeSet.contains(e.getHeadLoc()) && nodeSet.contains(e.getDepLoc()))
				completeEdgeList.add(e);
		}
		/* It is easy to see that the following condition is NECESSARY, and it's not hard to prove that the condition is SUFFICIENT
		 * as well. Every element of 'selected' must be a part of 'completeEdgeList' since it's MAXIMAL, so 'selected' is a subset of
		 * of 'completeEdgeList'. A subset of size equal to the parent set can be noting but the parent set ITSELF. Hence, the SUFFICIENT
		 * criterion is proved.
		 */
				
		if (completeEdgeList.size() == selected.size())
			return true;
		return false;
	}

	private void putCompleteEdgeRep(List<Edge> selected)
	{
		this.completeEdgeReps.add(selected);
	}
	
	public List<List<Edge>> getCompleteEdgeReps()
	{
		return this.completeEdgeReps;
	}

	private void enumerate(List<Edge> left, List<Edge> selected) throws GraphFormatException
	{
		if (left.size() == 0)
		{
			if (checkCompleteEdgeRep(selected))
				putCompleteEdgeRep(selected);	
			return;
		}

		Edge e = left.get(left.size()-1);
		left.remove(left.size()-1);
		
		enumerate(left, selected);
		
		selected.add(e);
		enumerate(left, selected);

		/* restoring the states of left and selected before returning to the parent function. */
		/* Keeping the states of 'left' and 'selected' intact before and after a function call is a part of the strategy used for recursion here. */
		left.add(e);	
		selected.remove(selected.size()-1);
	}
}

public class Fragment {

/* featureCountMap stores the # of times a feature occurs with a fragment in the corpus. */
private Map<Integer, Integer> featureCountMap;
private Index featureIndex;
private Index nodeIndex;
private Index edgeIndex;

public Fragment(Index featureIndex, Index nodeIndex, Index edgeIndex) {
	featureCountMap = new HashMap<Integer, Integer>();
	this.featureIndex = featureIndex;
	this.nodeIndex = nodeIndex;
	this.edgeIndex = edgeIndex;
}
	
/* Finds the features of a fragment and updates the 'featureCountMap' appropriately. */ 
private void putStats(List<Edge> completeEdgeRep, List<Edge> edges) throws GraphFormatException
{
	/* 'completeEdgeRep' represents a fragment i.e. a CONNECTED NODE SUBGRAPH */
	Set<Integer> nodeSet = new HashSet<Integer>();
	for (Iterator it = completeEdgeRep.iterator(); it.hasNext();)
	{
		Edge e = (Edge) it.next();
		if (!e.hasMarker())
			throw new GraphFormatException("Fragment.java:putStats() Encountered an unmarked edge", e.toString());
		nodeSet.add(e.getHeadLoc());
		nodeSet.add(e.getDepLoc());
		//nodeSet.add(nodeIndex.getId(e.getHead().getLabel()));			
		//nodeSet.add(nodeIndex.getId(e.getDep().getLabel()));			
	}
	/* Every 'completeEdgeRep' represents a UNIQUE connected dependency graph. */
	for(Iterator it = edges.iterator(); it.hasNext();)
	{
		Edge e = (Edge) it.next();
		if (!e.hasMarker())
			throw new GraphFormatException("Fragment.java:putStats() Encountered an unmarked edge", e.toString());
		Integer e1 = e.getHeadLoc();
		Integer e2 = e.getDepLoc();

		//Integer e1 = nodeIndex.getId(e.getHead().getLabel());
		//Integer e2 = nodeIndex.getId(e.getDep().getLabel());
		if ( (nodeSet.contains(e1) && nodeSet.contains(e2)) || !(nodeSet.contains(e1) || nodeSet.contains(e2)) )
			continue;
		else
		{
			/* This edge is a feature of this fragment. */
			Integer edgeId = this.edgeIndex.getId(e.getLabel());
			String featureLabel = edgeId + "," + nodeIndex.getId(e.getDep().getLabel());
			Integer featureId = featureIndex.getId(featureLabel);
			if (featureCountMap.containsKey(featureId))
				featureCountMap.put(featureId, featureCountMap.get(featureId)+1);
			else
				featureCountMap.put(featureId,1);		
		}
	}
}


/* A fragment is any (weakly) connnected subgraph of a dependency graph. Weak connectivity is defined as follows: for every node (i) there exists
 * a node (j) such that either (i) -> (j) or (j) -> (i). We need to enumerate all the fragments of all the dependency graphs within the corpus in
 * order to compute P(*,f), which requires us to calculate the # of times a particular feature 'f' occurs with any fragment within the corpus. 
 * DEFINITION: A fragment is any connected subgraph of a directed dependency graph containing one or more words and the grammatical relations between them.
 * We won't however consider fragments that span across sentences or documents (even if they actually satisfy the definition of a fragment) since such
 * fragments might not serve any purpose as far as their semantic meanings are concerned.
 * We can enumerate the set of all CONNECTED node subgraphs through enumerating the set of 'COMPLETE' edge representations. Every connected subgraph has 
 * a 'COMPLETE' edges representation. And given a 'COMPLETE' edges representation, we have a UNIQUE connected subgraph. The notion of 'COMPLETE' edges 
 * representation arises from the question of whether every edges representation has a corresponding node subgraph or not. It occurs that only 'COMPLETE' 
 * edges representations have a corresponding node subgraph.
 * No connected node subgraph could be left since every such graph has a 'COMPLETE' edges representation and all such edges representations are enumerated
 * where each of them produces a UNIQUE connected node subgraph.
 */

public void buildStats(Map<Integer, List<Graph>> sentenceGraphsMap)
{
	for (Iterator it = sentenceGraphsMap.entrySet().iterator(); it.hasNext();)
	{
		Map.Entry e = (Map.Entry) it.next();
		/* TODO We, at present, only consider a single valid parse of each sentence in the corpus. Hence, the index '0' in the following statement. */
		List<Graph> gList = (List) e.getValue();
		Graph g = gList.get(0);
		List<Edge> edges = g.getEdges();
		Enumerator enumerator = null;
		/* We need to enumerate all the 'COMPLETE' edges representations. */
		try {
			enumerator = new Enumerator(edges, nodeIndex);				
		}
		catch(GraphFormatException ex) {
			System.out.println(ex.getMessage());
			ex.printLine();
			ex.printStackTrace();
			continue;
		}
		List<List<Edge>> completeEdgeReps = enumerator.getCompleteEdgeReps();
		for (Iterator iu = completeEdgeReps.iterator(); iu.hasNext();)
		{
			List<Edge> completeEdgeRep = (List) iu.next();
			try {
				putStats(completeEdgeRep, edges);
			}
			catch(GraphFormatException ex)
			{
				System.out.println(ex.getMessage());
				ex.printLine();
				ex.printStackTrace();
				break;	
			}
		}  
	}	
}	

public Map<Integer, Integer> getFeatureCountMap()
{
	return this.featureCountMap;
}

}
