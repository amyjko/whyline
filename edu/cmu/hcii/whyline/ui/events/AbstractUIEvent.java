package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.trace.Serializer;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class AbstractUIEvent<T> {

	protected static final int FIRST_ARGUMENT_INDEX = 3; 
	
	protected final Trace trace;
	protected final long time;
	protected final T entity;
	protected final String ui;
	protected final boolean userInitiated;
	
	public AbstractUIEvent(T entity, String ui, boolean userInitiated) {
		
		this.trace = null;
		this.entity = entity;
		this.time = System.currentTimeMillis();
		this.ui = ui;
		this.userInitiated = userInitiated;
		
	}

	/**
	 * Construct a ui event from a log.
	 */
	public AbstractUIEvent(Trace trace, String[] args) {

		this.trace = trace;
		this.time = Long.parseLong(args[1]);
		this.ui = args[2];
		this.entity = parseEntity(args);
		this.userInitiated = false;
		
	}

	protected abstract T parseEntity(String[] args);

	public long getTime() { return time; }
	
	public T getEntity() { return entity; }
	
	public boolean wasUserInitiated() { return userInitiated; }

	protected abstract String getParsableStringArguments();
	
	protected abstract UIEventKind getParsableStringKind();
	
	public final String getParsableString() {

		return Serializer.listToString(getParsableStringKind().getShortName(), Long.toString(time), ui, getParsableStringArguments());
		
	}

	public String getUI() { return ui; }
	
}
