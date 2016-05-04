package edu.cmu.hcii.whyline.trace;

import edu.cmu.hcii.whyline.qa.Answer;
import edu.cmu.hcii.whyline.qa.NoExplanationAnswer;
import edu.cmu.hcii.whyline.qa.Question;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class UnknownValue extends Value {

	private final int consumerID;
	private final Reason reason;
	
	public static enum Reason {

		JSR_ARGUMENT("Value is address pushed by JSR"),
		THIS_NOT_RECORDED("Couldn't find set argument event for 'this' , or the argument passed in."),
		PLACEHOLDER_WITH_NO_VALUE("Found placeholder event but it didn't have a value for some reason."),
		PLACEHOLDER_WITH_NO_CORRESPONDING("Found placeholder event  but didn't find corresponding instantiation."),
		NO_PLACEHOLDER("Couldn't find placeholder for instantion: "),
		NO_INVOCATION_INSTANCE("Found the invocation, but didn't find the producer of the first argument."),
		UNKNOWN("Don't know a reason whyline I couldn't explain."),
		NO_EXCEPTION_SOURCE("Couldn't find source of exception."),
		NO_INCREMENT("Couldn't find increment value.")
		;
		
		private final String reason;
		
		private Reason(String reason) { this.reason = reason; }
		
	}
		
	public UnknownValue(Trace trace, int consumerID, Reason reason) {

		super(trace);
		
		this.consumerID = consumerID;
		this.reason = reason;
	
	}

	public boolean getBoolean() throws NoValueException { throw new NoValueException(trace, consumerID, ""); }
	public int getInteger() throws NoValueException { throw new NoValueException(trace, consumerID, ""); }
	public long getLong() { return -1; }
	public float getFloat() throws NoValueException { throw new NoValueException(trace, consumerID, ""); }
	public double getDouble() throws NoValueException { throw new NoValueException(trace, consumerID, ""); }

	public Object getValue() throws NoValueException { throw new NoValueException(trace, consumerID, ""); }

	public Object getImmutable() throws NoValueException { throw new NoValueException(trace, consumerID, ""); }

	public boolean isObject() { return false; }

	public String getVerbalExplanation() { return "<b>[unknown]</b>"; }
	
	public String getDisplayName(boolean html) { return "[unknown]"; }

	public String getReasonNotFound() { return reason.reason; }
	
	public int getEventID() { return -1; }

	public boolean hasEventID() { return false; }

	public String toString() { return "UnknownStackValue " + reason; }

	public Answer getAnswer(Question<?> q) {
		
		return new NoExplanationAnswer(q, null);
	
	}
	
}
