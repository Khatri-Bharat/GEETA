package sem.util;

import java.util.*;
import java.io.IOException;

import sem.exception.GraphFormatException;
import sem.graph.Graph;
import sem.graph.Node;
import sem.graph.Edge;
import sem.graphreader.GraphReader;
import sem.graphreader.RaspGraphReader;
import sem.grapheditor.GraphEditor;
import sem.grapheditor.LowerCaseGraphEditor;
import sem.grapheditor.NumTagsGraphEditor;
import sem.util.Index;
import sem.util.Gadgets;
import sem.util.Match;
import sem.util.BucketGroup;
import sem.util.StringContainer;
import sem.model.SemModelAux;
import sem.exception.GraphFormatException;
/* This class serves serves as an interface to the core for finding entailment any two text fragments. */


public class FragmentProcessor
{
	private class BucketGroup
	{
		public Map<Integer, List<Integer>> bucketMap;
		public Map<Integer, List<Integer>> bucketSelectionsMap;
		public Map<Integer, Integer> countMap;
		public List<Edge> edges;
		public List<Edge> fragmentEdges;
		public List<List<Edge>> matchedFragmentList;
		
		public BucketGroup(Map<Integer, List<Integer>> bucketMap, Map<Integer, List<Integer>> bucketSelectionsMap, Map<Integer, Integer> countMap, List<List<Edge>> matchedFragmentList, List<Edge> edges, List<Edge> fragmentEdges) {
			this.countMap = countMap;
			this.bucketMap = bucketMap;
			this.bucketSelectionsMap = bucketSelectionsMap;
			this.edges = edges;
			this.fragmentEdges = fragmentEdges;
			this.matchedFragmentList = matchedFragmentList;
		}
	}
	
	private SemModelAux semModelAux;
	private Index nodeIndex;

	/*TODO The constructor has been changed, so make the required changes elsewere. */
	public FragmentProcessor(SemModelAux semModelAux, Index nodeIndex)
	{
		this.semModelAux = semModelAux;
		this.nodeIndex = nodeIndex;
	}       	
	
	private Map<String, Graph> getDependencyGraphs(String filename)
	{
		Map<String, Graph> dependencyGraphsMap = new HashMap<String, Graph>();
		try
		{	
			GraphReader reader = new RaspGraphReader(filename, false);
			ArrayList<GraphEditor> graphEditors = new ArrayList<GraphEditor>(Arrays.asList(new LowerCaseGraphEditor()/*, new NumTagsGraphEditor()*/));
			while (reader.hasNext())
			{
				StringContainer sContainer = new StringContainer();
				Graph graph = reader.next(sContainer);
				
				for(GraphEditor graphEditor : graphEditors)
					graphEditor.edit(graph);

				String sentence = sContainer.get();
				if (sentence != null && !dependencyGraphsMap.containsKey(sentence))
				{	
					/*System.out.println("%%%%%%  In getDependencyGraphs() %%%%%%%");
					System.out.println("%%%%% List for " + sentence + "%%%%%");
					List<Node> gNodeList = graph.getNodes();
					for (int i = 0; i < gNodeList.size(); i++)
					{
						System.out.println(gNodeList.get(i).getLabel());
					}
					int inChar;
					try {
						inChar = System.in.read();
						inChar = System.in.read();
					}
					catch (IOException io)
					{
					
					}*/	

					dependencyGraphsMap.put(sentence, graph);
				}	
			}		
			reader.close();	
		} catch (GraphFormatException e) {
			e.printLine();
			e.printStackTrace();
		}
		
		return dependencyGraphsMap;
	}	
	

	private void printRepSentenceMatches(List<Integer> repSentenceMatches) {
		if (repSentenceMatches == null)
			return;
		Index sentenceIndex = this.semModelAux.getSentenceIndex();
		for (int i = 0; i < repSentenceMatches.size(); i++) {
			Integer sentenceId = repSentenceMatches.get(i);
			String sentence = sentenceIndex.getLabel(sentenceId);	
			System.out.println(sentence);
		}	
	}

