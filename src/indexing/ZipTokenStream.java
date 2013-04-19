package indexing;

import java.io.*;

import dao.Document;
import dao.Token;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.*;

/**
 * Class to tokenize a zipped file using standard Java API's zipFile functionality. Files are processed in chunks of size
 * <code>BUFFER_SIZE_IN_BYTES</code>, terms are split using the delimiters defined in <code>TERM_LIMITER</code>. 
 * 
 * Note that ZipTokenStream is NOT thread safe, meaning that methods should never be called concurrently!
 * 
 */
public class ZipTokenStream implements ITokenStream<Token>
{
	private boolean useStemming;
	private ZipFile zipFile;
	private Enumeration<? extends ZipEntry> fileEntries;
	private boolean hasNext;
	
	private char[] currentString;
	private ZipEntry currentEntry;
	private Document currentDocument;
	private InputStream is;
	private Reader in;
	
	private Token nextToken; // the next token
	private Token nextNextToken; // the token after the next token
	
	public static int DOC_ID = 0; // will be increased by one for each new document
	
	public static final int BUFFER_SIZE_IN_BYTES = 1024; // 1 kB, TODO choose good value here, maybe do some testing what values work / are fast. Must be at least as long as the longest occurring word! *Edit* 1kB seems to work fine for our documents, 1MB is much slower.
//	public static final char[] TERM_LIMITER = {' ', '\n', '\t', '\r', 30}; //delimiters to use. @30: Record Separator
	
	private int currentOffset;
	private int offsetOfLastValidChar;

	private Stemmer stemmer;
	
	private ArrayList<String> filesToTokenize; 
	
	private boolean skipMetaData;
	private boolean newDocLoaded;
	
	/**
	 * Constructor, initializes the stream by loading the first chunk of data and caching the first two tokens.
	 * 
	 * @param _zipFile The zipped file to process.
	 * @param _useStemming Whether or not to use stemming.
	 * @throws ZipException Thrown if creation of the zipFile fails.
	 * @throws IOException Thrown if any of the IO operations involved fails. TODO define safe fallback etc.!
	 */
	public ZipTokenStream(File _zipFile, boolean _useStemming) throws ZipException, IOException
	{
		filesToTokenize = new ArrayList<String>(0);
		zipFile = new ZipFile(_zipFile); // throws ZipException and IOException
		useStemming = _useStemming;
		stemmer = new Stemmer();
		
		DOC_ID = 0;

		currentString = new char[BUFFER_SIZE_IN_BYTES]; // holds the current String to be tokenized
		currentOffset = 0;
		skipMetaData = false;
		//hasNext = true;
		
		fileEntries = zipFile.entries(); // a list of all files in the zip file (this is an unordered list!)
	}
	
	public void setSkipMetaData(boolean skip)
	{
		this.skipMetaData = skip;
	}
	
	/**
	 * We assume that META data ends with 'lines: XXX'. This method tries to find the first occurrence of this 
	 * pattern in the current file and advances the tokenstream to after this point. If the pattern is not found, TODO
	 */
	private boolean skipMetaData()
	{
		
		Token temp = this.current();
		int tempDocID = temp.getDoc().getId();
		Document tempDocument = currentDocument;
		ZipEntry tempEntry = currentEntry;
		
		System.out.println("Skipping metda data of "+temp.getDoc().getCategory()+"/"+temp.getDoc().getName());
		
		/*
		if (temp.getDoc().getName().equals("54432"))
		{
			System.out.println();
		}
		*/
		
		boolean success = false;
		while (hasNext && current().getDoc().getId() == tempDocID)
		{
			if (temp.getTerm().contains("line"))
			{
				success = true;
				//next(); // advance tokenstream just after the first occurrence of the search pattern
				break;
			}
			temp = next();
		}
		
		if (!success)
		{
			currentDocument = tempDocument;
			currentEntry = tempEntry;
		}
		return success;
	}
	
	public void restrictToFiles(String[] files) {
		filesToTokenize = new ArrayList<String>(files.length);
		for (String file : files) {
			filesToTokenize.add(file);
		}
	}
	
	public void restrictToFile(String file) {
		filesToTokenize = new ArrayList<String>(1);
		filesToTokenize.add(file);
	}
	
