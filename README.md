IR SS2013 Exercise 2 - Stefan Csizmazia - Group H
=================================
Contents
=================================
- Main file is "ir-exercise02-tu-vienna-2013-grp-h.jar".
- Indices can be found in folder ./output/stemming and ./output/no_stemming
- Query result files can be found in folder ./output
- queries.txt in the root folder contains the topics the user wants to search for and can be adjusted by simply adding/removing topics.

RUN CONFIGURATION
=================================

The parameters for the program are as follows:
 - Usage for building an index:
ir-exercise02-tu-vienna-2013-grp-h.jar index <collectionFilePath> <indexName> [<useStemming{"true"/"false"}> <lowThresh> <highThresh>]

 - Usage for querying an index:
ir-exercise02-tu-vienna-2013-grp-h retrieve <method{"cosine"/"dfr"}> <index{"large"/"medium"/"small"}> <collectionFilePath> <queryFile> [<useStemming{"true"/"false"}>]

Examples:
Example 1: How to perform searches on all query topics in "query.txt" by stemming the query and, using the "large", stemmed index, plus writing the output to files according to the naming scheme provided in the task description:
ir-exercise02-tu-vienna-2013-grp-h retrieve dfr large resources/20_newsgroups_subset.zip queries.txt true

Example 2: How to build a new index for any given dataset:
ir-exercise02-tu-vienna-2013-grp-h.jar index resources/20_newsgroups_subset.zip large true 1 1000

NOTES
=================================

Note that the retrieval output files will be named automatically and will be prefixed with the name of the index, which - in the case of index-files 'large', 'medium' and 'small' - will create output files as specified in the task description.

Please find the javaDoc for this application in ./doc/index.html

For this exercise i decided to use a variant of the DFR model, called DFRee ("DFR free from parameters").
My reasons for this decision were as follows
 - no additional data needed for retrieval compared to cosine similarity, thus full reuse of the index from exercise 1 is possible
 - simple implementation
 - results are good, though not perfect (e.g. for topic20 the model returns the actual query only on second place)
 
DFRee model uses the following parameters to compute a score for a document:
 - The term frequency of the term in the query: computed at retrieval time (when processing the query)
 - The term frequency of the term in the document: available in the index
 - The term frequency of the term in the collection: not available within the index structure of exercise 1
 - The total number of tokens in the collection: available in the index meta-data
 - The length of the document, i.e. number of words in the document: not really the number of words but the number of indexed terms in this document

Some notes on the term frequency of the term in the collection:
Since i did not have this data stored in my original index, i tried to fake it and used the document term frequency instead.
Interestingly enough, this seems to work out pretty well and the results differ only a small bit when changing this number (e.g. doc-term-freq times 10 or times 100).
That's why i decided not to extend my index with the collection term frequencies.    
