package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.trace.Value;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class LocalState extends ReferenceState implements Comparable<LocalState> {

	private String name, value;
	private int definitionID = -1;
	private final int localID;
	
	public LocalState(Trace trace,  int localID) {
		
		super(trace, -1);
		
		this.localID = localID;
		this.name = "-";
		
	}
	
	public String toString() { 
	
		update();
		return "<html><b>" + name + "</b> = " + value + "</html>"; 
//		return name + " = " + value; 
		
	}

	public int compareTo(LocalState object) { return localID - object.localID; }

	public boolean isLeaf() {  return objectID <= 0; }

	public long getObjectIDForChildren() { return objectID; }

	protected boolean performUpdate() {

		int startID = trace.getStartID(currentEventID);
		int nextEventIDInThread = trace.getNextEventIDInThread(currentEventID);
		int eventIDToShow;
		if(trace.getStartID(nextEventIDInThread) == startID) eventIDToShow = nextEventIDInThread;
		else eventIDToShow = currentEventID;

		// Update the name
		Instruction instruction = trace.getInstruction(eventIDToShow);
		name = instruction.getCode().getLocalIDNameRelativeToInstruction(localID, instruction);

		objectID = 0;
		
		// Update the value
		if(trace.eventDefinesLocalID(currentEventID, localID))
			definitionID = currentEventID;
		else 
			definitionID = trace.findLocalIDAssignmentBefore(localID, currentEventID);
				
		if(definitionID < 0) 
			value = "<i>unknown</i>";
		else {
			EventKind kind = trace.getKind(definitionID);
			if(kind.isArgument) {
				value = trace.getArgumentValueDescription(definitionID);
				if(kind == EventKind.OBJECT_ARG)
					objectID = trace.getObjectIDProduced(definitionID);
			}
			else {
				Value valueSet = trace.getDefinitionValueSet(definitionID);
				if(valueSet == null) {
					value = "<i>unknown</i>";
				}
				else {
					value = valueSet.getDisplayName(true);
					if(valueSet.isObject())
						objectID = valueSet.getLong();
				}
			}				
		}
		
		resetChildren();
		
		return true;
		
	}

	public int getDefinitionID() { 
		
		update();
		return definitionID; 
		
	}

	public int getAssociatedEventID() { 
		
		update();
		return definitionID; 
		
	}

	public String getAssociatedEventIDDescription() { return "value"; }

}
