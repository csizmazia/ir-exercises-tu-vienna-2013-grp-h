package indexing;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import dao.*;

/**
 * Class that provides fast buffered writing of block dictionaries while trying to allocate as little memory as possible. 
 * Is still a WIP but so far, the memory footprint is relatively low even for large files and many instances of this class, so long 
 * as the garbage collector does it's job.
 * 
 * TODO replace Writer out with FileChannel to increase performance
 *
 */
public class BufferedBlockDictionaryWriter
{
	private File file;
	
	private char[] currentString;
	
	private Writer out;
	
	public static final int BUFFER_SIZE_IN_BYTES = 1024; // 1 kB, TODO choose good value here. Needs to be large enough to fit any term in the dictionary inside!

	
	private Dictionary dict;

	/**
	 * Constructor, creates a BufferedBlockDictionaryWriter with the given parameters. Does not write to the disk, merely creates 
	 * the object!
	 * 
	 * @param _file The file to write the dictionary to.
	 * @param _dict The dictionary to write to the file.
	 */
	public BufferedBlockDictionaryWriter(File _file, Dictionary _dict)
	{
		file = _file;

		currentString = new char[BUFFER_SIZE_IN_BYTES]; // holds the current String to be tokenized
		
		dict = _dict;
	}
	
	/**
	 * Attempts to write the contents of the <code>dict</code> variable to the the file <code>file</code>. 
	 * 
	 * @throws IOException If the writing operation fails.
	 */
	public void writeToFile() throws IOException
	{
		try
		{
			if (out != null)
			{
				out.close(); 
			}
			
			out = new FileWriter(file);
		}
		catch (IOException ioe)
		{
			System.err.println();
			System.err.println("Error opening output stream to "+file.getPath()+"! --Stack Trace follows.");
			ioe.printStackTrace();
		}
		
		List<String> terms = dict.getTerms();
		String current;
		PostingsList pl;
		
		int termCounter = 0;
		
		Entry<Integer, Integer> entry;
				
		int numOfCharsFilled = 0;
		
		Iterator<String> termsIterator = terms.iterator();
		
		while (termsIterator.hasNext())
		{
			current = termsIterator.next();
			pl = dict.getPostingsList(current);

			termCounter++;
			
			for (int i = 0; i < current.length(); i++)
			{
				currentString[numOfCharsFilled++] = current.charAt(i);
			}
			currentString[numOfCharsFilled++] = '\t';

			Iterator<Entry<Integer, Integer>> postingsIterator = pl.getPostings().entrySet().iterator();
			while (postingsIterator.hasNext())
			{
				while(numOfCharsFilled < BUFFER_SIZE_IN_BYTES - 25 && postingsIterator.hasNext()) // max int length: 10 chars, \t<int:int> = max 24 chars long, +1 for the optional '\n'
				{
					entry = postingsIterator.next();
					
					currentString[numOfCharsFilled++] = '<';
					
					char[] docID_Chars = String.valueOf(entry.getKey()).toCharArray();
					char[] tf_Chars = String.valueOf(entry.getValue()).toCharArray();
					
					for (int i = 0; i < docID_Chars.length; i++)
					{
						currentString[numOfCharsFilled++] = docID_Chars[i];
					}
					
					currentString[numOfCharsFilled++] = ':';
					
					for (int i = 0; i < tf_Chars.length; i++)
					{
						currentString[numOfCharsFilled++] = tf_Chars[i];
					}
					
					currentString[numOfCharsFilled++] = '>';
				}
				out.write(currentString, 0, numOfCharsFilled);
				numOfCharsFilled = 0;
			}
			currentString[0] = '\n';
			numOfCharsFilled = 1;
			
		
		}
		
		System.out.print("Wrote "+termCounter+" of a total of "+dict.getTerms().size()+" terms. ");
		out.close();
	}	
}
