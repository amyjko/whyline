package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.source.ParseException;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class UndoableUIEvent<T> extends AbstractUIEvent<T> {

	public UndoableUIEvent(T entity, String ui, boolean userInitiated) {
		super(entity, ui, userInitiated);
	}

	public UndoableUIEvent(Trace trace, String[] args) {
		super(trace, args);
	}

	public abstract void select(WhylineUI whylineUI);

	public abstract Line getCorrespondingLine(Trace trace) throws ParseException;

}
