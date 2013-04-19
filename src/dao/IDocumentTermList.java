package dao;

/**
 * Defines the methods a documentTerm list needs to implement for a dictionary. Specifically, this list stores 
 * hashed terms as keys and term frequency as values.
 *
 */
public interface IDocumentTermList
{
	/**
	 * Adds the specified term to the end of the list.
	 * @param termID
	 */
	public void add(int termID);
	
	/**
	 * Doubles the amount of preallocated memory for this list. 
	 */
	public void doubleSize();
	
	/**
	 * Returns whether this list is full or not.
	 * @return true if the list is full, otherwise false.
	 */
	public boolean isFull();
	
	/**
	 * Returns the number of Bytes preallocated for this list. 
	 * @return The number of preallocated Bytes for this list.
	 */
	public int getAllocatedSizeInBytes();
}
