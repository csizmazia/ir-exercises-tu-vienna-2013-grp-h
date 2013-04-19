package dao;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PostingsList
 *
 */
public class PostingsList implements IPostingsList
{
	private Map<Integer,Integer> postings;
	
	public PostingsList() {
		super();

		/**
		 * The postingsList uses a LinkedHashMap to store docIDs as Keys and termFrequency as Values. The initial size is 1 and the loading factor
		 * is 1.0f, because LinkedHashMap will internally double the size of allocated memory each time size >= capacity*loadFactor, which is
		 * exactly what we want for our Indexer.
		 */
		this.postings = new LinkedHashMap<Integer,Integer>(1, 1.0f);
	}

	/**
	 * Adds a document to the PostingsList. Adds 1 to the term frequency for the document in case the document already was in the PostingsList.
	 */
	@Override
	public void add(int docID)
	{
		Integer current_tf = postings.get(docID);
		if (current_tf != null)
		{
			this.postings.put(docID,(int) current_tf + 1);
		}
		else 
		{
			this.postings.put(docID,1);
		}
	}
	
	/**
	 * Adds the specified docID to the PostingsList and adds the specified amount to the termFrequency. For new entries, this 
	 * generates a posting with an initial termFrequency of <code>termFreq</code>. 
	 * 
	 * This method is used when merging the PostingsLists of several dictionaries. 
	 * 
	 * @param docID The ID of the document to add.
	 * @param termFreq The term frequency to be added to the current value.
	 */
	public void add(int docID, int termFreq)
	{
		Integer current_tf = postings.get(docID);
		if (current_tf != null)
		{
			this.postings.put(docID,(int) current_tf + termFreq);
		}
		else 
		{
			this.postings.put(docID, termFreq);
		}
	}

	/**
	 * Not used in this implementation of PostingsList, since memory allocation is handled internally by LinkedHashMap.
	 * Returns without performing any changes on the PostingsList.
	 */
	@Override
	public void doubleSize()
	{
	}

	/**
	 * Not used in this implementation of PostingsList, since memory allocation is handled internally by LinkedHashMap. Returns a default value of 
	 * false which does not reflect the actual state of PostingsList in any way.
	 */
	@Override
	public boolean isFull()
	{
		return false;
	}

	/**
	 * PostingsLists have an initial capacity of 1 and increase their capacity by a factor of 2 every time the internal table is filled
	 * to capacity. To calculate the actual amount of allocated memory (disregarding class overhead and such) we 
	 */
	@Override
	public int getAllocatedSizeInBytes()
	{
		int n = postings.size(); 
		
	//	System.out.print("Size = "+n+", ");
			
		n = Integer.highestOneBit(n);
		n = n << 1;  
		
		n = Math.max(n, 1); // 1 is the initial size of the list
	//	System.out.println("Allocated Size = "+n);
		
		return n * 16; // a postingsList has 4 fields, each needing 4 byte of data (in 32bit systems): Key, Value, RefToNext, RefToPrev
	}
	
	/**
	 * Getter for member variable <code>postings</code>
	 */
	public Map<Integer,Integer> getPostings() {
		return this.postings;
	}
	
	public Integer getTermFrequency(Integer docID) {
		Integer termFrequency = this.postings.get(docID);
		return termFrequency == null ? 0 : termFrequency;
	}
	
	public Integer getTermFrequency() {
		Integer termFrequency = 0;
		for(Integer tf_per_doc : this.postings.values()) {
			termFrequency += tf_per_doc;
		}
		return termFrequency;
	}
	
	public Integer getDocumentFrequency() {
		return this.postings.size();
	}
	
	public void clear()
	{
		postings.clear();
	}
	
	@Override
	public String toString() {
		String ret = "";
	    
		for(Integer docID : postings.keySet()) 
		{
			ret += "<";
			ret += docID;
			ret += ":";
			ret += postings.get(docID);
			ret += ">"; 
		}
		
		/*
		boolean first = true;
		for(Integer docID : this.postings.keySet()) {
			if(!first) {
				ret += ",";
			}
			first = false;
			ret += docID+":"+this.postings.get(docID);
		}
		ret += "";
		*/
		return ret;
	}
}
