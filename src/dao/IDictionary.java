package dao;

/**
 * Defines methods a dictionary needs to provide. 
 *
 */
public interface IDictionary
{
	/**
	 * Adds the term <code>new_term</code> to the dictionary and returns a new (i.e. empty) postings list for this term.
	 * 
	 * @param new_term The term to add to the dictionary.
	 * @return The newly created, empty postings list.
	 */
	public PostingsList addTerm(String new_term);
	
	/**
	 * Returns the postings list for the term <code>term_in_dictionary</code> or null if <code>term_in_dictionary</code> is not in the dictionary.
	 * @param term_in_dictionary The term of the postings list to retrieve.
	 * @return The postings list of the specified term of null if the term is not in the dictionary. 
	 */
	public PostingsList getPostingsList(String term_in_dictionary);
	

	
	/**
	 * Sorts the terms in the dictionary. 
	 */
	public void sortTerms();
	
	/**
	 * Writes the current dictionary to the specified output file while omitting terms which occur too rarely / too often. 
	 * Note that you usually want to call {@link #sortTerms() sortTerms} prior to calling this method. 
	 * 
	 * @param outputFile The file to write the dictionary to.
	 * @throws java.io.IOException
	 */
	public void writeToDisk(java.io.File outputFile, int lowThresh, int highThresh) throws java.io.IOException;
}
