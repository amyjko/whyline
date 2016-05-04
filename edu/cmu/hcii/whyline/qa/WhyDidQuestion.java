package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.util.Named;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class WhyDidQuestion<T extends Named> extends Question<T> {

	public WhyDidQuestion(Asker asker, T subject, String descriptionOfEvent) {
		super(asker, subject, descriptionOfEvent);
	}

	public final boolean isPhrasedNegatively() { return false; }

}