	/* Returns the smallest list of all the sentenceId lists corresponding to the nodeIds contained within the text fragment. */
	private List<Integer> getRepSentenceMatches(Map<Integer, Integer> wordDistCrit)
	{
		//System.out.println("%%%%%%% In getRepSentenceMatches() %%%%%%%%%");
		//Gadgets.printWordDist(wordDistCrit, this.nodeIndex);	
		
		int repSize = -1;
		List<Integer> repSentenceMatches = null;
		for (Iterator it = wordDistCrit.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry pair = (Map.Entry) it.next();
			Integer nodeId = (Integer) pair.getKey();
			Integer nodeCount = (Integer) pair.getValue();
			//System.out.println("Finding sentences in the corpus where " + this.nodeIndex.getLabel(nodeId) + " => " + nodeCount);
			List<Integer> sentenceMatches = semModelAux.getSentenceMatches(nodeId, nodeCount);
			//System.out.println("Sentence Matches for " + this.nodeIndex.getLabel(nodeId));
			//printRepSentenceMatches(sentenceMatches);	
			
			if (sentenceMatches == null)
				return null;
			Integer curSize = sentenceMatches.size();
			if (repSize < 0 || repSize > curSize)
			{	
				repSize = curSize;
				repSentenceMatches = sentenceMatches;
			}
		}
		return repSentenceMatches;
	}	


	


	/* @filename consists of text fragments parsed by RASP. */
	/* TODO change the function at the places where it is called since the function's interface has been changed. */
	private Map<String, List<Integer>> getProbSentenceMatches(Map<String, Graph> dependencyGraphsMap)
	{
		Map<String, List<Integer>> probSentenceMatchesMap = new HashMap<String, List<Integer>>();
		/* This map is also needed in getSentenceMatches() function. */
		for (Iterator it = dependencyGraphsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			String sentence = (String) e.getKey();
			Graph graph = (Graph) e.getValue();
			//System.out.println("Finding probable sentence matches for :: " + sentence);
			
			//Gadgets.printNodeList(sentence, graph);	

			/* Step 1 */
			Map<Integer, Integer> wordDistCrit = null;
			try {
				wordDistCrit = Gadgets.getWordDist(graph, this.nodeIndex);
			}	
			catch (GraphFormatException ex) {
				System.out.println(ex.getMessage());
				ex.printLine();
				ex.printStackTrace();
				continue;		
			}	
			/* Step 2 */
			if (wordDistCrit != null)
			{	
				/* TODO REMOVE_ME */
				
				System.out.println("Finding representative sentences for " + sentence + "\n");
				Gadgets.printWordDist(wordDistCrit, this.nodeIndex);	
				System.out.println("");	

				List<Integer> repSentenceMatches = getRepSentenceMatches(wordDistCrit);
				
							
				/* Step 3 */ 
				/* Find the probable sentences which might contain a given text fragment as a subgraph of themselves. */
				/* 'probSentenceMatches' gives the sentence ids of all the sentences in the corpus which fulfill the node criteria
				 * required for them to contain the current text fragment as a subgraph of themselves. */
				if (repSentenceMatches != null)
				{	
					System.out.println("Printing representative sentences ...");
					printRepSentenceMatches(repSentenceMatches);
					System.out.println("");
					//System.out.println("Word Distribution for " + sentence);
					//Gadgets.printWordDist(wordDistCrit, this.nodeIndex);	
					//System.out.println("");	
	
					List<Integer> probSentenceMatches = semModelAux.getIntersection(repSentenceMatches, wordDistCrit, this.nodeIndex);
					if (probSentenceMatches != null) {
						System.out.println("Printing probable match sentences matches ... ");
						printRepSentenceMatches(probSentenceMatches);
						System.out.println("");	
						probSentenceMatchesMap.put(sentence, probSentenceMatches);
					}	
				}
				else
				{
					System.out.println("No representative sentences found." + "\n");	
				}	
			}
		}	
		/* Now, we need to perform exact matching between the dependency graphs of the text fragment and the dependency graphs of 
		 * probSentenceMatches. */
		return probSentenceMatchesMap;
	}

	private boolean isConnected(BucketGroup buckets) throws GraphFormatException {
		Map<Integer, List<Integer>> nodeSelectionsMap = buckets.bucketSelectionsMap;
		List<Edge> edges = buckets.edges;
		/* We have to check if every selected node is involved in an 'edge' of the graph or not. */
		Set<Integer> connectedNodeSet = new HashSet<Integer>(); 
		for (int i = 0; i < edges.size(); i++) {
			Edge e = edges.get(i);
			if (!e.hasMarker())
				throw new GraphFormatException("Encountered an unmarked edge.", e.toString(), 0);
			int headLoc = e.getHeadLoc();
			int depLoc = e.getDepLoc();
			int headId = this.nodeIndex.getId(e.getHead().getLabel());
			int depId = this.nodeIndex.getId(e.getDep().getLabel());
			if (nodeSelectionsMap.containsKey(headId) && nodeSelectionsMap.containsKey(depId))  {
				if (nodeSelectionsMap.get(headId).contains(headLoc) && nodeSelectionsMap.get(depId).contains(depLoc)) {
					connectedNodeSet.add(headLoc);
					connectedNodeSet.add(depLoc);
				}	
			}	
		}
		int nodeCount = 0;
		for (Iterator it = nodeSelectionsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			List selectionsList = (List) e.getValue();  
			nodeCount += selectionsList.size();
		}	
		if (nodeCount == connectedNodeSet.size())
			return true;
		return false;
	}

