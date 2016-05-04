package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class FrameState extends DynamicNode<LocalState> implements Comparable<FrameState> {

	private LocalState[] locals;
	
	private final Trace trace;
	private final MethodInfo method;
	private final int startID, invocationID;
	private int recentEventIDInCall;
	
	public FrameState(Trace trace, int startID, int invocationID) {
		
		this.trace = trace;
		this.startID = startID;
		this.invocationID = invocationID;
		this.recentEventIDInCall = startID;
		
		method = trace.getInstruction(startID).getMethod();

	}
	
	public int getInvocationID() { return invocationID; }

	public int compareTo(FrameState object) { return object.startID - startID; }

	public boolean isLeaf() { return false; }

	protected void determineChildren() {

		if(locals == null) {
			int numberOfLocals = method.getCode().getMaxLocals();
			locals = new LocalState[numberOfLocals];
			for(int localID = 0; localID < numberOfLocals; localID++)
				locals[localID] = new LocalState(trace, localID);
		}
		
		int nextEventIDInThread = trace.getNextEventIDInThread(recentEventIDInCall);
		int eventIDToShow;
		if(trace.getStartID(nextEventIDInThread) == startID) eventIDToShow = nextEventIDInThread;
		else eventIDToShow = recentEventIDInCall;

		Instruction instructionAfter = trace.getInstruction(eventIDToShow);
		int numberOfLocals = method.getCode().getMaxLocals();
		for(int localID = 0; localID < numberOfLocals; localID++) {
			if(!instructionAfter.getCode().hasLocalVariableInfo() || instructionAfter.getCode().localIDIsDefinedAt(localID, instructionAfter)) {
				addChild(locals[localID]);
				locals[localID].propagateCurrentEventID(eventIDToShow);
			}
		}

	}

	public void showEventID(int eventID) {

		recentEventIDInCall = 
			trace.getStartID(eventID) == startID ?
					eventID : invocationID;

		if(hasDeterminedChildren())
			resetChildren();
		
	}

	/**
	 * Returns the local state of the most recent local assignment at or prior to the given eventID.
	 * If this frame has yet to determine its children, returns nothing.
	 */
	public LocalState getLocalFor(int eventID) {

		if(hasDeterminedChildren()) {
			LocalState mostRecentlyAssignedLocal = null;
			for(LocalState local : getChildren()) {
				if(mostRecentlyAssignedLocal == null || local.getDefinitionID() > mostRecentlyAssignedLocal.getDefinitionID())
					mostRecentlyAssignedLocal = local;
			}
			return mostRecentlyAssignedLocal; 
		}
		return null;
		
	}

	public int getAssociatedEventID() { return startID; }

	public String getAssociatedEventIDDescription() { return "invocation"; }

	protected boolean performUpdate() { return false; }

	public String toString() { return "<html>" + method.getClassfile().getSimpleName() + " : <b>" + method.getJavaName() + "</b>()"; }

}
