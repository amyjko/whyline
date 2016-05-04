package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.io.IOEvent;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.util.Named;

public final class Scope implements Named {

	private final Trace trace;
	private final int inputEventID, outputEventID;
	
	public Scope(Trace trace, int inputID, int outputID) {
		
		this.trace = trace;
		this.inputEventID = inputID;
		this.outputEventID = outputID;
		
	}
	
	public IOEvent getInputEvent() { return trace.getIOHistory().getEventAtTime(inputEventID); }
	public IOEvent getOutputEvent() { return trace.getIOHistory().getEventAtTime(outputEventID); }
	
	public int getInputEventID() { return inputEventID; }
	public int getOutputEventID() { return outputEventID; }

	public String getDescription() {

		IOEvent eventAtInputTime = getInputEvent();
		
		return inputEventID == 0 ? 
				"the program started..." : 
				"" + (eventAtInputTime == null ? "event " + inputEventID : eventAtInputTime.getHTMLDescription()) + "...";
	
	}

	public String getDisplayName(boolean html, int lengthLimit) {
		
		return getDescription();
		
	} 

	public boolean isEndOfProgram() { return inputEventID == trace.getNumberOfEvents() - 1; }

	public boolean includesInclusive(int eventID) { return inputEventID <= eventID && outputEventID >= eventID; } 

	public boolean includesExclusive(int eventID) { return inputEventID < eventID && outputEventID > eventID; }
	
}