	private List<Edge> getEdgeMatches(List<Edge> edges, Edge e)
	{
		int headId = this.nodeIndex.getId(e.getHead().getLabel());	
		int depId = this.nodeIndex.getId(e.getDep().getLabel());
		List<Edge> eMatches = new ArrayList<Edge>();
		for (int i = 0; i < edges.size(); i++)
		{
			Edge edge = edges.get(i);
			/*TODO check if  nodeIndex returns a null. */
			int headIdMatch = this.nodeIndex.getId(edge.getHead().getLabel());	
			int depIdMatch = this.nodeIndex.getId(edge.getDep().getLabel());
			/*System.out.println("%%% In getEdgeMatches() %%%");
			System.out.println(e.getHead().getLabel() + "," + headId + " : " + edge.getHead().getLabel() + "," + headIdMatch);
			System.out.println(e.getDep().getLabel() + "," + depId + " : " + edge.getDep().getLabel() + "," + depIdMatch);
			System.out.println(e.getLabel() + " : " + edge.getLabel());
			if (headId == headIdMatch)
				System.out.println("Head matches");
			if (depId == depIdMatch)
				System.out.println("Dependent matches");
			if (e.getLabel().equals(edge.getLabel()))
				System.out.println("Edge Label matches");*/
			if ( (headId == headIdMatch) && (depId == depIdMatch) && (e.getLabel().equals(edge.getLabel())) )
				eMatches.add(edge);
		}
		return eMatches;	
	}

	private void printEdgeList(List<Edge> list)
	{
		for(int i = 0; i < list.size(); i++) {
			Edge e = list.get(i);
			System.out.println(e.getHead().getLabel() + " => " + e.getDep().getLabel() + ", " + e.getHeadLoc() + " => " + e.getDepLoc());
		}	
	}	

	private void printEdgeMatches(List<Edge> list, Edge e)
	{

		System.out.println("%%%%%%%%% Matching edges for " + e.getHead().getLabel() + " => " + e.getDep().getLabel() + ", " + e.getHeadLoc() + " => " + e.getDepLoc() + "%%%%%%%%%%");
		for (int i = 0; i < list.size(); i++) {
		Edge edge = list.get(i);
		System.out.println(edge.getHead().getLabel() + " => " + edge.getDep().getLabel() + ", " + edge.getHeadLoc() + " => " + edge.getDepLoc());
		}
		System.out.println("");
	}	

	private void printMatchingsMap(Map<Integer, Integer> matchingsMap) {
		for (Iterator it = matchingsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			Integer key = (Integer) e.getKey();
			Integer value = (Integer) e.getValue();
			System.out.println(key + " => " + value);
		}	
	}	


	/* The 'isExactMatch' method is a recursive procedure. It recurses over all possible matchings of nodes until it finds any one match that 
	 * satisfies the subgraph problem. 
	 */
	
