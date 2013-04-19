package sem.util;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

/**
 * Holds a three-dimensional tensor.
 * The three keys that specify a position in the tensor are integers, and the value in a specific position is a double.
 */
public class Tensor{
	private TIntObjectHashMap<TIntObjectHashMap<TIntDoubleHashMap>> map;
	
	/**
	 * Creates a new tensor.
	 */
	public Tensor(){
		map = new TIntObjectHashMap<TIntObjectHashMap<TIntDoubleHashMap>>();
	}
	
	/**
	 * Creates a new tensor and initialises it from an input file.
	 * @param file Inputfile to read the tensor from. Should be generated by the save() method
	 */
	public Tensor(String file){
		this();
		this.load(file);
	}
	
	/**
	 * Get the underlying three-dimensional map.
	 * @return	Get the map.
	 */
	public TIntObjectHashMap<TIntObjectHashMap<TIntDoubleHashMap>> getMap(){
		return this.map;
	}
	
	/**
	 * Set the value in the specified location equal to the new value.
	 * If there was no previous entry at this position, a new one is created.
	 * If an entry existed, it is overwritten.
	 * @param key1 Key 1
	 * @param key2 Key 2
	 * @param key3 Key 3
	 * @param value The new value
	 */
	public synchronized void set(Integer key1, Integer key2, Integer key3, Double value){
		if(key1 == null || key2 == null || key3 == null || value == null)
			throw new RuntimeException("Keys and values are not allowed to be null.");
		
		TIntObjectHashMap<TIntDoubleHashMap> tempMap1 = map.get(key1);
		if(tempMap1 == null){
			tempMap1 = new TIntObjectHashMap<TIntDoubleHashMap>();
			map.put(key1, tempMap1);
		}
		
		TIntDoubleHashMap tempMap2 = tempMap1.get(key2);
		if(tempMap2 == null){
			tempMap2 = new TIntDoubleHashMap();
			tempMap1.put(key2, tempMap2);
		}
		tempMap2.put(key3, value);
	}
	
	/**
	 * Add a value to the tensor. If the key combination already exists, the new value gets added to the old value.
	 * @param key1 Key 1
	 * @param key2 Key 2
	 * @param key3 Key 3
	 * @param value The added value
	 */
	public synchronized void add(Integer key1, Integer key2, Integer key3, Double value){
		if(key1 == null || key2 == null || key3 == null || value == null)
			throw new RuntimeException("Keys and values are not allowed to be null.");
		
		TIntObjectHashMap<TIntDoubleHashMap> tempMap1 = map.get(key1);
		if(tempMap1 == null){
			tempMap1 = new TIntObjectHashMap<TIntDoubleHashMap>();
			map.put(key1, tempMap1);
		}
		
		TIntDoubleHashMap tempMap2 = tempMap1.get(key2);
		if(tempMap2 == null){
			tempMap2 = new TIntDoubleHashMap();
			tempMap1.put(key2, tempMap2);
		}
		
		Double oldVal = tempMap2.get(key3);
		if(oldVal == null)
			tempMap2.put(key3, value);
		else
			tempMap2.put(key3, oldVal + value);
	}
	
	/**
	 * Clear the tensor
	 */
	public synchronized void clear(){
		this.map.clear();
	}
	
	/**
	 * Get the size of the first dimension in the tensor.
	 * @return the size of the first dimension
	 */
	public int size(){
		return this.map.size();
	}
	
