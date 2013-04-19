package dao;


import java.util.Iterator;
import java.util.LinkedHashMap;


/**
 * The inverse of an inverted index, this class stores a matrix with one row for each documentID and one column 
 * for each termID. One may think of a DocumentTermMatrix as documentTerm - Vectors in the Vectorspace created by 
 * the terms. 
 * 
 * To save memory, termIDs are used instead of Strings. Thus, an (outside) Vocabulary needs to maintain the correct
 * termID-term mapping.
 *
 */
public class DocumentTermMatrix
{
	
	private LinkedHashMap<Integer, DocumentTermList> docTermLists;
	
	
	public DocumentTermMatrix()
	{
		docTermLists = new LinkedHashMap<Integer, DocumentTermList>(); // list will be ordered according to docID
	}
	
	/**
	 * Clear the contents of the matrix.
	 */
	public void clear()
	{
		docTermLists.clear();
	}

	/**
	 * Creates a new row in the matrix for the given document and returns a DocumentTermList associated with this entry.
	 * If the document already had a row associated with it, this method does not modify the matrix and simply returns 
	 * the previously associated DocumentTermList.
	 * 
	 * @param newDoc
	 * @return
	 */
	public DocumentTermList addDoc(int newDoc)
	{
		DocumentTermList documentTermList;
		if(docTermLists.containsKey(newDoc)) 
		{
			documentTermList = docTermLists.get(newDoc);
		}
		else {
			documentTermList = new DocumentTermList();
			docTermLists.put(newDoc, documentTermList);
			documentTermList = docTermLists.get(newDoc);
		}
		return documentTermList;
	}


	
	/**
	 * Sorts all rows in the matrix by termID.
	 */
	public void sortTermListingsByID()
	{
		Iterator<DocumentTermList> it = docTermLists.values().iterator();
		DocumentTermList current;
		
		while (it.hasNext())
		{
			current = it.next();
			current.sortTermsByID();
		}
		
	}
	
	/**
	 * Returns the matrix. Usually, you want to call sortTermListingsByID() prior to calling this method. 
	 */
	public LinkedHashMap<Integer, DocumentTermList> getMatrix()
	{
		return docTermLists;
	}
	

	/**
	 * Returns the number of rows in the matrix.
	 * @return
	 */
	public int getNumberOfDocs() 
	{
		return docTermLists.size();
	}
	
	
	/**
	 * Calculates the IDF value for all entries in the matrix. This assumes that the current entry value 
	 * hold the termFrequency instead (values will be overwritten). The provided vocabulary is needed to
	 * get the documentFrequency of terms.
	 * 
	 * @param vocabulary
	 */
	public void calculateIdfs(Vocabulary vocabulary) 
	{
		Iterator<DocumentTermList> it = docTermLists.values().iterator();
		DocumentTermList current;
		
		while (it.hasNext())
		{
			current = it.next();
			current.calculateIDFs(vocabulary);
		}
		
	}
	
	/**
	 * Can only be called *before* calling {@link #calculateIdfs(Vocabulary)}. This will delete entries in the matrix
	 * if the termFrequency of the entry is outside the thresholds provided.
	 *  
	 * @param lowTfThreshold
	 * @param highTfThreshold
	 * @return The total number of terms removed from this matrix.
	 */
	public int applyTfThresholds(int lowTfThreshold, int highTfThreshold) 
	{
		int totalCnt = 0;
		for(Integer doc : docTermLists.keySet()) 
		{
			totalCnt += docTermLists.get(doc).applyTfThresholds(lowTfThreshold, highTfThreshold);
		}
		
		return totalCnt;
	}
}
