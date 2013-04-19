package indexing;

import java.io.*;

import dao.DocumentTermMatrixFileEntry;


/**
 * Class to read arbitrarily sized BlockDictionaries one chunk of <code>BUFFER_SIZE_IN_BYTES</code> at a time. The implementation
 * leans heavily on the implementation in {@link ZipTokenStream}, but there are some important changes:
 * 
 */
public class BufferedBlockMatrixReader implements ITokenStream<DocumentTermMatrixFileEntry>
{
	private File file;
	
	private boolean hasNext;
	
	private String currentString; // acts as a buffer to store a chunk of data
	
	private int currentDoc; 
	
	private int nextLineBreak; // a lineBreak indicates that the term has changed
	private int nextTab; // indicates the end of the term
	
	private int docCounter;
	 
	
	private FileInputStream is;
	
	private DocumentTermMatrixFileEntry nextEntry; // the next entry
	private DocumentTermMatrixFileEntry nextNextEntry; // the entry after the next entry
	
	
	
	public static int BUFFER_SIZE_IN_BYTES = 1024; // 1 kB, TODO choose good value here, maybe do some testing what values work / are fast. Must be at least as long as the longest occurring word! *Edit* 1kB seems to work fine for our documents, 1MB is much slower.
	
	
	/**
	 * Constructor, initializes the stream by loading the first chunk of data and caching the first two entries.
	 * 
	 * @param _file The file to process.
	 * @throws IOException Thrown if any of the IO operations involved fails. TODO define safe fallback etc.!
	 */
	public BufferedBlockMatrixReader(File _file) throws IOException
	{
		file = _file;

		currentString = "";
		currentDoc = -1;
		docCounter = 0;
		
		
	
		is = new FileInputStream(file);		
		
		
		
		boolean success = processNextChunk(); // read the first chunk of data, update the nextLineBreak and nextTab variables
		if (success)
		{
			currentDoc = Integer.valueOf(currentString.substring(0, nextTab));
			docCounter++;
			currentString = currentString.substring(nextTab+1);
			nextTab = currentString.indexOf('\t');
			if (nextTab == -1)
			{
				nextTab = Integer.MAX_VALUE;
			}
		}
		else
		{
			throw new IOException("Error reading file "+file.getPath()+". File appears to be empty or corrupt!");
		}
		
		
		// initialize the stream by caching the first two Entries:
		nextEntry = prepareNextEntry();
		nextNextEntry = prepareNextEntry();
		
		hasNext = nextEntry != null;	
	}
	
	/**
	 * We cache two entries: nextEntry and nextNextEntry. This is needed to ensure that hasNext is indeed only true iff there is at least one valid 
	 * entry left.  
	 * 
	 * This method generates the next entry in the stream and caches it for use in the <code>next</code> method. Files are processed 
	 * in chunks of size <code>BUFFER_SIZE_IN_BYTES</code>, additional chunks are loaded as needed. If there are no more valid entries 
	 * this method returns null. 
	 * 
	 * @return The next entry in the stream or null if the stream does not contain any more entries.
	 */
	private DocumentTermMatrixFileEntry prepareNextEntry()
	{	
		DocumentTermMatrixFileEntry nextEntry = new DocumentTermMatrixFileEntry();
		
	//	PostingsListEntry nextEntry = new PostingsListEntry();
		
		int beginningOfNextEntry = currentString.indexOf('<');
		int middleOfNextEntry = currentString.indexOf(':');
		int endOfNextEntry = currentString.indexOf('>');
		
		// does the buffer contain at least one more posting?
		boolean isComplete = beginningOfNextEntry >= 0 && middleOfNextEntry >= 0 && endOfNextEntry >= 0;
		boolean processedNewChunk = true;
		
		// if not, we try to load additional chunks of data to find the next posting
		while (!isComplete && processedNewChunk) 
		{
			processedNewChunk = processNextChunk();
			nextTab = currentString.indexOf('\t');
			if (nextTab == -1)
			{
				nextTab = Integer.MAX_VALUE;
			}
			beginningOfNextEntry = currentString.indexOf('<');
			middleOfNextEntry = currentString.indexOf(':');
			endOfNextEntry = currentString.indexOf('>');
			isComplete = beginningOfNextEntry >= 0 && middleOfNextEntry >= 0 && endOfNextEntry >= 0;	
		}
		
		// we tried to load additional chunks of data to find the next posting, but we failed. Therefore we can conclude that 
		// there are no more chunks of data to process and we return null which will cause the hasNext() method to return false
		// (after the cache has been emptied).
		if (!isComplete)
		{
			return null;
		}
		
		// The standard case, at least one more posting is available and we are still in the postingsList of currentTerm. We 
		// simply determine the entry contents according to their delimiters (reminder: term\t<docID:tf><docID:tf>...\n)
		if (beginningOfNextEntry < nextTab)
		{		
			nextEntry.termID = Integer.parseInt(currentString.substring(beginningOfNextEntry+1, middleOfNextEntry));
			nextEntry.value = Float.parseFloat(currentString.substring(middleOfNextEntry+1, endOfNextEntry));
			nextEntry.docID = currentDoc;

		}
		else // we need to extract the next term first:
		{
			nextLineBreak = currentString.indexOf('\n');

			if (nextLineBreak == -1)
			{
				// error! nextTab is valid, but there is no line break.
				System.err.println("Error parsing matrix file, missing \\n!");
				return null;
			}
			currentDoc = Integer.valueOf(currentString.substring(nextLineBreak+1, nextTab));
			docCounter++;
			
			currentString = currentString.substring(nextTab+1);
			
			beginningOfNextEntry -= nextTab + 1;
			middleOfNextEntry -= nextTab + 1;
			endOfNextEntry -= nextTab + 1;
			
			nextTab = currentString.indexOf('\t');
			if (nextTab == -1)
			{
				nextTab = Integer.MAX_VALUE;
			}
			
			nextEntry.termID = Integer.parseInt(currentString.substring(beginningOfNextEntry+1, middleOfNextEntry));
			nextEntry.value = Float.parseFloat(currentString.substring(middleOfNextEntry+1, endOfNextEntry));
			nextEntry.docID = currentDoc;		
		}
		
		
		
		currentString = currentString.substring(endOfNextEntry+1);
		nextTab -= endOfNextEntry + 1; 
		
		return nextEntry;
	}