	/* TODO Include this check in the caller	if (edges.size() == fragmentEdges.size() && fragmentEdges.size() > 0)
								if (isExactMatch(edges, fragmentEdges, matchingsMap))
								{}							
	Make sure to have 'nodeIndex' available for this function. 	
	*/
	/* A 'true' return value propagates right up the root whereas a 'false' return value results in the adoption of a new search path until all the 
	 * possible paths are explored. 
	 * [explanation] The matchings already done during the exploration of a path should be "respected" for the left portion of the particular
	 * path in the search tree. If we find any 'satisfactory' matching during such an exploration, then the 'fragment' graph is ensured to contain
	 * "sufficient"(but may be more) material for a RE-LABELING of the parent graph. If the edges of the 'fragment' graph also exhaust in the
	 * process, then we find an isomorphism between the two graphs.
	 * */
	private boolean isExactMatch(List<Edge> edges, List<Edge> fragmentEdges, Map<Integer, Integer> matchingsMap) throws GraphFormatException {
		/* Exact matches would be done with the help of edges. */
		
		/*System.out.println("%%%%%%% Printing parent graph %%%%%%%%");
		printEdgeList(edges);
		System.out.println("%%%%%%% Printing fragment graph %%%%%%%%");
		printEdgeList(fragmentEdges);
	
		printMatchingsMap(matchingsMap);*/

		if (edges.size() == 0) {				
			if (fragmentEdges.size() == 0)
				return true;
			return false;
		}
		/* Pick the first edge to match. */
		Edge e = edges.get(0);
		/* List of matching edges. */
		List<Edge> eMatches = getEdgeMatches(fragmentEdges, e);
		
		//printEdgeMatches(eMatches, e);	
		
		if (eMatches.size() != 0)
		{
			/* Check if any of the 'ends' of the edge 'e' already has a mapping in the 'matchingsMap'. */
			if (!e.hasMarker())
				throw new GraphFormatException("Encountered an unmarked edge.", e.toString(), 0);
			int headLoc = e.getHeadLoc();
			int depLoc = e.getDepLoc();

			//System.out.println("%%%%%%% Trying to match " + e.getHead().getLabel() + " => " + e.getDep().getLabel() + ", " + headLoc + " => " + depLoc + "%%%%%%%");

			boolean headRes = false, depRes = false;
			if ( (headRes = matchingsMap.containsKey(headLoc)) && (depRes = matchingsMap.containsKey(depLoc)) )
			{	/* We have a 'constraint' for matching this edge. */
				//System.out.println("Matchings already exists.");
				int headMatch = matchingsMap.get(headLoc);
				int depMatch = matchingsMap.get(depLoc);
				for (int j = 0; j < eMatches.size(); j++)
				{
					Edge eMatch = eMatches.get(j);
					if (!eMatch.hasMarker())
						throw new GraphFormatException("Encountered an unmarked edge.", eMatch.toString(), 1);
					if ((eMatch.getHeadLoc() == headMatch) && (eMatch.getDepLoc() == depMatch))
					{
						edges.remove(0);
						fragmentEdges.remove(eMatch);
						boolean result = isExactMatch(edges, fragmentEdges, matchingsMap);
						edges.add(0,e);
						fragmentEdges.add(eMatch);
						return result;
					}
				}
				return false;	
			}	
			else if (headRes || depRes)
			{
				if (headRes)
				{
					//System.out.println("Matching for head already exists.");
					int headMatch = matchingsMap.get(headLoc);
					edges.remove(0);
					for (int j = 0; j < eMatches.size(); j++)
					{
						Edge eMatch = eMatches.get(j);
						if (!eMatch.hasMarker())
							throw new GraphFormatException("Encountered an unmarked edge.", eMatch.toString(), 1);
						if (eMatch.getHeadLoc() == headMatch)
						{
							int depMatch = eMatch.getDepLoc();
							//System.out.println("Entering the match " + depLoc + " => " + depMatch);
							matchingsMap.put(depLoc, depMatch);
							fragmentEdges.remove(eMatch);
							if (isExactMatch(edges, fragmentEdges, matchingsMap))
								return true;
							fragmentEdges.add(eMatch);
							matchingsMap.remove(depLoc);
						}	
					}
					edges.add(0,e);
					return false;	
				}                                                                                                                                                        else
				{
					//System.out.println("Matching for dependent already exists.");
					int depMatch = matchingsMap.get(depLoc);
					edges.remove(0);
					for (int j = 0; j < eMatches.size(); j++)
					{
						Edge eMatch = eMatches.get(j);
						if (!eMatch.hasMarker())
							throw new GraphFormatException("Encountered an unmarked edge.", eMatch.toString(), 1);
						if (eMatch.getDepLoc() == depMatch)
						{
							int headMatch = eMatch.getHeadLoc();
							//System.out.println("Entering the match " + headLoc + " => " + headMatch);
							matchingsMap.put(headLoc, headMatch);
							fragmentEdges.remove(eMatch);
							if (isExactMatch(edges, fragmentEdges, matchingsMap))
								return true;
							fragmentEdges.add(eMatch);
							matchingsMap.remove(headLoc);
						}	
					}
					edges.add(0,e);
					return false;	
				}	
			}	
			else	/* no constraint on matching the edge 'e'. */
			{	
				//System.out.println("No matching exists");
				edges.remove(0);
				for (int j = 0; j < eMatches.size(); j++)
				{
					Edge eMatch = eMatches.get(j);
					if (!eMatch.hasMarker())
							throw new GraphFormatException("Encountered an unmarked edge.", eMatch.toString(), 1);
					matchingsMap.put(e.getHeadLoc(), eMatch.getHeadLoc());
					matchingsMap.put(e.getDepLoc(), eMatch.getDepLoc());
					//System.out.println("Entering the match " + e.getHeadLoc() + " => " + eMatch.getHeadLoc());
					//System.out.println("Entering the match " + e.getDepLoc() + " => " + eMatch.getDepLoc());
					fragmentEdges.remove(eMatch);
					if (isExactMatch(edges, fragmentEdges, matchingsMap))
						return true;
					fragmentEdges.add(eMatch);
					matchingsMap.remove(e.getHeadLoc());
					matchingsMap.remove(e.getDepLoc());
				}
				edges.add(0,e);
				return false;	
			 }
		}
		else
			return false;
	}	