	public void initialize() {
		initNextFileEntry(); // let currentEntry point to the first non-empty non-directory entry in the enumeration and initialize the associated InputStream. Also sets the hasNext variable.
		
		processNextChunk(); // initialize currentString with the first chunk of data
		
		// initialize the stream by caching the first two terms:
		nextToken = prepareNextToken();
		nextNextToken = prepareNextToken();
		
		hasNext = nextToken != null;
		
		
		if (skipMetaData)
		{
			System.out.println("New Document, skipping data...");
			newDocLoaded = false;
			boolean success = skipMetaData();
			if (!success)
			{
				System.err.println("Error trying to skip META data. File "+currentEntry.getName()+" may not conform to the standard format.");
				// TODO reset file to beginning
			}
		}
	//	System.out.println("####################################################");
	}
	
	/**
	 * We cache two tokens: nextToken and nextNextToken. This is needed to ensure that hasNext is indeed only true iff there is at least one valid 
	 * token left. We have to take care of several special cases such as empty files, files consisting only of delimiters (which shouldn't happen, but
	 * who knows...) and so on. 
	 * 
	 * This method generates the next token in the stream and caches it for use in the <code>next</code> method. Files are processed 
	 * in chunks of size <code>BUFFER_SIZE_IN_BYTES</code>, additional chunks are loaded as needed. If the current document ends, this 
	 * method searches for the next valid term in the subsequent files. Only if there are no more valid terms will this method return null. 
	 * 
	 * @return The next token in the stream or null if the stream does not contain any more tokens.
	 */
	private Token prepareNextToken()
	{
		int beginningOfNextTerm = getBeginningOfNextTerm();
		int endOfNextTerm = -1;
		
		if (beginningOfNextTerm == -1)
		{
			boolean newChunkLoaded;
			
			//currentString = ""; // we can discard the current String since it does not contain any terms.
			currentOffset = 0;
			
			while (newChunkLoaded = processNextChunk()) // try to find a valid next chunk. Valid == contains at least one term
			{
				beginningOfNextTerm = getBeginningOfNextTerm();
				if (beginningOfNextTerm != -1)
				{
					break;
				}
				//currentString = "";
			}
			
			if (beginningOfNextTerm == -1) // we have failed to find a valid next chunk. Try the next file(s):
			{
				while(fileEntries.hasMoreElements() && filesToTokenize.size() == 0) // TODO this is a really ugly hack to make it work for now, need to refactor some changes by stefan...
				{
					initNextFileEntry(); 
					
					//currentString = ""; // we can discard the current String since it does not contain any terms.
					currentOffset = 0;
					
					while (newChunkLoaded = processNextChunk()) // try to find a valid next chunk. Valid == contains at least one term
					{
						beginningOfNextTerm = getBeginningOfNextTerm();
						if (beginningOfNextTerm != -1)
						{
							break;
						}
						//currentString = "";
					}
					
					if (beginningOfNextTerm == -1)
					{
						continue; // file does not contain any terms, try next file
					}
					else
					{
						break; // we have found the next term
					}						
				}
				if (beginningOfNextTerm == -1)
				{
					// we have searched all files, there are no more files remaining. Return null to let the caller know that there are no more terms in the tokenStream:
					return null;
				}
			}
			
		}
		
		if (beginningOfNextTerm == -1) // should never happen unless I screwed up with the code above...
		{
			System.err.println("Bug-Alert! Check ZipTokenStream's prepareNextToken()!");
			return null;
		}
			
			
		// At this point we know that there is at least one more term (beginningOfNextTerm points to a valid character in currentString). 
		// Now we need to find the end of this new term:
		
		endOfNextTerm = getEndOfNextTerm(beginningOfNextTerm);
		if (endOfNextTerm == -1)
		{
			// may happen at the end of chunks and documents. First, we try to load the next chunk:
			
			// copy the remaining chars to the beginning of the array:
			int counter = 0;
			for (int i = beginningOfNextTerm; i < offsetOfLastValidChar; i++)
			{
				currentString[counter++] = currentString[i];
			}
			
			beginningOfNextTerm = 0;
			
			currentOffset = counter; // we set the offset s.t. processNextChunk won't overwrite the remaining characters of the last chunk of data
			boolean newChunkLoaded = processNextChunk();
			currentOffset = 0; // we reset the offset
			
			if (!newChunkLoaded) // we have reached the end of the document. The last term ends with the last character:
			{
				endOfNextTerm = offsetOfLastValidChar;
			}
			else // loaded new chunk
			{
				endOfNextTerm = getEndOfNextTerm(beginningOfNextTerm);
				if (endOfNextTerm == -1) // may happen if the new chunk consists of only one char sequence without a delimiter
				{
					endOfNextTerm = offsetOfLastValidChar;
				}
			}
			
			
		}
		
		String term;
		if (useStemming)
		{
			stemmer.add(currentString, beginningOfNextTerm, endOfNextTerm - beginningOfNextTerm); // this will also convert the term to lower case
			stemmer.stem();
			term = stemmer.toString();
		//	System.out.println("Stemming Result: "+stemmer.toString());
		}
		else
		{
			term = new String(currentString, beginningOfNextTerm, endOfNextTerm - beginningOfNextTerm);
			term = term.toLowerCase(); // convert to lower case
		}
			
		
		if (endOfNextTerm < offsetOfLastValidChar)
		{
			currentOffset = endOfNextTerm + 1;
		}
		else
		{
			currentOffset = 0; 
		}
		
		
	//	System.out.println(term);
		Token newToken = new Token(term, currentDocument);
		
		
		return newToken;
	}

