package indexing;


/**
 * Helper DAO that simply stores a <ITokenStream, int> tuple. Is used by our priority queue 
 * when merging blockDictionaries and for merging documentTerm Matices (see {@link indexing.BlockIndexer}). 
 *
 */
public class PriorityQueueTermReaderToken<T>
{
	public int number; // the number of the reader which determines the order of the merging
	public ITokenStream<T> reader;
}
