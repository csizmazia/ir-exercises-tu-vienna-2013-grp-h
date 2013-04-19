package indexing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.PriorityQueue;
import java.util.zip.ZipException;

import dao.CollectionStatistics;
import dao.Document;
import dao.DocumentTermList;
import dao.DocumentTermMatrix;
import dao.DocumentTermMatrixFileEntry;
import dao.Token;
import dao.Vocabulary;
import dao.VocabularyEntry;

/**
 * Class to perform SPIMI indexing of a set of files (
 * {@link #indexFiles(File[], File, boolean)}) or a single zipped file (
 * {@link #indexZipFile(File, File, boolean)}). The resulting index will be
 * written to an output file in the WEKA arff file format. Tokenizers, Parsers
 * and Writers all try to ensure memory constraints, which should allow this
 * indexer to perform indexing on arbitrarily sized datasets (given enough disk
 * space).
 * 
 */
public class BlockIndexer implements IIndexer
{
	// TODO kind of ugly since we don't know the size a String will consume in
	// the memory.
	// We will approximate it by s.length * 2Bytes unless you know a better
	// method? Also we will disregard
	// class overheads and somesuch.
	public static final int BLOCK_SIZE_IN_BYTES = 10 * 1024 * 1024; // the
																	// amount of
																	// free
																	// space we
																	// use for
																	// one block
																	// (here: 10
																	// MB).
	public static final int NUMBER_OF_ENTRIES_TO_WRITE_IN_ONE_BLOCK = 10 * 1024;

	// private Dictionary mergedDictionary;

	private boolean useStemming = false;
	private int lowTfThreshold = 0;
	private int highTfThreshold = Integer.MAX_VALUE;

	@Override
	public void setUseStemming(boolean _useStemming)
	{
		this.useStemming = _useStemming;

	}

	@Override
	public void setLowTfThreshold(int _lowThresh)
	{
		this.lowTfThreshold = _lowThresh;
	}

	@Override
	public void setHighTfThreshold(int _highThresh)
	{
		this.highTfThreshold = _highThresh;
	}

	@Override
	public void indexFiles(File[] files, File outputFile)
	{
		System.err.println("Sorry, this feature is not implemented yet!");
		return;
	}

	@Override
	public void indexZipFile(File zipFile, String indexName)
	{
		try
		{
			ZipTokenStream zipTokenStream = new ZipTokenStream(zipFile, useStemming);
			zipTokenStream.initialize();

			System.out.println("Running...");

			System.out.print("Building Vocabulary... ");

			String statisticsFileName = indexName + ".stat";
			String directory = "output"+java.io.File.separator;
			
			if (useStemming)
			{
				directory += "stemming"+File.separator;
			}
			else
			{
				directory += "no_stemming"+File.separator;
			}
			
			File statisticsFile = new File(directory + statisticsFileName);
			
			CollectionStatistics collectionStatistics = new CollectionStatistics();
			collectionStatistics.setFile(statisticsFile);
			
			Vocabulary vocabulary = buildVocabulary(zipTokenStream, collectionStatistics);
			System.out.print(" Found " + vocabulary.size() + " terms. ");
			// System.gc();

			zipTokenStream = new ZipTokenStream(zipFile, useStemming); // rewind
																		// token
																		// stream
			zipTokenStream.initialize();

			System.out.println("Done. ");

			buildDocumentTermMatrix(zipTokenStream, vocabulary, directory, indexName, collectionStatistics);

			// System.out.println("Building index... ");
			// buildIndex(zipTokenStream, vocabulary, outputFile);

			/*
			 * while(zipTokenStream.hasNext()) { Token newToken =
			 * zipTokenStream.next();
			 * System.out.println("Token, ID = "+newToken.
			 * getDocID()+", term = "+newToken.getTerm()); }
			 */
			System.out.println("Done building index, files were saved to " + directory);
		}
		catch (ZipException ze)
		{
			ze.printStackTrace();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}

	}

