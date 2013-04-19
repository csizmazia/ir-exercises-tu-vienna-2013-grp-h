package dao;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores terms (Strings) and their IDs (Integers). Should be implicitly ordered by term ID. After all terms have been 
 * added, {@link #finalize()} should be called. A Vocabulary also stores some additional info, like the total number of 
 * documents in the index.
 *
 */
public class Vocabulary
{
	private LinkedHashMap<String, VocabularyEntry> vocabulary; // the termIDs are the values, the terms are the keys
	
	private ArrayList<Map.Entry<String, VocabularyEntry>> fast_access_vocabulary; // to allow for the fast reverse loopkup get(ID)
	
	private int totalNumberOfDocuments = 0;
	
	private int termID = 0;
	

	public Vocabulary(int _size)
	{
		vocabulary = new LinkedHashMap<String, VocabularyEntry>(_size, 1.0f);
		fast_access_vocabulary = null;
	}
	
	/**
	 * Sets the total amount of documents in the index to the specified value.
	 * @param _count
	 */
	public void setTotalNumberOfDocuments(int _count)
	{
		totalNumberOfDocuments = _count;
	}
	
	/**
	 * 
	 * @return The total amount of documents in the index.
	 */
	public int getTotalNumberOfDocuments()
	{
		return totalNumberOfDocuments;
	}
	
	/**
	 * Allocates memory to create a fastAccessVocabulary, which enables the efficient use of get(termID). This
	 * tacitly assumes that no additional calls to {@link #add(String, int)} are made!
	 */
	public void finalize()
	{
		fast_access_vocabulary = new ArrayList<Map.Entry<String, VocabularyEntry>>(vocabulary.entrySet());
	}
	
	/**
	 * Adds the specified term to the vocabulary. 
	 * 
	 * If it was already in the vocabulary and the 
	 * current docId has been counted towards the documentFrequency of the specified term, this method does not modify the 
	 * vocabulary and merely returns the VocabularyEntry of the term. 
	 * 
	 * If the term existed, but has occurred for the first time in the current Document, the docFrequency of this term 
	 * is increased by 1 and the adjusted VocabularyEntry is returned.
	 * 
	 * For new terms, a new VocabularyEntry is created (with an initial docFrequency of 1 and a new termID) and returned.
	 * 
	 * @param term The term to be added to the vocabulary
	 * @param currentDocID The current DocID, used to calculate the document frequency
	 * 
	 * @return The VocabularyEntry which is associated with the given term.
	 */
	public VocabularyEntry add(String term, int currentDocID)
	{
		VocabularyEntry old = get(term);
		if (old != null)
		{
			old.increaseCollectionFrequencyByOne();
			if (old.getLastDocID() != currentDocID)
			{
				old.increaseDocFrequencyByOne(currentDocID);
				vocabulary.put(term, old);
				return old;
			}
			else
			{
				return old;
			}	
		}
			
		VocabularyEntry newEntry = new VocabularyEntry(termID, currentDocID);
		vocabulary.put(term, newEntry);
		termID++;
		
		return newEntry;
	}
	
	public VocabularyEntry addSilent(String term)
	{
		VocabularyEntry old = get(term);
		if (old != null)
		{
			return old;
		}
			
		VocabularyEntry newEntry = new VocabularyEntry(termID, -1);
		newEntry.setDocFreq(0);
		newEntry.setCollectionFreq(0);
		
		vocabulary.put(term, newEntry);
		termID++;
		
		return newEntry;
	}
	

	
	
	/**
	 * Returns the number of terms in this vocabulary.
	 * @return
	 */
	public int size()
	{
		return vocabulary.size();
	}
	
	/**
	 * Convenience method that allows to access the vocabulary directly.
	 * @return
	 */
	public LinkedHashMap<String, VocabularyEntry> getVocabulary()
	{
		return vocabulary;
	}
	


	/**
	 * Returns the VocabularyEntry for the given term or null if the term is not in the vocabulary. 
	 * 
	 * @param term
	 * @return
	 */
	public VocabularyEntry get(String term)
	{
		return vocabulary.get(term);
	}
	
	/**
	 * Fast access method to get the VocabularyEntry associated with the provided termID or null if the termID does 
	 * not exist or the Vocabulary has not been finalized yet.
	 * @param termID The termID of the term the caller is interested in.
	 * @return The VocabularyEntry associated with the provided termID or null if either the term does not exist or this
	 * method has been called before Vocabulary.finalize() has been called.
	 */
	public VocabularyEntry get(int termID)
	{
		if (fast_access_vocabulary == null)
		{
			System.err.println("Error, tried to access Vocabulary.get(termID) before the Vocabulary was finalized!");
			return null;
		}
		return fast_access_vocabulary.get(termID).getValue();
	}
	
	public ArrayList<Map.Entry<String, VocabularyEntry>> get_fast_access_vocabulary()
	{
		return fast_access_vocabulary;
	}
	
	public void debugPrintContent()
	{
		for (int i = 0; i < fast_access_vocabulary.size(); i++)
		{
			System.out.println(fast_access_vocabulary.get(i).getKey()+": Df = "+fast_access_vocabulary.get(i).getValue().getDocFrequency());
		}
	}
	
}
