package dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;


/**
 * Inverted PostingsList, this class stores one row in the DocumentTermMatrix.
 *
 */
public class DocumentTermList implements IDocumentTermList
{
	private LinkedHashMap<Integer, Float> docTermEntries; // termIDs are the key, termFrequencies/IDFs are the value

	public DocumentTermList()
	{
		/**
		 * The docTermEntries list uses a LinkedHashMap to store termIDs as Keys
		 * and termFrequency (which will be replaced by IDFs) as Values. The initial size is 1 and the loading
		 * factor is 1.0f, because LinkedHashMap will internally double the size
		 * of allocated memory each time size >= capacity*loadFactor, which is
		 * exactly what we want for our Indexer.
		 */
		this.docTermEntries = new LinkedHashMap<Integer, Float>(1, 1.0f);
	}

	/**
	 * Adds the specified termID to the list. While we are still adding terms, the values are still term frequencies! 
	 * Thus, repeated calls of this method with the same termID will not add additional entries to the list, but merely
	 * increase the termFrequency by 1 for each call.
	 */
	@Override
	public void add(int termID)
	{
		Float current_tf = docTermEntries.get(termID);
		if (current_tf != null)
		{
			this.docTermEntries.put(termID, current_tf + 1.0f);
		}
		else
		{
			this.docTermEntries.put(termID, 1.0f);
		}

	}
	
	/**
	 * Adds the specified entry to the list. If the entry contained the element, the value is increased by the specified amount. Else a new 
	 * entry with the provided value is created. 
	 * @param termID The termID of the term to add.
	 * @param value
	 */
	public void add(int termID, float value)
	{
		Float previousValue = docTermEntries.get(termID);
		if (previousValue != null)
		{
			this.docTermEntries.put(termID, value + previousValue);
		}
		else
		{
			this.docTermEntries.put(termID, value);
		}
		
	}


	@Override
	public void doubleSize()
	{
		// not needed since Java takes care of that for us.

	}

	@Override
	public boolean isFull()
	{
		return false;
	}

	@Override
	public int getAllocatedSizeInBytes()
	{
		return -1;
	}

	/**
	 * Getter for member variable <code>docTermEntries</code>
	 */
	public Map<Integer, Float> getDocTermEntries()
	{
		return docTermEntries;
	}

	/**
	 * Returns the term frequency of the provided termID. This method assumes that IDFs have not been calculated yet (since 
	 * that would overwrite termFrequency values).
	 * @param termID
	 * @return
	 */
	public Float getTermFrequency(Integer termID)
	{
		Float termFrequency = docTermEntries.get(termID);
		return termFrequency == null ? 0 : termFrequency;
	}


	/**
	 * Removes all elements from this list.
	 */
	public void clear()
	{
		docTermEntries.clear();
	}

	/**
	 * Applies termFrequency Thresholding using the provided thresholds. This method assumes that IDFs have not been calculated yet (since 
	 * that would overwrite termFrequency values).
	 * @param lowThresh
	 * @param highThresh
	 * @return The number of terms removed;
	 */
	public int applyTfThresholds(int lowThresh, int highThresh)
	{
		Iterator<Float> it = docTermEntries.values().iterator();
		int cnt = 0;
		float Tf;
		while (it.hasNext())
		{
			Tf = it.next();
			if (Tf <= lowThresh || Tf >= highThresh)
			{
				it.remove();
				cnt++;
			}
		}
		return cnt;
	}
	

	/**
	 * Sorts this termList by termID.
	 */
	public void sortTermsByID()
	{
		List<Map.Entry<Integer, Float>> entries = new ArrayList<Map.Entry<Integer, Float>>(docTermEntries.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<Integer, Float>>()
		{
			public int compare(Map.Entry<Integer, Float> a, Map.Entry<Integer, Float> b)
			{
				return a.getKey().compareTo(b.getKey());
			}
		});

		docTermEntries.clear();

		for (Map.Entry<Integer, Float> entry : entries)
		{
			docTermEntries.put(entry.getKey(), entry.getValue());
		}

	}

	/**
	 * Calculates the IDF for the entries of this list. This method assumes that IDFs have not been calculated yet. 
	 * Overwrites the values of this list with the newly calculated IDFs. Also see {@link DocumentTermMatrix}.
	 * @param vocabulary
	 */
	public void calculateIDFs(Vocabulary vocabulary)
	{
		Set<Entry<Integer, Float>> list = docTermEntries.entrySet();
		Iterator<Entry<Integer, Float>> it = list.iterator();
		
		Entry<Integer, Float> entry;
		VocabularyEntry vocEntry;
		int docFrequency;
		int termFrequency;
		
		int docCount = vocabulary.getTotalNumberOfDocuments();
		
		while (it.hasNext())
		{
			entry = it.next();
			vocEntry = vocabulary.get(entry.getKey());
			docFrequency = vocEntry.getDocFrequency();
			termFrequency = (int)Math.floor(entry.getValue());//vocEntry.getCollectionFreq();
			entry.setValue((float)(  Math.log10(1 + termFrequency) * Math.log10((docCount / docFrequency))));		
		}
		
		
	}
}
