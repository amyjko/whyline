package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.trace.Value;
import edu.cmu.hcii.whyline.trace.CallStack;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class IOEvent {

	protected final Trace trace;
	protected final int eventID;

	public IOEvent(Trace trace, int eventID) {
		
		this.trace = trace;
		this.eventID = eventID;
		
	}
	
	public Instruction getInstruction() { return trace.getInstruction(eventID); }

	public int getEventID() { return eventID; } 

	public Value getArgument(int argument) { return trace.getOperandStackValue(eventID, argument); }
	
	public abstract boolean segmentsOutput();

	public Trace getTrace() { return trace; }
	
	public CallStack getCallStack() { return trace.getCallStack(eventID); }

	public int getNumberOfArgumentProducers() { return trace.getInstruction(eventID).getNumberOfArgumentProducers(); }
	
	public abstract String getHTMLDescription();
	
}
