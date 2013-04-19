package sem.model;
import java.util.*;
import java.io.IOException;

import sem.util.Index;
import sem.util.Gadgets;
import sem.graph.Graph;
import sem.graph.Node;
import sem.exception.GraphFormatException;
/* 1. sentence index => an index of all the sentences in all the documents.
 * 2. location map => a map with (key, value) pairs of the form ( nodeId, {senId, ....} )
 * 3. word map => a map with (key, value) pairs of the form ( senId, {nodId, ...} ); nodeIds here could be stored in sorted order if needed.
 */

public class SemModelAux {
	
	/* 'wordSentenceMap' helps us find the sentence locations of different words in the corpus. */
	private Map<Integer, Map<Integer, List<Integer>>> wordSentenceMap;
	
	/* 'sentenceWordDistMap' helps us find the word distribution vector of a given sentence. */
	private Map<Integer, Map<Integer, Integer> > sentenceWordDistMap;

	/* 'sentenceGraphsMap' maps a 'sentence index' to the list of its possible dependency graphs. */
	private Map<Integer, List<Graph> > sentenceGraphsMap; 

	/* 'sentenceIndex' is an index of the sentences in the corpus. */
	private Index sentenceIndex; 
	// private String sentenceIndexFileName = "_sentenceindex.vsm";

	public SemModelAux()
	{
		this.wordSentenceMap = new HashMap<Integer, Map<Integer, List<Integer> > >();
		this.sentenceWordDistMap = new HashMap<Integer, Map<Integer, Integer> >();
		this.sentenceGraphsMap = new HashMap<Integer, List<Graph> >();
		this.sentenceIndex = new Index();
	}	

	public int putSentenceStats(String sentence)
	{
		return sentenceIndex.add(sentence);
	}      	
	
	public boolean putSentenceGraphStats(Integer sentenceId, List<Graph> graphs)
	{
		if (!sentenceGraphsMap.containsKey(sentenceId))
		{	
			sentenceGraphsMap.put(sentenceId, graphs);
			return true;
		}
		return false;	
	}	

	/* TODO We need to create an index on the sentences, and decide the point to populate the sentence index. */
	/* We need to make sure that as a sentence is being parsed, we make an entry corresponding to a node just once alongwith its correct occurrence
	 * count in that sentence. */
	private void putWordStats(Integer nodeId, Integer sentenceId, Integer nodeCount, Index nodeIndex)
	{
		/* inserting an entry in the wordSentenceMap. */
		/*System.out.println("In putWordStats()");
		System.out.println(this.sentenceIndex.getLabel(sentenceId) + " <<<>>> " + nodeIndex.getLabel(nodeId) + "," + nodeCount);
		int inChar;
		try {
			inChar = System.in.read();	
			inChar = System.in.read();	
		}	
		catch (IOException io) {
		}*/
		if (wordSentenceMap.containsKey(nodeId))
		{
			
			//System.out.println("nodeId already exists");
			Map<Integer, List<Integer>> wordStatMap = wordSentenceMap.get(nodeId);
			if (wordStatMap.containsKey(nodeCount))
			{
				//System.out.println("nodeCount already exists");
				List<Integer> sentenceList = wordStatMap.get(nodeCount);
				sentenceList.add(sentenceId);
			}
			else
			{
				List<Integer> sentenceList = new ArrayList<Integer>();
				sentenceList.add(sentenceId);
				wordStatMap.put(nodeCount, sentenceList);				
			}		

		}
		else
		{
			/* TODO Change the order of operations to better reflect the process after a test experiment with object references. */
			Map<Integer, List<Integer> > entry = new HashMap<Integer, List<Integer> >();
			List<Integer> sentenceList = new ArrayList<Integer>();
			sentenceList.add(sentenceId);
			entry.put(nodeCount, sentenceList);
			wordSentenceMap.put(nodeId, entry);
		}	
	}	

	/* We need to perhaps over (nodeId, nodeCount) word dist vectors of each sentence and populate the sentenceWordDistMap. */
	private void putSentenceStats(Integer sentenceId, Integer nodeId, Integer nodeCount)
	{
		/* inserting an entry in the sentenceWordDistMap */
		if (sentenceWordDistMap.containsKey(sentenceId))
		{
			Map<Integer, Integer> wordStatMap = sentenceWordDistMap.get(sentenceId);
			wordStatMap.put(nodeId, nodeCount);
		}
		else
		{
			Map<Integer, Integer> wordStatMap = new HashMap<Integer, Integer>();
			wordStatMap.put(nodeId, nodeCount);
			sentenceWordDistMap.put(sentenceId, wordStatMap);
		}
	}	




