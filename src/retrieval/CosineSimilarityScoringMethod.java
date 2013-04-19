package retrieval;

public class CosineSimilarityScoringMethod implements IScoringMethod {
	private static final String REQUIRED_INDEX_SUFFIX = "_tfidf";
	
	public String getRequiredIndexSuffix() {
		return REQUIRED_INDEX_SUFFIX;
	}
	
	@Override
	public boolean requiresPlainTf() {
		return false;
	}
	
	@Override
	public boolean requiresVectorLengths() {
		return true;
	}
	
	/**
	 * Cosine similarity weighting for the given parameters. 
	 * 
	 * @param tfQuery The term frequency of the term in the query.
	 * @param tfIdf The tf-idf of {term,document}.
	 * @param tfCollection IGNORED
	 * @param numberOfTokens IGNORED
	 * @param documentLength IGNORED
	 * @return
	 */
	public double score(float tfQuery, float tfIdf, float tfCollection, long numberOfTokens, int documentLength) {
		return tfQuery * tfIdf;
	}
	
	public double useVectorLenghts(double similarity, double queryVectorLength, double documentVectorLength) {
		if (queryVectorLength == 0.0 || documentVectorLength == 0.0) { // avoid division by zero
			similarity = 0.0;
		}
		else {
			similarity = similarity/(queryVectorLength*documentVectorLength);
		}
		
		return similarity;
	}
}
