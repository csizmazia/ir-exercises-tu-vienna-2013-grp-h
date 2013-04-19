package dao;

/** 
 * A vocabulary entry stores a term's ID and the associated documentFrequency.
 *
 */
public class VocabularyEntry
{
	private int termID;
	private int docFrequency;
	private int collectionFrequency;
	private int lastDocID;
	
	

	public VocabularyEntry(int _termID, int _lastDocID)
	{
		this.termID = _termID;
		this.lastDocID = _lastDocID;
		docFrequency = 1;
		collectionFrequency = 1;
	}
	
	public void setDocFreq(int docFreq)
	{
		this.docFrequency = docFreq;
	}

	
	public void setCollectionFreq(int termFreq)
	{
		this.collectionFrequency = termFreq;
	}
	
	public int getCollectionFreq()
	{
		return this.collectionFrequency;
	}

	public int getLastDocID()
	{
		return lastDocID;
	}
	
	public int getTermID()
	{
		return termID;
	}
	
	public int getDocFrequency()
	{
		return docFrequency;
	}

	/**
	 * 
	 * @param docID The ID of the document that causes the docFrequency to be increased by 1. This ID is saved 
	 * s.t. additional calls with the same ID will not increase the docFrequency any further. This assumes that tokens
	 * are processed one document at a time.
	 */
	public void increaseDocFrequencyByOne(int docID)
	{
		if (docID != lastDocID)
		{
			docFrequency += 1;
			lastDocID = docID;
		}	
	}
	
	public void increaseCollectionFrequencyByOne()
	{
		collectionFrequency++;
	}
	
}