	private Vocabulary buildVocabulary(ITokenStream<Token> tokenStream, CollectionStatistics collectionStatistics)
	{
		Vocabulary vocabulary = new Vocabulary(65536); // will grow as needed
		Token next = null;
		
		long totalNumberOfTokens = 0;

		while (tokenStream.hasNext())
		{
			next = tokenStream.next();
			totalNumberOfTokens++;
			
			/*
			 * System.out.println(next.getDoc().getCategory()+"/"+next.getDoc().
			 * getName()+": "+next.getTerm()); if
			 * (next.getDoc().getName().equals("54457")) { System.out.println();
			 * }
			 */
			vocabulary.add(next.getTerm(), next.getDoc().getId());
		}
		collectionStatistics.setNumberOfDocuments(next.getDoc().getId());
		collectionStatistics.setNumberOfTokens(totalNumberOfTokens);
		collectionStatistics.setNumberOfUniqueTerms(vocabulary.getVocabulary().size());
		collectionStatistics.setAverageDocumentLength(totalNumberOfTokens / next.getDoc().getId());
		

		vocabulary.setTotalNumberOfDocuments(next.getDoc().getId()); 

		vocabulary.finalize();
		

		return vocabulary;
	}

	private void buildDocumentTermMatrix(ITokenStream<Token> tokenStream, Vocabulary vocabulary, String directory, String indexName, CollectionStatistics collectionStatistics)
	{
		Token currentToken;
		File blockOutputFile;
		DocumentTermMatrix blockMatrix = null;
		DocumentTermList currentTermList = new DocumentTermList();
		int numberOfBlock = 0;

		ArrayList<File> blockMatrixFiles = new ArrayList<File>();
		LinkedHashMap<Integer, Document> documents = new LinkedHashMap<Integer, Document>();

		Document lastDocument = null;
		int termID = -1;

		while (tokenStream.hasNext())
		{
			// int accumulatedSize = 0;

			blockMatrix = new DocumentTermMatrix();
			blockOutputFile = new File("output/tmp/" + numberOfBlock + ".mat");
			blockMatrixFiles.add(blockOutputFile);

			long runningMemoryCounter = 0;

			System.out.print("Tokenizing block dictionary #" + numberOfBlock + "... ");
			currentTermList.clear();

			Token current = tokenStream.current();
			if (lastDocument != null && lastDocument.getId() == current.getDoc().getId()) // the
																							// previous
																							// entry
																							// was
																							// not
																							// completed,
																							// keep
																							// writing:
			{
				documents.put(lastDocument.getId(), lastDocument);
				currentTermList = blockMatrix.addDoc(lastDocument.getId());
			}

			VocabularyEntry tempEntry;
			// Runtime.getRuntime().totalMemory()-initial_used_memory <
			// BLOCK_SIZE_IN_BYTES &&
			while (tokenStream.hasNext() && runningMemoryCounter < BLOCK_SIZE_IN_BYTES) // 3
																						// while
																						// (free
																						// memory
																						// available)
			{
				currentToken = tokenStream.next(); // 4 do token =
													// next(token_stream)
				Document currentDocument = currentToken.getDoc();
				if (currentDocument != lastDocument)
				{
					documents.put(currentDocument.getId(), currentDocument);
					lastDocument = currentDocument;
					currentTermList = blockMatrix.addDoc(currentDocument.getId());
				}
				else
				{
				}

				tempEntry = vocabulary.get(currentToken.getTerm());

				termID = tempEntry.getTermID();

				currentTermList.add(termID);

				runningMemoryCounter += 16; // TODO make some experiments to
											// estimate a good approximation
			}

			/*
			 * Iterator<Entry<Integer, DocumentTermList>> it =
			 * blockMatrix.getMatrix().entrySet().iterator(); Entry<Integer,
			 * DocumentTermList> currentDocument;
			 * 
			 * Iterator<Entry<Integer, Float>> termIt = null; Entry<Integer,
			 * Float> entry;
			 * 
			 * while (it.hasNext()) // for every documentTerm list: {
			 * currentDocument = it.next();
			 * System.out.println("DocumentMatrix for document "
			 * +currentDocument.getKey());
			 * 
			 * termIt =
			 * currentDocument.getValue().getDocTermEntries().entrySet()
			 * .iterator(); while (termIt.hasNext()) // for every termEntry in
			 * the current documentTerm list: { entry = termIt.next();
			 * System.out.print("<" + entry.getKey() + ":" + entry.getValue() +
			 * ">"); } System.out.println(); }
			 */

			// One Block Matrix is full, we need to sort it and write it to
			// disk:

			blockMatrix.sortTermListingsByID();

			try
			{
				System.out.print("Writing to " + blockOutputFile.getPath() + "... ");

				BufferedBlockMatrixWriter bbmw = new BufferedBlockMatrixWriter(blockOutputFile, blockMatrix);
				bbmw.writeToFile();

				bbmw = null;
				blockMatrix = null;
				System.gc();

				// blockDictionary.writeToDisk(blockOutputFile, 0,
				// Integer.MAX_VALUE);
				System.out.println("Done.");
			}
			catch (IOException e)
			{
				System.err.println(); // to end any previous output line
				System.err.println("Error writing block dictionary #" + numberOfBlock + " to " + blockOutputFile.getPath() + ". Stack Trace follows---");
				e.printStackTrace();
			}

			numberOfBlock++;
		}

		tokenStream = null;
		System.gc();

		// we are done writing block dictionaries. We now have to merge them
		// into a large dictionary and write the resulting dict to the
		// file outputFile:
		System.out.print("Done indexing block dictionaries, now merging files... ");

		mergeMatrixFiles(blockMatrixFiles, directory, indexName, vocabulary, documents, collectionStatistics);
	}