	/* nodeSelectionsMap has the following format:
	 * [nodeIndex] -> {nodeLoc1, nodeLoc2, ..}
	 */
	private List<Edge> getEdges(List<Edge> edges, Map<Integer, List<Integer>> nodeSelectionsMap) throws GraphFormatException
	{
		List<Edge> edgeSelectionList = new ArrayList<Edge>();
		for (Iterator it = edges.iterator(); it.hasNext();)
		{
			Edge e = (Edge) it.next();
			if (!e.hasMarker())
				throw new GraphFormatException("FragmentProcessor.java:getEdges() Encountered an unmarked edge.", e.toString(), 0);
			int headId = this.nodeIndex.getId(e.getHead().getLabel());
			int depId = this.nodeIndex.getId(e.getDep().getLabel());
			if (nodeSelectionsMap.containsKey(headId) && nodeSelectionsMap.containsKey(depId))
			{
				List<Integer> headMatchList =  nodeSelectionsMap.get(headId);
				List<Integer> depMatchList =  nodeSelectionsMap.get(depId);
				if (headMatchList.contains(e.getHeadLoc()) && depMatchList.contains(e.getDepLoc()))
					edgeSelectionList.add(e);
			}	
		}	
		return edgeSelectionList;
	}	
	
	private void printSelectedNodeCombination(BucketGroup buckets)
	{
		Map<Integer, List<Integer>> bucketSelectionsMap = buckets.bucketSelectionsMap;
		for (Iterator it = bucketSelectionsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			Integer nodeId = (Integer) e.getKey();
			String nodeLabel = this.nodeIndex.getLabel(nodeId);
			List selectionsList = (List) e.getValue();
			System.out.print(nodeLabel + " => ");
			for (int i = 0; i < selectionsList.size(); i++)
			{
				System.out.print(selectionsList.get(i) + " ");
			}	
			System.out.println("");
		}

	}	

	private void printSelectFromBucket(List<Integer> list, List<Integer> selectionsList, int count, int startPos) {
		System.out.println("%%%%%%%%% list %%%%%%%%%%%");	
		for (int i = 0; i < list.size(); i++)
		{
			System.out.print(list.get(i) + " ");
		}	
		System.out.println("");
		System.out.println("%%%%%%%%% selections list %%%%%%%%%%%");	
		for (int i = 0; i < selectionsList.size(); i++)
		{
			System.out.print(selectionsList.get(i) + " ");
		}	
		System.out.println("");
		System.out.println("count = " + count);	
		System.out.println("startPos = " + startPos);	
	}	


