package edu.cmu.hcii.whyline.trace;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class ExecutionEventParser {

	protected final Trace trace;
	
	public ExecutionEventParser(Trace trace) {
		
		this.trace = trace;
		
	}

	public Trace getTrace() { return trace; }
	
	public abstract boolean handle(int eventID);
	
}
