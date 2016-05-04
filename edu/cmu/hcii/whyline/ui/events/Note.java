package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class Note extends AbstractUIEvent<String> {
	
	public Note(String note) {
		
		super(note, null, false);
		
	}

	public Note(Trace trace, String[] args) {
		
		super(trace, args);
		
	}

	protected String getParsableStringArguments() { return entity; }

	protected UIEventKind getParsableStringKind() { return UIEventKind.NOTE; }

	public void select(WhylineUI whylineUI) {}

	protected String parseEntity(String[] args) {
		
		return args[FIRST_ARGUMENT_INDEX];

	}
	
}