	/* isConnected(), getEdges() throws exceptions related to the parent graph. */
	/* isExactMatch() throws exceptions related to both the parent graph and the fragment graph. */
	private void selectFromBucket(List<Integer> list, List<Integer> selectionsList, int count, int startPos, BucketGroup buckets, List<Integer> bucketKeysList, int nextBucket) throws GraphFormatException {
		/*printSelectFromBucket(list, selectionsList, count, startPos);
		int inChar;
		try {
			inChar = System.in.read();
			inChar = System.in.read();
		}
		catch (IOException io) {}*/
		if (count == 0) {
			if (nextBucket < bucketKeysList.size())
				newBucket(buckets, bucketKeysList, nextBucket);
			else {	
				/* We have a 'combination' now. */
				System.out.println("selectFromBucket(): Found a combination ...");
				
				//printSelectedNodeCombination(buckets);

				if (isConnected(buckets)) {
					System.out.println("selectFromBucket(): Found a CONNECTED combination ...");
					List<Edge> nodeSelectionEdges = getEdges(buckets.edges, buckets.bucketSelectionsMap);
					Map<Integer, Integer> matchingsMap = new HashMap<Integer, Integer>();
					if (isExactMatch(nodeSelectionEdges, buckets.fragmentEdges, matchingsMap))
						buckets.matchedFragmentList.add(nodeSelectionEdges);
					else
						System.out.println("selectFromBucket(): isExactMatch returned a FALSE ...");

				}
				else {	
					System.out.println("selectFromBucket(): Combination doesn't satisfy the connectivity criterion ...");
				}	
			}	
		}	
		else {	
			for (int i = startPos; i < list.size(); i++) {
				Integer x = list.get(i);
				selectionsList.add(0, x);
				selectFromBucket(list, selectionsList, count-1, i+1, buckets, bucketKeysList, nextBucket);
				selectionsList.remove(0);
			}	
		}
	}

	private void newBucket(BucketGroup buckets, List<Integer> bucketKeysList, int nextBucket) throws GraphFormatException
	{
		//Map.Entry e = (Map.Entry) nextBucket.next();
		//Integer bucketKey = (Integer) e.getKey();
		Integer bucketKey = bucketKeysList.get(nextBucket);
		int count = buckets.countMap.get(bucketKey);	
		/* We're entering a new bucket; make sure that the 'selections' list for this bucket is empty. */
		List<Integer> selectionsList;
		if (buckets.bucketSelectionsMap.containsKey(bucketKey))
			selectionsList = buckets.bucketSelectionsMap.get(bucketKey);
		else
		{
			selectionsList = new LinkedList<Integer>();
			buckets.bucketSelectionsMap.put(bucketKey, selectionsList);
		}

		selectionsList.clear();	// clear the list.
		List<Integer> list = buckets.bucketMap.get(bucketKey);
		
		/** Print select input conditions. **/
		/*System.out.println("newBucket => selectFromBucket() for " + this.nodeIndex.getLabel(bucketKey) + " with count = " + count);
		int inChar;
		try {
			inChar = System.in.read();
			inChar = System.in.read();
		}
		catch (IOException io) {}*/
		/** Print select input conditions. **/

		selectFromBucket(list, selectionsList, count, 0, buckets, bucketKeysList, nextBucket+1);
	}	


	private void printBucketConditions(BucketGroup buckets)
	{
		System.out.println("%%%%%%% Node Choice List %%%%%%%%");
		Map<Integer, List<Integer>> bucketMap = buckets.bucketMap;
		for (Iterator it = bucketMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			Integer nodeId = (Integer) e.getKey();
			List nodeChoiceList = (List) e.getValue();
			System.out.print(this.nodeIndex.getLabel(nodeId) + " => ");
			for (int i = 0; i < nodeChoiceList.size(); i++)
			{
				System.out.print((Integer)nodeChoiceList.get(i) + " ");
			}
			System.out.println("");	
		}	

		System.out.println("%%%%%%% Choice Criteria List %%%%%%%%");
		
		Map<Integer, Integer> countMap = buckets.countMap;
		for (Iterator it = countMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			Integer nodeId = (Integer) e.getKey();
			Integer nodeCount = (Integer) e.getValue();
			System.out.println(this.nodeIndex.getLabel(nodeId) + " => " + nodeCount);
		}
	}	


	/* Checks whether 'fragment' is a subgraph of 'sentence' or not. */
	/* isFragment() -> newBucket() -> selectFromBucket() -> isConnected() -> throws an exception [problem with the original graph] */
	/* isFragment() -> newBucket() -> selectFromBucket() -> getEdges() -> throws an exception [problem with the original graph] */
	/* isFragment() -> newBucket() -> selectFromBucket() -> isExactMatch() -> throws an exception [problem with the original graph AND fragment] */
	private boolean isFragment(Graph fragment, Graph sentence, List<List<Edge>> matchedFragmentList) throws GraphFormatException
	{
		Map<Integer, Integer> countMap = Gadgets.getWordDist(fragment, this.nodeIndex);
		Map<Integer, List<Integer>> commonWordDistMap = Gadgets.getCommonWordDist(sentence, fragment, this.nodeIndex);
		Map<Integer, List<Integer>> nodeSelectionsMap = new HashMap<Integer, List<Integer>>();
		BucketGroup buckets = new BucketGroup(commonWordDistMap, nodeSelectionsMap, countMap, matchedFragmentList, sentence.getEdges(), fragment.getEdges());
		
		/*printBucketConditions(buckets);
		int inChar;
		try {
			inChar = System.in.read();
			inChar = System.in.read();
		}
		catch (IOException io) {
			
		}*/
		LinkedList<Integer> keyList = new LinkedList<Integer>(commonWordDistMap.keySet());
		//Iterator nextBucket = commonWordDistMap.entrySet().iterator();
		int nextBucket = 0;
		if (nextBucket < keyList.size())	
			newBucket(buckets, keyList, nextBucket);			
		if (buckets.matchedFragmentList.size() != 0)
			return true;	
		return false;
	}	

