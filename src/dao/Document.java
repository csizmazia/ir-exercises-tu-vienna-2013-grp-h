package dao;

/**
 * Stores document meta data (document ID, document name and document category)
 * 
 */
public class Document
{
	private int id; // if there are more than 2,147,483,647 documents we would
					// have to use long instead!
	private String category;
	private String name;

	public Document(int _id, String _docPath)
	{
		this.id = _id;
		String[] path_parts = _docPath.split("/");
		this.name = path_parts[2];
		this.category = path_parts[1];
	}

	/**
	 * Returns the id of this document.
	 * 
	 * @return The id of this document.
	 */
	public int getId()
	{
		return this.id;
	}

	public String getCategory()
	{
		return this.category;
	}

	public String getName()
	{
		return this.name;
	}

	/**
	 * Debugging method. Returns a String representation of this Token.
	 */
	@Override
	public String toString()
	{
		return "docID = " + Integer.toString(this.id) + ", name = " + this.name + ", cat = " + this.category;
	}

}
