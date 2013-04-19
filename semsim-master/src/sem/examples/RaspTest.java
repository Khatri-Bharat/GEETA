package sem.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.io.File;

import sem.exception.GraphFormatException;
import sem.exception.SemModelException;
import sem.graph.Graph;
import sem.grapheditor.GraphEditor;
import sem.grapheditor.LowerCaseGraphEditor;
import sem.grapheditor.NumTagsGraphEditor;
import sem.graphreader.GraphReader;
import sem.graphreader.RaspGraphReader;
import sem.model.SemModel;
import sem.model.SemModelAux;
import sem.model.VectorSpace;
import sem.util.Index;
import sem.util.FragmentProcessor;
import sem.util.Match;
import sem.model.Fragment;
/* class RaspTest is used to set up a framework for our fragment entailment engine to work on. */
public class RaspTest {

/* @arg[0] contains the filename of the file containing the parsed sentences from the corpus.
 * @arg[1] contains the filename of the file containing the parsed text fragments between which a similarity score is required.
 */

public static void main(String[] args) {
		try {
			// First, let's build the model
			// Using the SemGraph library to read in the dependency graphs
			/* args[0] contains the parsed sentences of a single document; we can extend the example to take in more than one document in
			 * order to build a model of the entire corpus. */
			// Creating a new empty model
			if (args.length < 2)
			{	
				System.out.println("Usage: sem.examples.RaspTest <parsed corpus file> <parsed input fragments>");
				System.exit(0);
			}
			File f = new File(args[0]);
			if (!f.exists())	
			{
				System.out.println("The file \"" + args[0] + "\" doesn't exist.");
				System.exit(0);
			}	
			
			SemModel semModel = new SemModel(false);
		
			// @Khatri Creating a new empty auxiliary model.
			SemModelAux semModelAux = new SemModelAux();
		
			GraphReader reader = new RaspGraphReader(args[0], false, semModelAux);
			// @Khatri

			// initializing some graph editors. They can be used to clean up the graphs, but are not required.
			ArrayList<GraphEditor> graphEditors = new ArrayList<GraphEditor>(Arrays.asList(new LowerCaseGraphEditor()/*, new NumTagsGraphEditor()*/));
			System.out.println("Begin reading the corpus ...");
			// Adding all the graphs to the model
			while(reader.hasNext()){
				Graph graph = reader.next();
				for(GraphEditor graphEditor : graphEditors)
					graphEditor.edit(graph);
				semModel.add(graph);
			}
			reader.close();
			System.out.println("Done reading the corpus ...");
			
			// @Khatri
			System.out.println("Begin corpus-search helper maps ...");
			semModelAux.buildModelStats(semModel.getNodeIndex());
			System.out.println("Done creating corpus-search helper maps ...");
			// @Khatri
			
			/* Now, we move on to find the extrinsic similarity between the two text fragments. */
			FragmentProcessor fragmentProcessor = new FragmentProcessor(semModelAux, semModel.getNodeIndex());
			
			// We construct a new vector space, using the PMI weighting scheme. The PMI_LIM scheme discards features that occur only once. 
                        VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true); 
			
			/* Build the feature count stats for the feature weighting schemes. */
			Fragment fragment = new Fragment(vectorSpace.getFeatureIndex(), semModel.getNodeIndex(), semModel.getEdgeIndex());
			Map<Integer, List<Graph>> sentenceGraphsMap = semModelAux.getSentenceGraphsMap();
			
			/* Populates the sample space (A,f) required for feature weighting. */	
			System.out.println("Begin building the (A,f) statistics ...");
			fragment.buildStats(sentenceGraphsMap);
			System.out.println("Done building the (A,f) statistics ...");

			/*System.out.println("Begin printing Word Sentence Map");
			semModelAux.printWordSentenceMap(semModel.getNodeIndex());	
			System.out.println("End printing Word Sentence Map");*/

			System.out.println("Begin search for input fragments ...");
			Map<String, List<Match>> sentenceMatchesMap = fragmentProcessor.getSentenceMatches(args[1], semModel.getNodeIndex());
			System.out.println("Done search for input fragments ...");


			Index sentenceIndex = semModelAux.getSentenceIndex();
			/* Now that we have the list of probable sentence Matches, we need to find the exact sentence matches using the 'getSentenceMatches'
			 * function of the fragmentProcessor class. */		
			System.out.println("Begin printing fragment search results ...");
			for (Iterator it = sentenceMatchesMap.entrySet().iterator(); it.hasNext();)
			{
				Map.Entry e = (Map.Entry) it.next();
				String sentence = (String) e.getKey();
				System.out.println("\n\n" + sentence + ":=>");
				List matchList = (List) e.getValue();
				if (matchList == null)
				{
					System.out.println(sentence + "is an 'alien' fragment for the corpus.");
					continue; 
				} 
				for (Iterator iu = matchList.iterator(); iu.hasNext();)
				{
					Match match = (Match) iu.next();
					Integer sentenceId = match.sentenceId;
					String sentenceMatch = sentenceIndex.getLabel(sentenceId);
					System.out.println(sentenceMatch);		
				}
			}	
			System.out.println("Done printing fragment search results ...");
			
		} catch (GraphFormatException e) {
			System.out.println(e.getMessage());
			e.printLine();
			e.printStackTrace();
		} catch (SemModelException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
