package indexing;

/**
 * Defines the methods a TokenStream needs to implement.
 *
 */
public interface ITokenStream<T>
{
	
	/**
	 * @return Returns the next Token in this tokenStream.
	 */
	public T next();
	
	/**
	 * Similar to {@link #next()}, but does not remove the token from the stream. 
	 * 
	 * @return
	 */
	public T current();
	
	/**
	 * 
	 * @return Returns whether or not this tokenStream has tokens left.
	 */
	public boolean hasNext();
}
