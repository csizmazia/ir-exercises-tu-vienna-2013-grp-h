package indexing;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import dao.*;

/**
 * Class that provides fast buffered writing of block matrices while trying to allocate as little memory as possible. 
 * Is still a WIP but so far, the memory footprint is relatively low even for large files and many instances of this class, so long 
 * as the garbage collector does it's job.
 * 
 * TODO replace Writer out with FileChannel to increase performance
 *
 */
public class BufferedArffMatrixWriter
{
	private File file;
	
	private char[] currentString;
	
	
	GZIPOutputStream gzos;
	
	private Writer out;
	
	public static final int BUFFER_SIZE_IN_BYTES = 1024; // 1 kB, TODO choose good value here. Needs to be large enough to fit any term in the dictionary inside!

	int lastActiveDocument;
	
	private int numOfCharsFilled;
	
	private Vocabulary vocabulary;
	private LinkedHashMap<Integer, Document> documents; 

	/**
	 * Constructor, creates a BufferedArffMatrixWriter with the given parameters. Does not write to the disk, merely creates 
	 * the object!
	 * 
	 * @param _file The file to write the matrix to.
	 * @param _vocabulary The vocabulary of the matrix.
	 * @param documents Provides class and name of the documentInstances to write.
	 */
	public BufferedArffMatrixWriter(File _file, Vocabulary _vocabulary, LinkedHashMap<Integer, Document> _documents)
	{
		file = _file;

		currentString = new char[BUFFER_SIZE_IN_BYTES]; 
		numOfCharsFilled = 0;
		lastActiveDocument = -1;
		this.vocabulary = _vocabulary;
		this.documents = _documents;
		
		
	}
	
	/**
	 * Opens the filestream and writes the header section of the arff file (containing the vocabulary).
	 * @throws IOException
	 */
	public void init() throws IOException
	{	
		if (!file.exists())
		{
			File parent = new File(file.getParent());
			parent.mkdirs();
			file.createNewFile();
		}
		gzos = new GZIPOutputStream(new FileOutputStream(file));

		out = new BufferedWriter(new OutputStreamWriter(gzos));//, "UTF-8"));
        
	//	out = new FileWriter(file);

		writeHeader(); 
	}
	
	/**
	 * Writes the header section of the arff file, including the vocabulary (as Attributes). The first Attribute 
	 * is the document number, the second attribute the class and the third attribute the name of the instance. 
	 * The remaining Attributes are the vocabulary of the index.
	 */
	private void writeHeader() throws IOException
	{
		out.write("@RELATION documentterm\n\n");
		out.write("% The first three columns are the docID, class and the name of the document.\n");
		out.write("% We add ! in front of the attributes to make them unique (no term starts with a !).\n");
		out.write("@ATTRIBUTE !docID NUMERIC\n");
		out.write("@ATTRIBUTE !class string\n");
		out.write("@ATTRIBUTE !name string\n");
	//	out.write("% Now we write the Vocabulary. The termIDs are offset by the number of attributes before, i.e. 3.");
		

		Iterator<Entry<String, VocabularyEntry>> entries = vocabulary.getVocabulary().entrySet().iterator();
		Entry<String, VocabularyEntry> nextEntry;
		
		
		while (entries.hasNext())
		{
			nextEntry = entries.next();
			out.write("@ATTRIBUTE ");
			out.write(nextEntry.getKey());
			out.write(" NUMERIC\n");
		}
		
		out.write("\n\n@DATA\n");
	}
	
