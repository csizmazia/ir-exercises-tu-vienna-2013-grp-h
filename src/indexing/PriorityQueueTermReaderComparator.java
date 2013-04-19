package indexing;

import java.util.Comparator;

/**
 * Comparator to sort our priority queue first by the term and then by the number of the reader (= fileNumber). Also used
 * when merging documentTermMatrices. 
 *
 */
public class PriorityQueueTermReaderComparator<T extends Comparable<T>> implements Comparator<PriorityQueueTermReaderToken<T>>
{
    @Override
    public int compare(PriorityQueueTermReaderToken<T> x, PriorityQueueTermReaderToken<T> y)
    {
    	int compareVal = x.reader.current().compareTo(y.reader.current()); 
    	if (compareVal == 0)
    	{
    		if (x.number < y.number)
    		{
    			return -1;
    		}
    		else 
    		{
    			return 1;
    		}
    	}
    	return compareVal;
    }
}

