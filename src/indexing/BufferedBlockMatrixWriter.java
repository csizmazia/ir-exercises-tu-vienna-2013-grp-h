package indexing;

import java.io.*;
import java.util.Iterator;
import java.util.Map.Entry;

import dao.*;

/**
 * Class that provides fast buffered writing of block matrices while trying to allocate as little memory as possible. 
 * Is still a WIP but so far, the memory footprint is relatively low even for large files and many instances of this class, so long 
 * as the garbage collector does it's job.
 * 
 * TODO replace Writer out with FileChannel to increase performance
 *
 */
public class BufferedBlockMatrixWriter
{
	private File file;
	
	private char[] currentString;
	
	private Writer out;
	
	public static final int BUFFER_SIZE_IN_BYTES = 1024; // 1 kB, TODO choose good value here. Needs to be large enough to fit any term in the dictionary inside!

	
	private DocumentTermMatrix matrix;

	/**
	 * Constructor, creates a BufferedBlockDictionaryWriter with the given parameters. Does not write to the disk, merely creates 
	 * the object!
	 * 
	 * @param _file The file to write the dictionary to.
	 * @param _dict The dictionary to write to the file.
	 */
	public BufferedBlockMatrixWriter(File _file, DocumentTermMatrix _matrix)
	{
		file = _file;

		currentString = new char[BUFFER_SIZE_IN_BYTES]; // holds the current String to be tokenized
		
		matrix = _matrix;
	}
	
	/**
	 * Attempts to write the contents of the <code>matrix</code> variable to the the file <code>file</code>. 
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
			if (!file.exists())
			{
				File parent = new File(file.getParent());
				parent.mkdirs();
				file.createNewFile();
			}
			out = new FileWriter(file);
		}
		catch (IOException ioe)
		{
			System.err.println();
			System.err.println("Error opening output stream to "+file.getPath()+"! --Stack Trace follows.");
			ioe.printStackTrace();
		}
	
		
		
		Iterator<Entry<Integer, DocumentTermList>> it = matrix.getMatrix().entrySet().iterator();
		Entry<Integer, DocumentTermList> currentDocument;
		
		Iterator<Entry<Integer, Float>> termIt = null; 
		
		int numOfCharsFilled = 0;
		Entry<Integer, Float> entry;
		
		while (it.hasNext()) // for every documentTerm list:
		{
			currentDocument = it.next();
			char[] docID_Chars = String.valueOf(currentDocument.getKey()).toCharArray();
			for (int i = 0; i < docID_Chars.length; i++)
			{
				currentString[numOfCharsFilled++] = docID_Chars[i];
			}
			

			currentString[numOfCharsFilled++] = '\t';
			
			
			termIt = currentDocument.getValue().getDocTermEntries().entrySet().iterator();
			while (termIt.hasNext()) // for every termEntry in the current documentTerm list:
			{
				while(numOfCharsFilled < BUFFER_SIZE_IN_BYTES - 30 && termIt.hasNext()) 
				{
					entry = termIt.next();
					
					currentString[numOfCharsFilled++] = '<';
					
					char[] termID_Chars = String.valueOf(entry.getKey()).toCharArray(); 
					char[] idf_Chars = String.valueOf(entry.getValue()).toCharArray();
					
					for (int i = 0; i < termID_Chars.length; i++)
					{
						currentString[numOfCharsFilled++] = termID_Chars[i];
					}
					
					currentString[numOfCharsFilled++] = ':';
					
					for (int i = 0; i < idf_Chars.length; i++)
					{
						currentString[numOfCharsFilled++] = idf_Chars[i];
					}
					
					currentString[numOfCharsFilled++] = '>';
				}
				out.write(currentString, 0, numOfCharsFilled);
				numOfCharsFilled = 0;
				
				if (!termIt.hasNext())
				{
					currentString[0] = '\n';
					numOfCharsFilled = 1;
					break;
				}		
			}
			
		}

		out.close();
	}	
}