	private void mergeMatrixFiles(ArrayList<File> blockMatrixFiles, String directory, String indexName, Vocabulary vocabulary, LinkedHashMap<Integer, Document> documents, CollectionStatistics collectionStatistics)
	{
		PriorityQueue<PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry>> queue = new PriorityQueue<PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry>>(blockMatrixFiles.size(), new PriorityQueueTermReaderComparator<DocumentTermMatrixFileEntry>());

		File idfIndex = new File(directory+indexName+"_tfidf.arff.gz");
		File tfIndex = new File(directory+indexName+"_tf.arff.gz");
		
		BufferedArffMatrixWriter idfArffWriter = new BufferedArffMatrixWriter(idfIndex, vocabulary, documents);
		BufferedArffMatrixWriter tfArffWriter = new BufferedArffMatrixWriter(tfIndex, vocabulary, documents);

		try
		{
			System.out.print("Writing ARFF Header and Vocabulary... ");
			idfArffWriter.init(); // open File and write Header + vocabulary
			tfArffWriter.init();
			System.out.println("Done.");
		}
		catch (IOException e1)
		{
			System.err.println("Error accessing files " + idfIndex.getPath() + ", "+tfIndex.getPath()+"! -- Stack Trace follows.");
			e1.printStackTrace();
			return;
		}

		int thresholdedItemsCounter = 0;
		int docCounter = 0;

		System.out.print("Writing ARFF Data... ");

		DocumentTermMatrix tempMatrix;
		DocumentTermList currentTermList;

		for (int i = 0; i < blockMatrixFiles.size(); i++)
		{
			try
			{
				PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry> newReader = new PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry>();
				newReader.number = i;

				BufferedBlockMatrixReader reader = new BufferedBlockMatrixReader(blockMatrixFiles.get(i));

				if (!reader.hasNext()) // should not happen, just here for
										// reference
				{
					continue;
				}

				newReader.reader = reader;

				queue.add(newReader); // entries are ordered by term first and
										// then by their number

				/*
				 * while(bbdr.hasNext()) { PostingsListEntry newEntry =
				 * bbdr.next(); }
				 * System.out.println("Processed "+bbdr.getTermCounter
				 * ()+" terms.");
				 */

			}
			catch (IOException e)
			{
				System.err.println(); // to end any previous output line
				System.err.println("IOError merging the matrix. Stack Trace follows---");
				e.printStackTrace();
			}
		}

		ArrayList<PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry>> activeReaders = new ArrayList<PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry>>();
		PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry> currentToken;
		PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry> peekToken;

		tempMatrix = new DocumentTermMatrix();
		int entryCounter = 0;

		while (!queue.isEmpty())
		{
			activeReaders.clear();

			currentToken = queue.poll();
			peekToken = queue.peek();

			activeReaders.add(currentToken);

			int newDoc = currentToken.reader.current().docID;


			currentTermList = tempMatrix.addDoc(newDoc);
			
			/*
			Iterator<Map.Entry<Integer, DocumentTermList>> ititit = tempMatrix.getMatrix().entrySet().iterator();
			while (ititit.hasNext())
			{
				Map.Entry<Integer, DocumentTermList> en = ititit.next();
				System.out.println("docID: "+en.getKey()+", numEntries = "+en.getValue().getDocTermEntries().size());
			}
			*/
			

			

			docCounter++;
			

			while (!queue.isEmpty() && newDoc == peekToken.reader.current().docID)
			{
				activeReaders.add(queue.poll());
				peekToken = queue.peek();
			}

			DocumentTermMatrixFileEntry minimumEntry = activeReaders.get(0).reader.current();
			int mergingDocID = minimumEntry.docID;
			DocumentTermMatrixFileEntry testEntry;
			int minReader = 0;

			if (activeReaders.size() > 1) // we need to merge stuff
			{
				while (activeReaders.size() > 0)
				{
					while (activeReaders.size() > 0 && entryCounter < NUMBER_OF_ENTRIES_TO_WRITE_IN_ONE_BLOCK)
					{
						// try all readers to find the minimum token:
						for (int i = 0; i < activeReaders.size(); i++)
						{
							PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry> curReader = activeReaders.get(i);

							testEntry = activeReaders.get(i).reader.current();

							if (testEntry.compareTo(minimumEntry) < 0)
							{
								minimumEntry = testEntry;
								minReader = i;
							}
						}
						// we add the minimum entry to the list:
						minimumEntry = activeReaders.get(minReader).reader.next(); // ==
																					// minimumEntry,
																					// but
																					// we
																					// advance
																					// the
																					// stream
																					// to
																					// the
																					// next
																					// token
						currentTermList.add(minimumEntry.termID, minimumEntry.value); // this
																						// increases
																						// the
																						// Tf
																						// by
																						// minimumEntry.value
						entryCounter++;

						// find next candidate:
						for (int i = 0; i < activeReaders.size(); i++)
						{
							if (activeReaders.get(i).reader.hasNext() && activeReaders.get(i).reader.current().docID == mergingDocID)
							{
								minimumEntry = activeReaders.get(i).reader.current();
								minReader = i;
							}
							else
							// reader has provided all terms for the active
							// document. Remove it from the merging reader array
							// and re-add it to the priority queue.
							{
								PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry> old = activeReaders.remove(i);
								if (old.reader.hasNext())
								{
									PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry> newReader = new PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry>();
									newReader.number = old.number;
									newReader.reader = old.reader;

									queue.add(newReader);
								}
								continue;
							}
						}
					}
					
					// at this point we either have (1) accumulated enough entries to write them to the output file
					// or (2) are done merging. If (1), we write to the output file and continue merging. If (2), 
					// we continue by adding regular entries

					if (entryCounter > NUMBER_OF_ENTRIES_TO_WRITE_IN_ONE_BLOCK)
					{
						try
						{
							if (lowTfThreshold > 0 || highTfThreshold < Integer.MAX_VALUE)
							{
								//System.out.print("Applying Tf thresholds... ");
								thresholdedItemsCounter += tempMatrix.applyTfThresholds(lowTfThreshold, highTfThreshold);
							}
							//System.out.print("Calculating idfs... ");
							tfArffWriter.writeNextPart(tempMatrix);
							tempMatrix.calculateIdfs(vocabulary);
							idfArffWriter.writeNextPart(tempMatrix);
							tempMatrix.clear();
							entryCounter = 0;
							if (activeReaders.size() > 0)
							{
								currentTermList = tempMatrix.addDoc(newDoc); 
							}
						}
						catch (IOException e)
						{
							System.err.println("Error writing to file " + idfIndex.getPath() + "! -- Stack Trace follows.");
							e.printStackTrace();
							try
							{
								tfArffWriter.close();
								idfArffWriter.close();
							}
							catch (IOException e1)
							{
							}
							return;
						}
						//currentTermList = tempMatrix.addDoc(newDoc); 
						continue; // continue merging
					}
					else
					{
						break; // entryCounter !> NUMBER_OF_ENTRIES_TO_WRITE_IN_ONE_BLOCK, therefore we are here because #activeReaders == 0. 
						// we break the merging loop(s) and continue the main loop. 
					}
					
					// we write the next entryCounter entries to the file
					/*
					
					*/
				}
				continue; // we need to restart the process to obtain active readers
			}
			else
			// no merging, simply write out the data
			{
				//System.out.print("");
			}

			// merge the postings of all active readers:
			for (int i = 0; i < activeReaders.size(); i++)
			{
				PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry> curReader = activeReaders.get(i);

				while (curReader.reader.hasNext() && curReader.reader.current().docID == newDoc)
				{
					DocumentTermMatrixFileEntry nextEntry = curReader.reader.next();

					currentTermList.add(nextEntry.termID, nextEntry.value);
					entryCounter++;
					
					
					// System.out.println("Adding <"+nextEntry.currentDocID+":"+nextEntry.currentTermFrequency+">");

					if (entryCounter > NUMBER_OF_ENTRIES_TO_WRITE_IN_ONE_BLOCK)
					{
						try
						{
							if (lowTfThreshold > 0 || highTfThreshold < Integer.MAX_VALUE)
							{
								//System.out.print("Applying Tf thresholds... ");
								thresholdedItemsCounter += tempMatrix.applyTfThresholds(lowTfThreshold, highTfThreshold);
							}
							//System.out.print("Calculating idfs... ");
							tfArffWriter.writeNextPart(tempMatrix);
							tempMatrix.calculateIdfs(vocabulary);
							idfArffWriter.writeNextPart(tempMatrix);
							tempMatrix.clear();
							entryCounter = 0;
							if (curReader.reader.hasNext() && curReader.reader.current().docID == newDoc)
							{
								currentTermList = tempMatrix.addDoc(newDoc); 
							}
						}
						catch (IOException e)
						{
							System.err.println("Error writing to file " + idfIndex.getPath() + "! -- Stack Trace follows.");
							e.printStackTrace();
							try
							{
								idfArffWriter.close();
							}
							catch (IOException e1)
							{
							}
							return;
						}
					}
				}

				

			}

			// re-add the used readers to the priority queue:
			for (int i = 0; i < activeReaders.size(); i++)
			{
				PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry> cur = activeReaders.get(i);

				if (cur.reader.hasNext())
				{
					PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry> newReader = new PriorityQueueTermReaderToken<DocumentTermMatrixFileEntry>();
					newReader.number = cur.number;
					newReader.reader = cur.reader;

					queue.add(newReader);
				}
			}
		}
		
		//System.out.println("Writing last part... ");
		try
		{
			if (lowTfThreshold > 0 || highTfThreshold < Integer.MAX_VALUE)
			{
				//System.out.print("Applying Tf thresholds... ");
				thresholdedItemsCounter += tempMatrix.applyTfThresholds(lowTfThreshold, highTfThreshold);
			}
			//System.out.print("Calculating idfs... ");
			tfArffWriter.writeNextPart(tempMatrix);
			tempMatrix.calculateIdfs(vocabulary);
			idfArffWriter.writeNextPart(tempMatrix);
			tempMatrix.clear();
		}
		catch (IOException e)
		{
			System.err.println("Error writing to file " + idfIndex.getPath() + "! -- Stack Trace follows.");
			e.printStackTrace();
			try
			{
				tfArffWriter.close();
				idfArffWriter.close();
			}
			catch (IOException e1)
			{
			}
			return;
		}

		try
		{
			tfArffWriter.close();
			idfArffWriter.close();
		}
		catch (IOException e1)
		{
			System.err.println("Error closing file " + idfIndex.getPath() + "! -- Stack Trace follows.");
			e1.printStackTrace();
			return;
		}
		System.out.print("Removed "+thresholdedItemsCounter+" items due to thresholding. ");
		System.out.println("Done.");
		
		collectionStatistics.setNumberOfTokens(collectionStatistics.getNumberOfTokens() - thresholdedItemsCounter);
		collectionStatistics.setAverageDocumentLength(collectionStatistics.getNumberOfTokens() / collectionStatistics.getNumberOfDocuments());
		collectionStatistics.writeToFile();
		
		// System.out.println("Merged Dictionary contains a total of " +
		// docCounter + " documents.");
	}

