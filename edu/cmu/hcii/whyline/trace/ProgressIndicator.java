package edu.cmu.hcii.whyline.trace;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public interface ProgressIndicator {

	public void madeProgress(int progressMade, String string);
	
}
