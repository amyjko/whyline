package edu.cmu.hcii.whyline.trace;

import edu.cmu.hcii.whyline.qa.Answer;
import edu.cmu.hcii.whyline.qa.Question;
import edu.cmu.hcii.whyline.util.Named;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class Value implements Named {

	protected final Trace trace;
	
	public Value(Trace trace) {
		
		this.trace = trace;
		
	}
	
	public Trace getTrace() { return trace; }

	public abstract Object getValue() throws NoValueException;
	
	public abstract boolean getBoolean() throws NoValueException;
	public abstract int getInteger() throws NoValueException; 
	public abstract long getLong();
	public abstract float getFloat() throws NoValueException;
	public abstract double getDouble() throws NoValueException;

	public abstract Object getImmutable() throws NoValueException;

	public abstract boolean isObject();
	
	public abstract String getVerbalExplanation();

	public abstract String getDisplayName(boolean html);
	
	public abstract int getEventID();
	
	public String getDisplayName(boolean html, int limit) {
		
		return getDisplayName(html);
		
	}

	public abstract boolean hasEventID();

	public abstract  Answer getAnswer(Question<?> q);
	
}