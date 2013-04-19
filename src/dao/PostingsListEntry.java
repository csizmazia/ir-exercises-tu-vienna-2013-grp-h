package dao;

/** 
 * Small convenience DAO to store an intermediate block dictionary's individual posting entries, 
 * which consist of the term, the docID and the term frequency. 
 *
 */
public class PostingsListEntry implements Comparable<PostingsListEntry>
{
	public String currentTerm;
	public int currentDocID;
	public int currentTermFrequency;
	
	@Override
	public int compareTo(PostingsListEntry o)
	{
		int retval = currentTerm.compareTo(o.currentTerm);
		if (retval == 0)
		{
			retval = Integer.compare(this.currentDocID, o.currentDocID);
			return retval;
		}
			
		return retval;
	}
	

	
	
}
