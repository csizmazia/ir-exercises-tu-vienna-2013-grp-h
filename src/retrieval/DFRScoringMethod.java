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
	 * DFR scoring for the given parameters.
	 * Scoring is done using the DFRee algorithm found in org.terrier.matching.models.DFRee 
	 * 
	 * @param tfQuery The term frequency of the term in the query
	 * @param tfDocument The term frequency of the term in the document
	 * @param tfCollection The term frequency of the term in the collection
	 * @param numberOfTokens The total number of tokens in the collection
	 * @param documentLength The length of the document, i.e. number of words in the document
	 * @return
	 */
	public double score(float tfQuery, float tfDocument, float tfCollection, long numberOfTokens, int documentLength) {
		tfQuery = (int)Math.floor(tfQuery);
		tfDocument = (int)Math.floor(tfDocument);
		
		// let's fake tfCollection. we don't have it in our index now and since this fake seems to work, why bother?
		tfCollection = tfDocument;
		
		double prior = tfDocument/documentLength;
		double posterior  = (tfDocument+1)/(documentLength+1);
		double InvPriorCollection = numberOfTokens/tfCollection;					
					
		double norm = tfDocument*Math.log(posterior/prior); 
		 
		return tfQuery * norm * (tfDocument * (Math.log (prior*InvPriorCollection)*(-1)) + (tfDocument+1) * (Math.log ( posterior*InvPriorCollection)) + 0.5*Math.log(posterior/prior));
	}

	@Override
	public double useVectorLenghts(double similarity, double queryVectorLength, double documentVectorLength) {
		return similarity;
	}
}
