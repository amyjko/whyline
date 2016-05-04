package edu.cmu.hcii.whyline.trace;

import edu.cmu.hcii.whyline.qa.Answer;
import edu.cmu.hcii.whyline.qa.CauseAnswer;
import edu.cmu.hcii.whyline.qa.Question;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class IncrementValue extends Value {

	private final int eventID;
	private final int value;
	
	public IncrementValue(Trace trace, int eventID, int value) {
		
		super(trace);
		
		this.eventID = eventID;
		this.value = value;
		
	}

	public int getInteger() { return value; }

	public boolean getBoolean() { throw new RuntimeException("Can't get boolean from IINC instruction."); }
	public long getLong() { throw new RuntimeException("Can't get boolean from IINC instruction."); }
	public float getFloat() { throw new RuntimeException("Can't get boolean from IINC instruction."); }
	public double getDouble() { throw new RuntimeException("Can't get boolean from IINC instruction."); }

	public Object getImmutable() { throw new RuntimeException("Can't get object from IINC instruction."); }
			
	public Integer getValue() { return new Integer(value); }
	
	public boolean isObject() { return false; }
	
	public String getVerbalExplanation() { 

		String valueText = "<b>" + getDisplayName(true) + "</b>";

		return valueText +" was incremented to " + value;
			
	}
	
	public String getDisplayName(boolean html) { 
		
		return Integer.toString(value);
		
	}

	public boolean hasEventID() { return true; }

	public int getEventID() { return eventID; }

	public Answer getAnswer(Question<?> q) {
		
		return new CauseAnswer(q, eventID, "These events were responsible for this increment.");

	}

}
