package dao;

/**
 * Defines the method a postings list needs to implement for a dictionary. Note that due to the way SPIMI 
 * dictionaries are created, we do not need to insert new docIDs at their correct place, but need only to insert
 * them at the end of the list. Refer to "An Introduction to Information Retrieval" (Manning et al. 2009) pp. 73f.
 *
 */
public interface IPostingsList
{
	/**
	 * Adds the specified docID to the end of the postings list.
	 * @param docID
	 */
	public void add(int docID);
	
	/**
	 * Doubles the amount of preallocated memory for this postings list. 
	 */
	public void doubleSize();
	
	/**
	 * Returns whether this postings list is full or not.
	 * @return true if the postings list is full, otherwise false.
	 */
	public boolean isFull();
	
	/**
	 * Returns the number of Bytes preallocated for this postings list. 
	 * @return The number of preallocated Bytes for this postings list.
	 */
	public int getAllocatedSizeInBytes();
	
	// TODO we also need methods to incrementally step through the postings list
}
