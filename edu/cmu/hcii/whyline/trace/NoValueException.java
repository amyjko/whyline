package edu.cmu.hcii.whyline.trace;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class NoValueException extends Exception {

	private final Trace trace;
	private final int eventID;
	
	public NoValueException(Trace trace, int eventID, String message) {
		
		super(message);
		this.trace = trace;
		this.eventID = eventID;
		
	}
	
	public String getMessage() { 

		return super.getMessage() + ": \n\t" + trace.eventToString(eventID);	
		
	}
	
}
