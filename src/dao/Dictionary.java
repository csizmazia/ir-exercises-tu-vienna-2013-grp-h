package dao;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dictionary
 *
 */
public class Dictionary implements IDictionary
{
	
	private Map<String, PostingsList> terms;
	private List<String> sorted_terms;
	private Map<String, Double> idfs;
	
	public Dictionary()
	{
		super();
		this.terms = new HashMap<String, PostingsList>();
		this.sorted_terms = null;
		this.idfs = new HashMap<String,Double>();
	}

	@Override
	public PostingsList addTerm(String new_term)
	{
		PostingsList postings_list;
		if(this.terms.containsKey(new_term)) {
			postings_list = this.terms.get(new_term);
			//this.term_frequencies.put(new_term, this.term_frequencies.get(new_term)+1);
		}
		else {
			postings_list = new PostingsList();
			this.terms.put(new_term, postings_list);
			//this.term_frequencies.put(new_term, 0);
		}
		
		return postings_list;
	}

	@Override
	public PostingsList getPostingsList(String term_in_dictionary)
	{
		if(this.terms.containsKey(term_in_dictionary)) {
			return this.terms.get(term_in_dictionary);
		}
		else {
			return null;
		}
	}


	@Override
	public void sortTerms()
	{
		this.sorted_terms = new ArrayList<String>(this.terms.keySet()); // TODO this doubles the amount of allocated memory! Maybe we can avoid this? At the very least, this needs to be documented and justified!!
		Collections.sort(this.sorted_terms);
	}

	@Override
	public void writeToDisk(File outputFile, int lowThresh, int highThresh) throws java.io.IOException
	{
		outputFile.createNewFile();
		BufferedWriter bfw = new BufferedWriter(new FileWriter(outputFile));
		
		int termCounter = 0;
		
		for (String term : this.sorted_terms) 
		{
			PostingsList pl = terms.get(term);
			if (pl.getPostings().size() > lowThresh && pl.getPostings().size() < highThresh)
			{
				bfw.write(term+"\t"+pl.toString()+"\n");
				termCounter++;
			}
			
		}
		System.out.print("Wrote "+termCounter+" of a total of "+sorted_terms.size()+" terms. ");
		bfw.close();
		//bfw.flush();
	}
	
	public int getNumberOfTerms() {
		return this.terms.size();
	}
	
	public List<String> getTerms() {
		if(this.sorted_terms == null) {
			sortTerms();
		}
		return this.sorted_terms;
	}
	
	public double getTf(int termIndex, Integer docID) {
		return this.terms.get(this.sorted_terms.get(termIndex)).getTermFrequency(docID);
	}
	
	public double getIdf(int termIndex) {
		return this.idfs.get(this.sorted_terms.get(termIndex));
	}
	
	public void calculateIdfs(int docCount) {
		if(this.sorted_terms == null) {
			sortTerms();
		}
		for(String term : this.sorted_terms) {
			this.idfs.put(term, Math.log10(docCount/this.terms.get(term).getDocumentFrequency()));
		}
	}

	public void applyTfThresholds(int lowTfThreshold, int highTfThreshold) {
		for(String term : this.terms.keySet()) {
			Integer tf = this.terms.get(term).getTermFrequency();
			if(tf < lowTfThreshold || tf > highTfThreshold) {
				this.terms.remove(term);
			}
		}
	}
}