	/* TODO fragmentGraphsMap
	 * sentenceGraphsMap
	 */
	private boolean isAlien(Graph fragmentGraph)
	{
		List<Node> nodes = fragmentGraph.getNodes();
		for (Iterator it = nodes.iterator(); it.hasNext();)
		{
			Node node = (Node) it.next();
			if (this.nodeIndex.getId(node.getLabel()) == null)
			{	
				System.out.println("node " + node.getLabel() + " doesn't exist in the index of nodes.");
				return true;
			}	
		}	
		return false;
	}	
	
	private Map<String, Graph> removeAlienGraphs(Map<String, Graph> dependencyGraphsMap)
	{
		Map<String, Graph> modDependencyGraphsMap = new HashMap<String, Graph>();
		//System.out.println("Begin removing alien graphs");
		for (Iterator it = dependencyGraphsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			Graph fragmentGraph = (Graph) e.getValue();
			if (!isAlien(fragmentGraph))
				modDependencyGraphsMap.put((String)e.getKey(), fragmentGraph);
			else
				System.out.println("Found an 'alien' fragment : " + (String)e.getKey());
		}
		//System.out.println("End removing alien graphs");
		/*System.out.println("Begin Printing NON-alien graphs");
		for (Iterator it = modDependencyGraphsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			String fragment = (String) e.getKey();
			System.out.println(fragment);
		}	
		System.out.println("End Printing NON-alien graphs");*/
		return modDependencyGraphsMap;	
	}	

	private void addAlienGraphs(Map<String, Graph> dependencyGraphsMap, Map<String, List<Match>> exactSentenceMatchesMap)
	{
		for (Iterator it = dependencyGraphsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			String key = (String) e.getKey();
			if (!exactSentenceMatchesMap.containsKey(key)) 
				exactSentenceMatchesMap.put(key,null);
		}	
	}	

	private void printProbSentenceMatches(Map<String, List<Integer>> probSentenceMatchesMap)
	{
		System.out.println("Begin Printing prob matches");
		Index sentenceIndex = this.semModelAux.getSentenceIndex();
		for (Iterator it = probSentenceMatchesMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			String fragment = (String) e.getKey();
			System.out.println(fragment + "====>");
			List probSentenceMatchesList = (List) e.getValue();
			for (int i = 0; i < probSentenceMatchesList.size(); i++)
			{
				Integer sentenceId = (Integer) probSentenceMatchesList.get(i);
				String sentence = sentenceIndex.getLabel(sentenceId);
				System.out.println(i + " :: " + sentence);
			}
			System.out.println("\n");
		}	
		System.out.println("End Printing prob matches");
	}

	private void printFragmentStrings(Map<String, Graph> dependencyGraphsMap)
	{
		System.out.println("Begin printing input fragments");
		for (Iterator it = dependencyGraphsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			String fragment = (String) e.getKey();
			System.out.println(fragment);
		}	
		System.out.println("End printing input fragments");
	
	}	


