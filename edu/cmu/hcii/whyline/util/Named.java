package edu.cmu.hcii.whyline.util;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public interface Named {

	/**
	 * Can optionally return strings with html tags. A length limit of < 0 should return the full length string.
	 * If the limit > 0, the limit should be used as a guideline, not as a literal length.
	 * 
	 */
	public String getDisplayName(boolean html, int lengthLimit);
	
}