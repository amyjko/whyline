package edu.cmu.hcii.whyline.analysis;

import java.util.SortedSet;

import edu.cmu.hcii.whyline.source.Token;

/**
 * @author Andrew J. Ko
 *
 */
public interface SearchResultsInterface {

	public String getResultsDescription();
	public String getCurrentStatus();
	public SortedSet<Token> getResults();
	public boolean isDone();
	
}