	public Map<String, List<Match>> getSentenceMatches(String filename, Index nodeIndex) throws GraphFormatException
	{
		Map<Integer, List<Graph>> sentenceGraphsMap = this.semModelAux.getSentenceGraphsMap();
		Index sentenceIndex = this.semModelAux.getSentenceIndex();

		/* TODO Modify getProbSentenceMatches() and printProbMatches() functions for the new return types. */	
		Map<String, Graph> dependencyGraphsMap = getDependencyGraphs(filename);
		//printFragmentStrings(dependencyGraphsMap);
		
		/*System.out.println("%%%%%%% Printing nodeLists, just entered getSentenceMatches() %%%%%%%%%");
		for (Iterator it = dependencyGraphsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			String sentence = (String) e.getKey();
			Graph g = (Graph) e.getValue();
			Gadgets.printNodeList(sentence, g);
			int inChar;
			try {
				inChar = System.in.read();
				inChar = System.in.read();
			}
			catch(IOException ioe)
			{
			
			}	
		}*/
		
		Map<String, Graph> modDependencyGraphsMap = removeAlienGraphs(dependencyGraphsMap);
		Map<String, List<Integer>> probSentenceMatchesMap = getProbSentenceMatches(modDependencyGraphsMap);
		
		/*printProbSentenceMatches(probSentenceMatchesMap);
		
		int inChar;
		try {
			inChar = System.in.read();
		}
		catch (IOException io)
		{
			System.out.println("Error reading input");
		}*/

		/* The exact matchings would be done using a brute-force method using all possible options for all possible nodes. */
		Map<String, List<Match>> exactSentenceMatchesMap = new HashMap<String, List<Match> >();
		for (Iterator it = probSentenceMatchesMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			String fragment = (String) e.getKey();
			System.out.println("Begin finding exact matches for => " + fragment);
			Graph fragmentGraph = dependencyGraphsMap.get(fragment);
			List probSentenceMatches = (List) e.getValue();
			for (Iterator iu = probSentenceMatches.iterator(); iu.hasNext();)
			{
				Integer sentenceId = (Integer) iu.next();
				Graph sentenceGraph = sentenceGraphsMap.get(sentenceId).get(0);
				String sentence = sentenceIndex.getLabel(sentenceId);
				List<List<Edge>> matchedFragmentList = new ArrayList<List<Edge>>(); 
				try {
					//System.out.println("isExactMatch() for " + sentence + " @@@@ " + fragment);
					if (isFragment(fragmentGraph, sentenceGraph, matchedFragmentList))
					{
						Match match = new Match(sentenceId, matchedFragmentList);
						if (exactSentenceMatchesMap.containsKey(fragment))
							exactSentenceMatchesMap.get(fragment).add(match);
						else
						{
							List<Match> exactSentenceMatches = new ArrayList<Match>();
							exactSentenceMatches.add(match);
							exactSentenceMatchesMap.put(fragment, exactSentenceMatches);
						}	
					}	
				}
				catch(GraphFormatException ex) {
					Integer type = ex.getType();
					if (type != null) {	
						if (type == 0) {
							System.out.println(ex.getMessage());
							ex.printLine();
							ex.printStackTrace();
							continue;	
						}
						else if (type == 1) {
							System.out.println(ex.getMessage());
							ex.printLine();
							ex.printStackTrace();
							exactSentenceMatchesMap.put(fragment, null);
							break;	
						}
						else			
							throw new GraphFormatException("FragmentProcessor.java: Exception type is unknown.", ex.getMessage());
					}			
					else
						throw new GraphFormatException("FragmentProcessor.java: Exception type is null", ex.getMessage());
				}
			}	
			System.out.println("End finding exact matches for => " + fragment);
		}
		//addAlienGraphs(dependencyGraphsMap, exactSentenceMatchesMap);		
		return exactSentenceMatchesMap;
	}

private void populateFeatureVector(List<Edge> features, Map<Integer, Integer> featureVectorMap)
{
	/* We would need the featureIndex to populate 'featureVectorMap' */


}	


public List<Edges> getSubtraction(List<Edges> edges, List<Node> fragmentNodes)
{
	for (Iterator it = edges.iterator(); it.hasNext(); )
	{
		Edge edge = (Edge) it.next();

	}	

}	


public Map<Integer, Integer> getFeatureVector(List<Match> matchList)
{
	Map<Integer, List<Graph>> sentenceGraphsMap = this.semModelAux.getSentenceGraphsMap();
	Map<Integer, Integer> featureVectorMap = new HashMap<Integer, Integer>();
	for (Iterator it = matchList.iterator(); it.hasNext();)
	{
		/* For a particular sentence */
		Match match = (Match) it.next();
		Integer sentenceId = match.sentenceId;
		List<List<Edge>> matchedFragmentList = match.matchedFragmentList;
		for  (Iterator iu = matchedFragmentList.iterator(); iu.hasNext();)
		{
			List matchedEdges = (List) iu.next();
			/* TODO Implement getSubtraction() .*/
			List<Edge> features = getSubtraction(sentenceGraphsMap.get(sentenceId).getEdges(), matchedEdges);
			/* Populate the feature index. */
			//populateFeatureVector(features, featureVectorMap);
		}	
	}
	return featureVectorMap;	
}

}
