package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.qa.Question;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */
public class QuestionSelected extends AbstractUIEvent<Question<?>> {

	public QuestionSelected(Question<?> q, boolean userInitiated) {
		
		super(q, null, userInitiated);
		
	}

	public QuestionSelected(Trace trace, String[] args) {
		super(trace, args);
	}

	protected String getParsableStringArguments() { return "" + entity; }
	protected UIEventKind getParsableStringKind() { return UIEventKind.QUESTION_SELECTED; }

	protected Question<?> parseEntity(String[] args) { return null; }	
	
}
