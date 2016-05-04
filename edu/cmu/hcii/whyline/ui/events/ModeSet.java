package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.WhylineUI.Mode;

/**
 * @author Andrew J. Ko
 *
 */
public class ModeSet extends AbstractUIEvent<WhylineUI.Mode> {
	
	public ModeSet(WhylineUI.Mode mode) {

		super(mode, null, false);
		
	}

	public ModeSet(Trace trace, String[] args) {

		super(trace, args);

	}

	protected String getParsableStringArguments() { return entity.name(); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.MODE; }

	protected Mode parseEntity(String[] args) {
		
		return WhylineUI.Mode.valueOf(args[FIRST_ARGUMENT_INDEX]);
		
	}
	
}
