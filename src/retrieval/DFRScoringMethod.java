package retrieval;

public class DFRScoringMethod implements IScoringMethod {
	private static final String REQUIRED_INDEX_SUFFIX = "_tf";
	
	public String getRequiredIndexSuffix() {
		return REQUIRED_INDEX_SUFFIX;
	}
	
	@Override
	public boolean requiresPlainTf() {
		return true;
	}
	
	@Override
	public boolean requiresVectorLengths() {
		return false;
	}
	
	/**
	 * DFR weighting for the given parameters. 
	 * 
	 * @param tfQuery The term frequency of the term in the query
	 * @param tfDocument The term frequency of the term in the document
	 * @param tfCollection The term frequency of the term in the collection
	 * @param numberOfTokens The total number of tokens in the collection
	 * @param documentLength The length of the document, i.e. number of words in the document
	 * @return
	 */
	public double score(float tfQuery, float tfDocument, float tfCollection, long numberOfTokens, int documentLength) {
		return 0.0;
	}

	@Override
	public double useVectorLenghts(double similarity, double queryVectorLength, double documentVectorLength) {
		return similarity;
	}
}
