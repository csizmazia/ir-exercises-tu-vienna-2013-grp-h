package main;

import java.io.File;
import java.io.IOException;

import retrieval.CosineSimilarityScoringMethod;
import retrieval.DFRScoringMethod;
import retrieval.IScoringMethod;
import retrieval.SearchEngine;

import indexing.*;

public class Main
{
	/**
	 * @param args
	 *            A String array of command line arguments.
	 * 
	 *            - args[0]: Action: "index" or "retrieve" 
	 *            
	 *            in case of action == "index": 
	 *            - args[1]: Path to the archive of documents to index (a .zip file) 
	 *            - args[2]: Name of the index to create 
	 *            - args[3]: "true" or "false" (defaults to "false"), whether or not to use Stemming 
	 *            - args[4]: Integer value (defaults to "0"), specifying the low threshold, i.e. the lowest allowed term frequency 
	 *            - args[5]: Integer value (defaults to "Integer.MAX_VALUE"), specifying the high threshold, i.e. the highest allowed term frequency 
	 *            
	 *            in case of action == "retrieve"
	 *            - args[1]: Scoring Method {"cosine", "dfr"} 
	 *            - args[2]: Name of the index to use {"large", "medium", "small"} 
	 *            - args[3]: Path to the collection file (a .zip file) 
	 *            - args[4]: name of the query file (contains names of the query documents within the zipped collection file) 
	 *            - args[5]: "true" or "false" (defaults to "false"), whether or not to use Stemming
	 * 
	 *            If the args array is too long and/or contains invalid arguments,
	 *            a usage message is shown and the program terminates.
	 */
	public static void main(String[] args) {

		try {
			if (args.length > 0) {
				if (args[0].equals("index")) {
					String collectionFilePath = "";
					String indexName = "";
					boolean useStemming = false;
					int lowThresh = 0;
					int highThresh = Integer.MAX_VALUE;

					switch (args.length) {
						case 3: // use all defaults
							collectionFilePath = args[1];
							indexName = args[2];
							break;
						case 4: // use default low/high thresholds
							collectionFilePath = args[1];
							indexName = args[2];
							useStemming = Boolean.parseBoolean(args[3]);
							break;
						case 6: // user specified low/high thresholds
							collectionFilePath = args[1];
							indexName = args[2];
							useStemming = Boolean.parseBoolean(args[3]);

							try {
								lowThresh = Integer.parseInt(args[4]);
								highThresh = Integer.parseInt(args[5]);

								if (!(lowThresh >= 0 && highThresh >= 0 && highThresh >= lowThresh)) {
									throw new IllegalArgumentException(
											"invalid arguments: lowThresh and highThresh have to be positive numbers and highTresh has to be greater than or equal lowThresh");
								}
							}
							catch (NumberFormatException nfe) {
								throw new IllegalArgumentException("invalid arguments: lowThresh and highThresh have to be numeric");
							}

							break;
						default:
							throw new IllegalArgumentException("invalid number of arguments");
					}

					BlockIndexer indexer = new BlockIndexer();
					indexer.setUseStemming(useStemming);
					indexer.setLowTfThreshold(lowThresh);
					indexer.setHighTfThreshold(highThresh);
					indexer.indexZipFile(new File(collectionFilePath), indexName);
				}
				else if (args[0].equals("retrieve")) {
					String indexName = "";
					String queryFile = "";
					String collectionFilePath = "";
					boolean useStemming = true;
					IScoringMethod method;

					String methodString = args[1];
					if (methodString.equals("cosine")) {
						method = new CosineSimilarityScoringMethod();
					}
					else if (methodString.equals("dfr")) {
						method = new DFRScoringMethod();
					}
					else {
						throw new IllegalArgumentException("Illegal method " + methodString + "!");
					}

					switch (args.length) {
						case 6: // user specified stemming
							indexName = args[2];
							collectionFilePath = args[3];
							queryFile = args[4];

							useStemming = Boolean.parseBoolean(args[5]);
							break;
						case 7: // user specified output file
							indexName = args[2];
							collectionFilePath = args[3];
							queryFile = args[4];
							useStemming = Boolean.parseBoolean(args[5]);
							break;
						default:
							throw new IllegalArgumentException("invalid number of arguments");
					}

					try {
						SearchEngine searchEngine = new SearchEngine(indexName, useStemming);
						searchEngine.setScoringMethod(method);
						searchEngine.retrieveAndWriteQueries(10, new File(collectionFilePath), new File(queryFile));
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					throw new IllegalArgumentException("wrong argument: action. must be either \"index\" or \"retrieve\"");
				}
			}
			else {
				throw new IllegalArgumentException("invalid number of arguments");
			}
		}
		catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
			System.out.println("Invalid command line arguments!");
			System.out.println(e.getMessage());
			System.out.println("Usage for building an index:");
			System.out.println("ir-exercise02-tu-vienna-2013-grp-h.jar index <collectionFilePath> <indexName> [<useStemming{\"true\"/\"false\"}> <lowThresh> <highThresh>]");
			System.out.println("Usage for querying an index:");
			System.out.println("ir-exercise02-tu-vienna-2013-grp-h retrieve <method{\"cosine\"/\"dfr\"}> <index{\"large\"/\"medium\"/\"small\"}> <collectionFilePath> <queryFile> [<useStemming{\"true\"/\"false\"}>]");
			System.out.println();
			System.out.println("Example 1: How to perform searches on all query topics in \"query.txt\" by stemming the query and, using the \"large\", stemmed index, plus writing the output to files according to the naming scheme provided in the task description:");
			System.out.println("ir-exercise02-tu-vienna-2013-grp-h retrieve dfr large resources/20_newsgroups_subset.zip queries.txt true");
			System.out.println();
			System.out.println("Example 2: How to build a new index for any given dataset:");
			System.out.println("ir-exercise02-tu-vienna-2013-grp-h index resources/20_newsgroups_subset.zip large true 1 1000");
		}
	}
}