	/*
	 * private void writeDictionaryToArff(File outputFile) throws
	 * java.io.IOException { if(this.lowTfThreshold > 0 || this.highTfThreshold
	 * < Integer.MAX_VALUE) {
	 * this.mergedDictionary.applyTfThresholds(this.lowTfThreshold,
	 * this.highTfThreshold); }
	 * this.mergedDictionary.calculateIdfs(this.documents.size());
	 * 
	 * System.out.println(Calendar.getInstance().get(Calendar.HOUR)+":"+Calendar.
	 * getInstance
	 * ().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar
	 * .SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
	 * 
	 * Integer numberOfAttributes = this.mergedDictionary.getNumberOfTerms()+1;
	 * FastVector attributes = new FastVector(numberOfAttributes);
	 * attributes.addElement(new Attribute("@@class@@",(FastVector) null));
	 * for(String term : this.mergedDictionary.getTerms()) {
	 * attributes.addElement(new Attribute(term)); }
	 * 
	 * 
	 * ArffSaver saver = new ArffSaver(); //saver.setCompressOutput(true);
	 * saver.setFile(outputFile); Instances dataSet = new
	 * Instances("ArbesserCsizmaziaIndex",attributes,0);
	 * 
	 * saver.setInstances(dataSet); //int docCounter = 0; for(Document doc :
	 * this.documents) { SparseInstance documentInstance = new
	 * SparseInstance(numberOfAttributes); documentInstance.setDataset(dataSet);
	 * documentInstance.setValue(0,doc.getCategory()+"/"+doc.getName()); for(int
	 * termCounter = 1;termCounter<numberOfAttributes;termCounter++) { double tf
	 * = Math.log10(1+this.mergedDictionary.getTf(termCounter-1,doc.getId()));
	 * double tfIdf = tf*this.mergedDictionary.getIdf(termCounter-1);
	 * documentInstance.setValue(termCounter, tfIdf);
	 * //documentInstance.setValue(termCounter, termCounter); }
	 * dataSet.add(documentInstance); } saver.writeBatch();
	 * 
	 * System.out.println(Calendar.getInstance().get(Calendar.HOUR)+":"+Calendar.
	 * getInstance
	 * ().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar
	 * .SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND)); }
	 */
}
