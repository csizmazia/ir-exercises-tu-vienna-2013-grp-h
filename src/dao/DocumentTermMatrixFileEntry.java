package dao;

/**
 * The inverse of a regular posting, this DAO stores an entry in the documentTermMatrix, which specifically entails:
 * 
 * - the docID of the entry
 * - the termID of the entry
 * - the idf == value of the entry
 * 
 * The Comparable interface is needed to allow the merging of documentTermMatrices, where the correct ordering of termIDs
 * must be ensured.
 *
 */
public class DocumentTermMatrixFileEntry implements Comparable<DocumentTermMatrixFileEntry>
{
	public int docID;
	public int termID;
	public float value;
	
	@Override
	public int compareTo(DocumentTermMatrixFileEntry o)
	{
		int retval = Integer.compare(this.docID, o.docID);
		
		if (retval == 0)
		{
			return Integer.compare(this.termID, o.termID);
		}
		
		return retval;
	}
}