	@Override
	public Token next()
	{
		/*
		if (nextToken.getDoc().getName().equals("54432"))
		{
			System.out.println();
		}
		*/
		
		Token currentToken = nextToken;
		
		nextToken = nextNextToken;
		nextNextToken = prepareNextToken();
		
		hasNext = nextToken != null;
		
		
		if (newDocLoaded && hasNext && currentToken != null) // && currentToken.getDoc().getId() == currentDocument.getId()
		{
		//	System.out.println("New Document, skipping data...");
			if (skipMetaData)
			{
				newDocLoaded = false;
				boolean success = skipMetaData();
				if (!success)
				{
					try
					{
						is = zipFile.getInputStream(currentEntry); // rewind the stream to the beginning
						InputStreamReader isr = null;
						in = null;
						isr = new InputStreamReader(is, "UTF8");
						in = new BufferedReader(isr);
						processNextChunk();
						nextToken = prepareNextToken();
						nextNextToken = prepareNextToken();
						
						hasNext = nextToken != null;
						currentToken = nextToken;
					}		
					catch (IOException ioe)
					{
						ioe.printStackTrace();
					}
				//	System.err.println("Error trying to skip META data. File "+currentEntry.getName()+" may not conform to the standard format.");
					// TODO reset file to beginning
				}
			}

		//	System.out.println("####################################################");
		}
		if (nextToken != null && nextNextToken != null)
		{
			newDocLoaded = nextToken.getDoc().getId() != nextNextToken.getDoc().getId();
		}

			
		
	//	System.out.println(currentToken.getTerm());
		return currentToken;
	}


	@Override
	public boolean hasNext()
	{
		return hasNext;
	}
	