	/**
	 * Called repeatedly, this method writes the given matrix to the DATA section of the arff file and sews parts 
	 * together as needed. 
	 * @param matrix The next part of the complete DocumentTermMatrix to write.
	 * @throws IOException
	 */
	public void writeNextPart(DocumentTermMatrix matrix) throws IOException
	{
		Iterator<Entry<Integer, DocumentTermList>> it = matrix.getMatrix().entrySet().iterator();
		Entry<Integer, DocumentTermList> currentDocument;
		
		Iterator<Entry<Integer, Float>> termIt = null; 
		Document temp;
		
		int firstDocument = matrix.getMatrix().entrySet().iterator().next().getKey();
		
		Entry<Integer, Float> entry;
		while (it.hasNext()) // for every documentTerm list:
		{
			currentDocument = it.next();
			
			/*
			if (currentDocument.getKey() == 462)
			{
				System.out.println();
			}
			*/
			
			
			if (currentDocument.getKey() != lastActiveDocument) // we are at the start of a new documentTerm List
			{
				if (currentDocument.getKey() != firstDocument)
				{
					currentString[numOfCharsFilled++] = '}'; // close the curly brace of the previous document and
					currentString[numOfCharsFilled++] = '\n'; // start a new line for the current document
				}
				
		//		System.out.println("termID = "+currentDocument.getKey()+", term = "+vocabulary.get(currentDocument.getKey()).)

				currentString[numOfCharsFilled++] = '{';
				currentString[numOfCharsFilled++] = '0';
				currentString[numOfCharsFilled++] = ' ';
				
				char[] docID_Chars = String.valueOf(currentDocument.getKey()).toCharArray();
				for (int i = 0; i < docID_Chars.length; i++)
				{
					currentString[numOfCharsFilled++] = docID_Chars[i];
				}
				currentString[numOfCharsFilled++] = ',';

				currentString[numOfCharsFilled++] = '1';
				currentString[numOfCharsFilled++] = ' ';
				
				temp = documents.get(currentDocument.getKey());
				for (int i = 0; i < temp.getCategory().length(); i++)
				{
					currentString[numOfCharsFilled++] = temp.getCategory().charAt(i);
				}
				currentString[numOfCharsFilled++] = ',';
				currentString[numOfCharsFilled++] = '2';
				currentString[numOfCharsFilled++] = ' ';
				for (int i = 0; i < temp.getName().length(); i++)
				{
					currentString[numOfCharsFilled++] = temp.getName().charAt(i);
				}
				currentString[numOfCharsFilled++] = ',';
				
				lastActiveDocument = currentDocument.getKey();
			} // else we stitch the termLists together
			else
			{
				if (numOfCharsFilled == 0)
				{
					currentString[numOfCharsFilled++] = ',';
				}
				else if (currentString[numOfCharsFilled-1] != ',')
				{
					currentString[numOfCharsFilled++] = ',';
				}
			}

			
			
			termIt = currentDocument.getValue().getDocTermEntries().entrySet().iterator();
			while (termIt.hasNext()) // for every termEntry in the current documentTerm list:
			{
				while(numOfCharsFilled < BUFFER_SIZE_IN_BYTES - 30 && termIt.hasNext()) 
				{
					entry = termIt.next();
					
					char[] termID_Chars = String.valueOf(entry.getKey()+IIndexer.META_FIELD_COUNT).toCharArray(); // offset termIDs by #attributes before termAttributes
					
					char[] idf_Chars = String.valueOf(entry.getValue()).toCharArray();
					
					for (int i = 0; i < termID_Chars.length; i++)
					{
						currentString[numOfCharsFilled++] = termID_Chars[i];
					}
					
					currentString[numOfCharsFilled++] = ' ';
					
					for (int i = 0; i < idf_Chars.length; i++)
					{
						currentString[numOfCharsFilled++] = idf_Chars[i];
					}
					
					currentString[numOfCharsFilled++] = ',';
				}
				
				
				if (!termIt.hasNext())
				{
					numOfCharsFilled--; // no comma at the end of the list
				}
				
				
				out.write(currentString, 0, numOfCharsFilled);
				numOfCharsFilled = 0;
				
				/*
				if (!termIt.hasNext())
				{
					currentString[0] = '\n';
					numOfCharsFilled = 1;
					break;
				}
				*/			
			}
			lastActiveDocument = currentDocument.getKey();
		}
	}
	
	public void close() throws IOException
	{
		out.write('}'); // we need to manually close the last line
		if (out != null)
			out.close();
	}
}
