package dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Scanner;

/**
 * From Terrier's CollectionStatistics.java.
 * 
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - School of Computing Science
 * http://www.gla.ac.uk/
 * 
 */
public class CollectionStatistics
{
	/** The total number of documents in the collection.*/
	private int numberOfDocuments;
	
	/** The total number of tokens in the collection.*/
	private long numberOfTokens;

	/**
	 * The total number of unique terms in the collection.
	 * This corresponds to the number of entries in the lexicon.
	 */
	private int numberOfUniqueTerms;
	
	/**
	 * The average length of a document in the collection.
	 */
	private double averageDocumentLength;
	
	private File file;
	
	public CollectionStatistics(int _numberOfDocuments, long _numberOfTokens, int _numberOfUniqueTerms, double _averageDocumentLength)
	{
		this.numberOfDocuments = _numberOfDocuments;
		this.numberOfTokens = _numberOfTokens;
		this.numberOfUniqueTerms = _numberOfUniqueTerms;
		this.averageDocumentLength = _averageDocumentLength;
	}
	
	public CollectionStatistics()
	{
		this.numberOfDocuments = -1;
		this.numberOfTokens = -1;
		this.numberOfUniqueTerms = -1;
		this.averageDocumentLength = -1; 
	}
	
	public void setFile(File _file)
	{
		file = _file;
	}

	public int getNumberOfDocuments()
	{
		return numberOfDocuments;
	}

	public void setNumberOfDocuments(int numberOfDocuments)
	{
		this.numberOfDocuments = numberOfDocuments;
	}

	public long getNumberOfTokens()
	{
		return numberOfTokens;
	}

	public void setNumberOfTokens(long numberOfTokens)
	{
		this.numberOfTokens = numberOfTokens;
	}

	public int getNumberOfUniqueTerms()
	{
		return numberOfUniqueTerms;
	}

	public void setNumberOfUniqueTerms(int numberOfUniqueTerms)
	{
		this.numberOfUniqueTerms = numberOfUniqueTerms;
	}

	public double getAverageDocumentLength()
	{
		return averageDocumentLength;
	}

	public void setAverageDocumentLength(double averageDocumentLength)
	{
		this.averageDocumentLength = averageDocumentLength;
	}
	
	public void writeToFile()
	{
		writeToFile(file);
	}
	
	public File getFile()
	{
		return this.file;
	}
	
	public void writeToFile(File _file)
	{
		try
		{
			Writer out = new FileWriter(_file);
			
			out.write("% This is the CollectionStatistics File for the index with the same filename.\n");
			out.write("% This file was automatically generated during index creation. Do not modify!\n");
			out.write("numberOfDocuments="+numberOfDocuments+"\n");
			out.write("numberOfTokens="+numberOfTokens+"\n");
			out.write("numberOfUniqueTerms="+numberOfUniqueTerms+"\n");
			out.write("averageDocumentLength="+averageDocumentLength+"\n");
			
			out.close();
			System.out.println("Wrote StatisticsFile to "+_file.getPath()+".");
		}
		catch (IOException ioe)
		{
			System.err.println("Error writing the statistics file to "+_file.getPath()+"! -- Stack Trace follows");
			ioe.printStackTrace();
		}
	}

	public void readFromFile(File statisticsFile) throws IOException
	{
		Scanner sc = new Scanner(new FileInputStream(statisticsFile));
		String nextLine;
		
		String parameter;
		String value;
		int separationIndex;
		
		numberOfDocuments = -1;
		numberOfTokens = -1;
		numberOfUniqueTerms = -1;
		averageDocumentLength = -1.0;
		
		while (sc.hasNext())
		{
			nextLine = sc.nextLine();
			if (nextLine.length() > 0 && nextLine.charAt(0)=='%')
			{
				continue; // skip comments
			}
			
			separationIndex = nextLine.indexOf("=");
			if (separationIndex == -1)
			{
				continue;
			}
			parameter = nextLine.substring(0, separationIndex);
			value = nextLine.substring(separationIndex + 1);
			
			try
			{
				if (parameter.equals("numberOfDocuments"))
				{
					numberOfDocuments = Integer.parseInt(value);
				}
				else if (parameter.equals("numberOfTokens"))
				{
					numberOfTokens = Long.parseLong(value);
				}
				else if (parameter.equals("numberOfUniqueTerms"))
				{
					numberOfUniqueTerms = Integer.parseInt(value);
				}
				else if (parameter.equals("averageDocumentLength"))
				{
					averageDocumentLength = Double.parseDouble(value);
				}
				else 
				{
				}
			}
			catch (NumberFormatException nfe)
			{
				System.err.println("Illegal parameter value in statistics file "+statisticsFile.getName()+": \""+value+"\".");
			}
		}
	}
	
	
}
