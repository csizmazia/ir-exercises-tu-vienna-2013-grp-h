package retrieval;

public interface IScoringMethod {
	public String getRequiredIndexSuffix();
	
	public boolean requiresPlainTf();
	
	public double score(float tfQuery, float tfDocument, float tfCollection, long numberOfTokens, int documentLength);

	public double useVectorLenghts(double similarity, double queryVectorLength, double documentVectorLength);

	public boolean requiresVectorLengths();
}
