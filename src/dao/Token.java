package dao;

/**
 * A token as defined in "An Introduction to Information Retrieval" (Manning et al. 2009) 
 * is a term + docID pair. Here we define terms to be Strings and docIDs to be integers.
 *
 */
public class Token
{
	private String term;
	private Document doc;
	
	/**
	 * Constructs a new Token Object and initializes the data values to the provided parameters.
	 * @param _term The term of this token.
	 * @param _docID The document ID that this term occurs in.
	 */
	public Token(String _term, Document _doc)
	{
		this.term = _term;
		this.doc = _doc;
	}

	
	/**
	 * Returns the term of this token. 
	 * @return The term of this token.
	 */
	public String getTerm()
	{
		return term;
	}

	/**
	 * Returns the document of this token.
	 * @return The document of this token.
	 */
	public Document getDoc()
	{
		return this.doc;
	}
	
	/**
	 * Debugging method. Returns a String representation of this Token.
	 */
	@Override
	public String toString()
	{
		return "docID = " + Integer.toString(doc.getId())+", term = " + term;
	}
	
}
