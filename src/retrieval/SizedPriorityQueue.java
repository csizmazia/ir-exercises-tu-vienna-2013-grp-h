package retrieval;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/** 
 * Code by 
 * http://grunt1223.iteye.com/blog/909739
 *
 */
public class SizedPriorityQueue<T>
{
	private int mSize;
	private boolean mGetLowest = true;
	private LinkedList<T> mList;
	private LinkedList<Double> mPriorities;
	private Comparator<T> mComparator;

	/**
	 * Creates a fixed size priority queue that only tracks N values
	 * 
	 * @param size
	 *            - The maximum number of values to store
	 * @param getLowest
	 *            - false means to track the highest N values, true means to
	 *            track the lowest N values
	 */
	public SizedPriorityQueue(int size, boolean getLowest)
	{
		mSize = size;
		mGetLowest = getLowest;
		mList = new LinkedList<T>();
		mPriorities = new LinkedList<Double>();
	}

	/**
	 * Creates a fixed size priority queue with an explicit comparator for the
	 * class that you want to track. This can be handy if the generic class you
	 * have doesn't implement {@link Comparable}
	 * 
	 * @param size
	 *            - The maximum number of values to store
	 * @param getLowest
	 *            - false means to track the highest N values, true means to
	 *            track the lowest N values
	 * @param comparator
	 *            - Explicit comparator for the class you are tracking
	 */
	public SizedPriorityQueue(int size, boolean getLowest, Comparator<T> comparator)
	{
		this(size, getLowest);
		mComparator = comparator;
	}

	/**
	 * Add a value to the current list of items, it will be inserted into the
	 * correct position in the list if it has a higher priority than the other
	 * items, otherwise it will be dropped
	 * 
	 * @param value
	 */
	public void add(T value)
	{
		if (mComparator == null)
			throw new RuntimeException("Trying to use priority queue default add without comparator defined");
		int index = 0;
		for (T val : mList)
		{
			// int comparison = val.compareTo(value);
			int comparison = mComparator.compare(val, value);
			if (mGetLowest && comparison < 0)
				break;
			if (!mGetLowest && comparison > 0)
				break;
			index++;
		}

		if (index < mSize - 1)
			mList.add(index, value);

		if (mList.size() > mSize)
			mList.removeLast();
	}

	/**
	 * Add a value to the current list of items, it will be inserted into the
	 * correct position in the list if it has a higher priority than the other
	 * items, otherwise it will be dropped
	 * 
	 * @param value
	 */
	public void add(T value, double priority)
	{
		int index = 0;

		for (double val : mPriorities)
		{
			double comparison = val - priority;//priority - val;

			if (mGetLowest && comparison < 0)
				break;
			if (!mGetLowest && comparison > 0)
				break;
			index++;
		}

		if (index < mSize - 1)
		{
			mList.add(index, value);
			mPriorities.add(index, priority);
		}

		if (mList.size() > mSize)
		{
			mList.removeLast();
			mPriorities.removeLast();
		}
	}

	/**
	 * Like any ohter queue, it returns the top
	 * 
	 * @return
	 */
	public T pop()
	{
		if (mPriorities.size() > 0)
			mPriorities.pop();
		return mList.pop();
	}

	/**
	 * Just returns the top in the list, doesn't remove it
	 * 
	 * @return
	 */
	public T poll()
	{
		return mList.peek();
	}

	/**
	 * @return The size of current list
	 */
	public int size()
	{
		return mList.size();
	}

	/**
	 * Returns an ordered list of all of the scores currently held
	 * 
	 * @return
	 */
	public List<T> getAllScores()
	{
		return mList;
	}
}