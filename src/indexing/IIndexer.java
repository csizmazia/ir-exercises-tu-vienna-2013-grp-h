package indexing;

/**
 * Defines the methods an Indexer needs to implement.
 *
 */
public interface IIndexer
{
	static public final int META_FIELD_COUNT = 3;
	
	/**
	 * Performs indexing of the given array of files (i.e. documents) and saves the result to <code>outputFile</code>.
	 * @param files Array of files to process.
	 * @param outputFile The file to save the index to.
	 */
	public void indexFiles(java.io.File[] files, java.io.File outputFile);
	
	/**
	 * Performs indexing on the given zip file and saves the result to <code>outputFile</code>.
	 * @param zipFile The zip file to process.
	 * @param indexName The name of the index, which will be used to name the resulting file.
	 */
	public void indexZipFile(java.io.File zipFile, String indexName);

	public void setLowTfThreshold(int _lowThresh);
	
	public void setHighTfThreshold(int _highThresh);

	public void setUseStemming(boolean _useStemming);
}
