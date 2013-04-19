#####################################
IR SS2013 Exercise 2
Stefan Csizmazia
Group H
#####################################

#####################################
Contents
#####################################
- Main file is "ir-exercise02-tu-vienna-2013-grp-h.jar".
- Indices can be found in folder ./output/stemming and ./output/no_stemming
- Query result files can be found in folder ./output
- queries.txt in the root folder contains the topics the user wants to search for and can be adjusted by simply adding/removing topics.

#####################################
RUN CONFIGURATION
#####################################

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

#####################################
NOTES
#####################################

Note that the retrieval output files will be named automatically and will be prefixed with the name of the index, which - in the case of index-files 'large', 'medium' and 'small' - will create output files as specified in the task description.

Please find the javaDoc for this application in ./doc/index.html