	/**
	 * Save the tensor into a text file.
	 * @param file File path
	 */
	public synchronized void save(String file)
	{
		TIntObjectHashMap<TIntDoubleHashMap> tempMap1;
		TIntDoubleHashMap tempMap2;
		
		try{
			FileWriter fstream = new FileWriter(file);
		    BufferedWriter out = new BufferedWriter(fstream);
		    for(int key1 : map.keys()){
		    	tempMap1 = map.get(key1);
		    	out.write("" + key1);
		    	for(int key2 : tempMap1.keys()){
		    		tempMap2 = tempMap1.get(key2);
		    		for(int key3 : tempMap2.keys()){
		    			out.write("\t" + key2 + ":" + key3 + ":" + tempMap2.get(key3));
		    		}
		    	}
		    	out.write("\n");
		    }
		    out.close();
		}catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Load the tensor from a text file. The file should be generated by the save() method.
	 * @param file
	 */
	public synchronized void load(String file)
	{
		String[] chunks, chunks2;
		try{
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			Integer key1;
			
			while ((strLine = br.readLine()) != null)   {
				chunks = strLine.split("\\s+");
				if(chunks.length >= 1){
					key1 = Integer.parseInt(chunks[0]);
					for(int i = 1; i < chunks.length; i++){
						chunks2 = chunks[i].split(":");
						this.add(key1, Integer.parseInt(chunks2[0]), Integer.parseInt(chunks2[1]), Double.parseDouble(chunks2[2]));
					}
				}
				else{
					System.out.println("Error in Tensor.load(): Unknown num of columns: " + chunks.length);
					System.exit(1);
				}
			}
			in.close();
		}catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	/**
	 * Counts the values in the tensor
	 * If any of the three keys is null, this function will interpret that as a wildcard and sum over the appropriate columns
	 * @param key1 Key 1
	 * @param key2 Key 2
	 * @param key3 Key 3
	 * @return The correct value from the tensor, 0.0 if it does not exist, or a sum over the columns if some keys are null.
	 */
	public double get(Integer key1, Integer key2, Integer key3){

		TIntObjectHashMap<TIntDoubleHashMap> tempMap1;
		TIntDoubleHashMap tempMap2;
		double tempValue;
		
		if(key1 != null){
			if(key2 != null){
				if(key3 != null){
					if(((tempMap1 = this.map.get(key1)) != null) 
							&& ((tempMap2 = tempMap1.get(key2)) != null) 
							&& ((tempValue = tempMap2.get(key3)) != this.map.getNoEntryValue() ))
						return  tempValue;
					else
						return 0.0;
				}
				else {
					double sum = 0.0;
					if(((tempMap1 = this.map.get(key1)) != null) 
							&& ((tempMap2 = tempMap1.get(key2)) != null)) {
						for(double d : tempMap2.values()){
							sum += d;
						}
					}
					return sum;
				}
			}
			else if(key3 != null){
				double sum = 0.0;
				if(((tempMap1 = this.map.get(key1)) != null)){
					for(int tempKey2 : tempMap1.keys()){
						if((tempValue = tempMap1.get(tempKey2).get(key3)) != this.map.getNoEntryValue())
							sum += tempValue;
					}
				}
				return sum;
			}
			else {
				double sum = 0.0;
				if(((tempMap1 = this.map.get(key1)) != null)){
					/*for(TIntDoubleHashMap tMap2 : tempMap1.values()){
						for(double d : tMap2.values())
							sum += d;
					}*/
					for(int tempKey2 : tempMap1.keys()){
						for(double d : tempMap1.get(tempKey2).values())
							sum += d;
					}
				}
				return sum;
			}
		}
		else if(key2 != null){
			if(key3 != null){
				double sum = 0.0;
				
				/*for(TIntObjectHashMap<TIntDoubleHashMap> tMap1 : this.map.values()){
					if(((tempMap2 = tMap1.get(key2)) != null)
							&& ((tempValue = tempMap2.get(key3)) != this.map.getNoEntryValue())){
						sum += tempValue;
					}
				}
				*/
				
				for(int tempKey1 : this.map.keys()){
					if(((tempMap2 = this.map.get(tempKey1).get(key2)) != null)
							&& ((tempValue = tempMap2.get(key3)) != this.map.getNoEntryValue())){
						sum += tempValue;
					}
				}
				return sum;
			}
			else{
				double sum = 0.0;
				/*
				for(TIntObjectHashMap<TIntDoubleHashMap> tMap1 : this.map.values()){
					if(((tempMap2 = tMap1.get(key2)) != null)){
						for(double d : tempMap2.values())
							sum += d;
					}
				}
				*/
				for(int tempKey1 : this.map.keys()){
					if(((tempMap2 = this.map.get(tempKey1).get(key2)) != null)){
						for(double d : tempMap2.values())
							sum += d;
					}
				}
				return sum;
			}
		}
		else if(key3 != null){
			double sum = 0.0;
			/*
			for(TIntObjectHashMap<TIntDoubleHashMap> tMap1 : this.map.values()){
				for(TIntDoubleHashMap tMap2 : tMap1.values()){
					if((tempValue = tMap2.get(key3)) != this.map.getNoEntryValue())
						sum += tempValue;
				}
			}
			*/
			for(int tempKey1 : this.map.keys()){
				tempMap1 = this.map.get(tempKey1);
				for(int tempKey2 : tempMap1.keys()){
					tempMap2 = tempMap1.get(tempKey2);
					if((tempValue = tempMap2.get(key3)) != this.map.getNoEntryValue())
						sum += tempValue;
				}
			}
			return sum;
		}
		else {
			double sum = 0.0;
			/*
			for(TIntObjectHashMap<TIntDoubleHashMap> tMap1 : this.map.values()){
				for(TIntDoubleHashMap tMap2 : tMap1.values()){
					for(double d : tMap2.values())
						sum += d;
				}
			}
			*/
			for(int tempKey1 : this.map.keys()){
				tempMap1 = this.map.get(tempKey1);
				for(int tempKey2 : tempMap1.keys()){
					for(double d : tempMap1.get(tempKey2).values())
						sum += d;
				}
			}
			return sum;
		}
	}
	
	public int[] getKeys(){
		return this.map.keys();
	}
	
	public int[] getKeys(Integer key1){
		if(this.map.containsKey(key1))
			return this.map.get(key1).keys();
		return null;
	}
	
	public int[] getKeys(Integer key1, Integer key2){
		if(this.map.contains(key1) && this.map.get(key1).contains(key2))
			return this.map.get(key1).get(key2).keys();
		return null;
	}
	
	public boolean containsKey(int key1){
		return this.map.containsKey(key1);
	}

}