	public DocumentTermMatrixFileEntry current()
	{
		return nextEntry;
	}
	
	/**
	 * Returns the next PostingsListEntry in the stream and sets the {@link #hasNext} variable.
	 * @return
	 */
	public DocumentTermMatrixFileEntry next()
	{
		DocumentTermMatrixFileEntry currentToken = nextEntry;
		nextEntry = nextNextEntry;
		nextNextEntry = prepareNextEntry();
		
		hasNext = nextEntry != null;
		
		return currentToken;
	}
	
	/**
	 * Debug method to check whether all documents were retrieved or not.
	 * @return The number of terms processed.
	 */
	public int getDocCounter()
	{
		return this.docCounter;
	}

	/**
	 * Returns whether there are additional entries to be retrieved via calling the {@link #next()} method or not.
	 * @return
	 */
	public boolean hasNext()
	{
		return hasNext;
	}
	
	/**
	 * Finds the next non-empty chunk of data and appends it to the <code>currentString</code> variable. 
	 * 
	 * @return Returns whether a chunk of data was found and processed or not. Will return false if the end of file has been reached.
	 */
	private boolean processNextChunk()
	{
		if (is == null)
		{
			System.err.println("Bug Alert! Called processNextChunk() in BufferedBlockDictionaryReader with InputStream == null!");
			return false; 
		}
		
		int read;
		byte[] buffer = new byte[BUFFER_SIZE_IN_BYTES];

		try
		{
			if ((read = is.read(buffer, 0, BUFFER_SIZE_IN_BYTES)) > -1) // try to read the first BUFFER_SIZE_IN_BYTES bytes into the buffer. This fails if the specified file is empty.
			{
				String str = new String(buffer, 0, read);
				currentString += str;
				nextLineBreak = currentString.indexOf('\n'); // might be -1 if the postings list is too large to fit in the allocated buffer
				if (nextLineBreak == -1)
				{
					nextLineBreak = Integer.MAX_VALUE;
				}
				nextTab = currentString.indexOf('\t');
				if (nextTab == -1)
				{
					nextTab = Integer.MAX_VALUE;
				}
				return true;
			}
			else
			{
				nextLineBreak = currentString.indexOf('\n');
				nextTab = currentString.indexOf('\t');
				if (nextLineBreak == -1)
				{
					nextLineBreak = Integer.MAX_VALUE;
				}
				nextTab = currentString.indexOf('\t');
				if (nextTab == -1)
				{
					nextTab = Integer.MAX_VALUE;
				}
				
				return false;
				// We have reached the eof.
			}
			
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		return false;
		
	}
}