	/**
	 * Finds the next non-empty chunk of data and appends it to the <code>currentString</code> variable. 
	 * 
	 * @return Returns whether a chunk of data was found and processed or not. Thus will only return false if all
	 * files in the zipFile have been processed. 
	 */
	private boolean processNextChunk()
	{
		if (is == null)
		{
			return false; // this happens if we have finished indexing files
		}
		
		int read;		

		try
		{
			if ((read = in.read(currentString, currentOffset, BUFFER_SIZE_IN_BYTES - currentOffset)) > -1) // try to read the first BUFFER_SIZE_IN_BYTES chars into the buffer. This fails if the specified file is empty.
			{
				offsetOfLastValidChar = currentOffset + read;
				
				return true;
			}
			else
			{
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
	
	
	/**
	 * 
	 * Initializes the next entry in the zipFile associated with this ZipTokenStream. This does 3 things:
	 * - lets <code>currentEntry</code> point to the next non-empty, non-directory entry in the zipFile (null if there are no remaining files)
	 * - initializes the IOStream of the new entry (and closes the stream of the previous entry) (null if there are no remaining files)
	 * - sets the hasNext variable
	 */
	private void initNextFileEntry()
	{
		try
		{
			if (is != null)
			{
				is.close(); // close the old input stream
			}
			
			
			currentEntry = null;
			ZipEntry tempEntry = null;
			
			while (fileEntries.hasMoreElements())
			{
				tempEntry = fileEntries.nextElement();
	
				if (tempEntry.isDirectory()) // disregard directory entries
				{
					continue;
				}
				
				if(this.filesToTokenize.size() > 0 && !this.filesToTokenize.contains(tempEntry.getName()))  { // disregard all files that are not asked for
					continue;
				}
				
				currentEntry = tempEntry;
				
				is = zipFile.getInputStream(currentEntry); // init new input stream
				InputStreamReader isr = null;
				in = null;
				
				try
				{
					isr = new InputStreamReader(is, "UTF8");
					
				}
				catch (UnsupportedEncodingException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				in = new BufferedReader(isr);
				
		//		newDocLoaded = true;
				
				DOC_ID += 1; // note that empty files will still increase the counter. This shouldn't be a problem imho?
				currentDocument = new Document(DOC_ID,currentEntry.getName());
				return;
			}
			
			// if we get this far than we have reached the end of the enumeration without finding a valid next file. 
			// We set currentEntry and is to null s.t. the tokenStream dries out after delivering the final (cached) term:
			currentEntry = null;
			zipFile.close();
			is = null; 
		}
		catch (IOException ioe)
		{
			// ignore
		}
	}
	
	/**
	 * Attempts to find the beginning of the next term (== first valid character in currentString).
	 * @return The index of the first valid character in currentString or -1 if there are no valid characters left.
	 */
	private int getBeginningOfNextTerm()
	{
		int length = offsetOfLastValidChar;
		if (length == 0)
			return -1;
		
		int pos = currentOffset;
		char currentChar;
		boolean foundBeginning = false;
		
		// search for the beginning of the new term == the position of the first char != delimiters
		while (pos < length)
		{
			/*
			String test = String.valueOf(currentString.charAt(pos));
			if (pos == 56)
			{
				System.out.println("test = "+test);
				System.out.println("size = "+test.length());
				int charInt = currentString.charAt(pos);
				System.out.println("int = "+charInt);
			}
			*/
			currentChar = currentString[pos];
			
			if (!(Character.isLetter((int)currentChar)))// || Character.isDigit((int)currentChar)))
			{
				pos++;
				continue; // every term should start with a proper letter.
			}
			else
			{
				int end = getEndOfNextTerm(pos);
				if (end - pos > 3) // discard short words
				{
					foundBeginning = true;
					break;
				}
				else
				{
					pos ++;
					continue;
				}
				
				//foundBeginning = true;
				//break;
				
				
			}
				
			
			
			
			/*
			for (int i = 0; i < TERM_LIMITER.length; i++)
			{
				if (currentChar == TERM_LIMITER[i])
				{
					foundBeginning = false; // character is equal to one of the delimiters, thus we have not yet found the beginning of the next term. Advance search position and try again.
					break;
				}
			}
			
			
			
			if (foundBeginning)
			{
				break;
			}
			
			pos++;
			*/
		}
		
		if (!foundBeginning)
		{
			return -1; // beginning of the next term could not be found. May happen for instance if a document ends with several delimiters
		}
		
		
		
		return pos;
	}
	
	/**
	 * Attempts to find the end of the current term (== first delimiter after <code>beginningOfTerm</code>).
	 * @return The index of the first delimiter in currentString after beginningOfTerm or -1 if there is no such delimiter in the remaining currentString.
	 */
	private int getEndOfNextTerm(int beginningOfTerm)
	{
		int length = offsetOfLastValidChar;

		if (length == 0)
			return -1;
		
		if (beginningOfTerm <= -1)
			return -1;
		
		int pos = beginningOfTerm+1; // since beginningOfTerm points at a valid character we start looking at the next possible location
		char currentChar;
		char nextChar;
		boolean foundEnd = false;
		
		// search for the end of the new term == the position of the first char == delimiters
		while (pos < length)
		{
			currentChar = currentString[pos];
			
			if (pos+1 < length)
			{
				nextChar = currentString[pos+1];
			}
			else
			{
				nextChar = ' '; // just some default value
			}
			
			if (!(Character.isLetter((int)currentChar) || Character.isDigit((int)currentChar) || currentChar=='@' || (currentChar == '.' && Character.isLetter((int)nextChar)) || (currentChar == '-' && Character.isLetter((int)nextChar)))) 
			{
				foundEnd = true;
				break;
			}
			else
			{
				pos++;
				continue;
			}
			
			/*
			for (int i = 0; i < TERM_LIMITER.length; i++)
			{
				if (currentChar == TERM_LIMITER[i])
				{
					foundEnd = true;
					break;
				}
			}
			
			
			if (foundEnd)
			{
				break;
			}
			
			
			pos++;
			*/
		}
		
		if (!foundEnd)
		{
			return -1; // end of the next term could not be found. Happens at the end of documents or at the end of a block within a document
		}
		
		
		
		return pos;
	}

	@Override
	public Token current()
	{
		return nextToken;
	}
}
