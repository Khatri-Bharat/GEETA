package sem.util;

import java.util.*;
import java.io.IOException;

import sem.graph.Graph;
import sem.graph.Node;
import sem.util.Index;
import sem.exception.GraphFormatException;

public class Gadgets
{
	public static Map<Integer, Integer> getWordDist(Graph g, Index nodeIndex) throws GraphFormatException
	{
		Map<Integer, Integer> wordDistMap = new HashMap<Integer, Integer>();
		List<Node> nodeList = g.getNodes();
		for (Iterator it = nodeList.iterator(); it.hasNext(); )
		{
			Node node = (Node)it.next();
			/* Check if such a node exists in the corpus; in case it doesn't, then the fragment given doesn't have a match in the corpus and extrinisc similarity cannot be calculated. */
			Integer nodeId = nodeIndex.getId(node.getLabel());
			if (nodeId != null)
			{
				if (wordDistMap.containsKey(nodeId))
				{
					int value = wordDistMap.get(nodeId);
					wordDistMap.put(nodeId, value+1);
				}	
				else
					wordDistMap.put(nodeId,1);
			}
			else
			{	
				//System.out.println("Gadgets.java: The node " + node.getLabel() + " doesn't exist in the node index of the corpus.");
				//System.out.println("Gadgets.java: Returning 'null' value for the given text fragment.");
				throw new GraphFormatException("Gadgets.java:getWordDist() Encountered a node that doesn't exist in nodeIndex", node.getLabel());
				//printNodeIndex(nodeIndex);
				//return null;					
			}	
		}
		return wordDistMap;	
	}	


	public static Map<Integer, Integer> getWordDist(String s, Graph g, Index nodeIndex) throws GraphFormatException
	{
		System.out.println("Finding Word Dist for ::");
		System.out.println(s);
		Map<Integer, Integer> wordDistMap = new HashMap<Integer, Integer>();
		List<Node> nodeList = g.getNodes();
		for (Iterator it = nodeList.iterator(); it.hasNext(); )
		{
			Node node = (Node)it.next();
			/* Check if such a node exists in the corpus; in case it doesn't, then the fragment given doesn't have a match in the corpus and extrinisc similarity cannot be calculated. */
			Integer nodeId = nodeIndex.getId(node.getLabel());
			if (nodeId != null)
			{
				if (wordDistMap.containsKey(nodeId))
				{
					int value = wordDistMap.get(nodeId);
					System.out.println("Adding to " + nodeIndex.getLabel(nodeId) + " => " + value);
					wordDistMap.put(nodeId, value+1);
				}	
				else {
					System.out.println("Adding to " + nodeIndex.getLabel(nodeId) + " => 0");
					wordDistMap.put(nodeId,1);
				}	
			}
			else
			{	
				//System.out.println("Gadgets.java: The node " + node.getLabel() + " doesn't exist in the node index of the corpus.");
				//System.out.println("Gadgets.java: Returning 'null' value for the given text fragment.");
				throw new GraphFormatException("Gadgets.java:getWordDist() Encountered a node that doesn't exist in nodeIndex", node.getLabel());
				//printNodeIndex(nodeIndex);
				//return null;					
			}	
		}
		return wordDistMap;	

	}

	public static void printNodeList(String sentence, Graph g)
	{
		System.out.println("Printing the list of nodes for :: " +  sentence);
		List<Node> nodeList = g.getNodes();
		for (int i = 0; i < nodeList.size(); i++)
		{
			if (nodeList.get(i) != null)
				System.out.println(i + " => " + nodeList.get(i).getLabel());		
		}
	}	


	public static void printWordDist(Map<Integer, Integer> wordDist, Index nodeIndex)
	{
		for (Iterator it = wordDist.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			Integer id = (Integer) e.getKey();
			Integer nodeCount = (Integer) e.getValue();
			String nodeLabel =  nodeIndex.getLabel(id);
			System.out.println(nodeLabel + " => " + nodeCount);
		}	
	}

	public static void printNodeIndex(Index nodeIndex)
	{
		int inChar;
		HashMap<String, Integer> idMap = nodeIndex.getIdMap();
		for (Iterator it = idMap.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry entry = (Map.Entry) it.next();
			System.out.println(entry.getKey() + " => " + entry.getValue());
			try
			{
				inChar = System.in.read();
			} catch (IOException e) {
				System.out.println("Error reading input.");
			}	
		}	
	}

	/* getCommonWordDist() returns a map of nodes common to the 'parent' and 'subgraph' graph in the following form:
	 * [nodeIndex] -> {nodeLoc1, nodeLoc2, .... }
	 */
	public static Map<Integer, List<Integer> > getCommonWordDist(Graph parent, Graph subgraph, Index nodeIndex) throws GraphFormatException	
	{
		/* An exception would be thrown by 'getWordDist' if any of the 'subgraph' nodes is not found in the nodeIndex of the parent graph. */
		Map<Integer, Integer> subgraphWordDistMap = getWordDist(subgraph, nodeIndex);
		
		
		ArrayList<Node> nodes = parent.getNodes();
		Map<Integer, List<Integer>> commonWordDistMap = new HashMap<Integer, List<Integer>>();
		for (int i = 0; i < nodes.size(); i++)
		{
			Node node = nodes.get(i);
			int index = nodeIndex.getId(node.getLabel());
			List<Integer> matchIndicesList = null;
			if (subgraphWordDistMap.containsKey(index))
			{
				if (commonWordDistMap.containsKey(index))
					matchIndicesList = commonWordDistMap.get(index);
				else
				{
					matchIndicesList = new ArrayList<Integer>();
					commonWordDistMap.put(index, matchIndicesList);
				}
				matchIndicesList.add(i);
			}	
			
		}	
		return commonWordDistMap;
	}	
}