	/* This function is called at the very end when we're done with parsing all the documents and 'semModel' is done with all its statistics. */
	public boolean buildModelStats(Index nodeIndex)
	{
		for(Iterator it = sentenceGraphsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry entry = (Map.Entry) it.next();
			Integer sentenceId =  (Integer) entry.getKey();
			List graphs = (List) entry.getValue();
			for (Iterator iu = graphs.iterator(); iu.hasNext();)
			{
				Graph g = (Graph) iu.next();
				Map<Integer, Integer> sentenceWordDist = null;
				try {
					sentenceWordDist = Gadgets.getWordDist(g, nodeIndex);
				}
				catch(GraphFormatException e) {
				
				}
				if (sentenceWordDist != null) {			
					for (Iterator iv = sentenceWordDist.entrySet().iterator(); iv.hasNext(); )
					{
						Map.Entry e = (Map.Entry) iv.next();
						Integer nId = (Integer) e.getKey();
						Integer nCount = (Integer) e.getValue();
						putWordStats(nId, sentenceId, nCount, nodeIndex);
						/* TODO The sentenceWordDistMap won't be able to handle multiple parses for a single sentence. */
						putSentenceStats(sentenceId, nId, nCount);
					}	
				}
			}	
		}	
		return true;	
	}	

	public List<Integer> getSentenceMatches(Integer nodeId, Integer nodeCount)
	{
		Map<Integer, List<Integer>> sentenceLocs = this.wordSentenceMap.get(nodeId);
		return sentenceLocs.get(nodeCount);
	}

	public void printWordSentenceMap(Index nodeIndex) {
		for (Iterator it = this.wordSentenceMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry e = (Map.Entry) it.next();
			Integer nodeId = (Integer) e.getKey();

			System.out.print(nodeIndex.getLabel(nodeId) + ", ");
			
			Map map = (Map) e.getValue();
			for (Iterator iu = map.entrySet().iterator(); iu.hasNext();) {
				Map.Entry entry = (Map.Entry) iu.next();
				Integer nodeCount = (Integer) entry.getKey();
				
				System.out.println(nodeCount);
				
				List sentenceList = (List) entry.getValue();
				for (int i = 0; i < sentenceList.size(); i++) {
					Integer sentenceId = (Integer) sentenceList.get(i);
					System.out.println(this.sentenceIndex.getLabel(sentenceId));
				}	
			}	
		}	
	}	

	public List<Integer> getIntersection(List<Integer> sentenceIds, Map<Integer, Integer> wordDistCrit, Index nodeIndex)
	{
		List<Integer> resultSentenceIds = new ArrayList<Integer>();
		Map<Integer, Integer> wordDist;
		boolean failed;
		for (Iterator it1 = sentenceIds.iterator(); it1.hasNext(); )
		{
			Integer sentenceId = (Integer) it1.next();
			Map<Integer, Integer> sentenceWordDist = sentenceWordDistMap.get(sentenceId);
			failed = false;
			System.out.println("%%%%% Testing node mix for " + this.sentenceIndex.getLabel(sentenceId) + " %%%%%%");
			/* We need to check if this sentence (sentenceWordDist) satisfies the criteria (wordDistCrit) or not. */
			for (Iterator it2 = wordDistCrit.entrySet().iterator(); it2.hasNext(); )
			{
				Map.Entry pair = (Map.Entry) it2.next();
				Integer key = (Integer) pair.getKey();
				Integer value = (Integer) pair.getValue();
				System.out.println("Checking for " + nodeIndex.getLabel(key) + " => " + value);
				if (sentenceWordDist.containsKey(key))
				{
					System.out.println("Found " + nodeIndex.getLabel(key));
					if (sentenceWordDist.get(key) < value)
					{
						/* One of the criterions failed for this sentenceId. Hence, it is discarded. */
						failed = true;
						break;
					}	
					System.out.println(nodeIndex.getLabel(key) + "meets the criterion.");
				}
				else
				{
					System.out.println("Not Found " + nodeIndex.getLabel(key));
					failed = true;
					break;
				}	
			}		
			if (!failed)
			{	
				System.out.println("Adding :::: " + this.sentenceIndex.getLabel(sentenceId) + " ::::");
				resultSentenceIds.add(sentenceId);
			}
		}	
		return resultSentenceIds;
	}

public Index getSentenceIndex()
{
	return this.sentenceIndex;
}

public Map<Integer, List<Graph>> getSentenceGraphsMap()
{
	return this.sentenceGraphsMap;
}

}
