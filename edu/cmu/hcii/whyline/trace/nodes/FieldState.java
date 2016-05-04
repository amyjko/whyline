package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.bytecode.FieldInfo;
import edu.cmu.hcii.whyline.trace.Value;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class FieldState extends ReferenceState implements Comparable<FieldState> {

	private final FieldInfo field;

	private int definitionID;
	private Value currentValue = null;
	private long valueID = 0;
	
	public FieldState(Trace trace, long objectID, FieldInfo field) {

		super(trace, objectID);

		this.field = field;
		
	}
	
	public FieldInfo getField() { return field; }
	
	public String toString() {
		
		String name = field.getDisplayName(true, -1);
		String value;
		
		if(!isInstantiated())
			return "<html><strike>" + name + "</strike>";		
		
		// Hasn't been initialized yet
		if(isUpdated()) {
			// Default or wasn't recorded
			if(currentValue == null)
				value = "<b>" + field.getDefaultValue() + "</b>";
			else
				value = "<b>" + currentValue.getDisplayName(true) + "</b>";
		}
		else value = "...";
		
		return "<html>" + name + " = " + value; 
		
	}

	public int compareTo(FieldState o) {  return field.getName().compareTo(o.field.getName()); }

	public boolean isLeaf() { return isUpdated() ? getValueID() <= 0 : true; }

	private long getValueID() { 
		
		update();
		return valueID; 
		
	}

	public long getObjectIDForChildren() { return getValueID(); }
	
	protected boolean performUpdate() {
		
		long oldValueID = valueID;
		
		currentValue = null;
		valueID = -1;

		definitionID = trace.findFieldAssignmentBefore(field, objectID, currentEventID);
		if(definitionID > 0) {
			EventKind kind = trace.getKind(definitionID);
			if(kind == EventKind.PUTFIELD) {
				currentValue = trace.getDefinitionValueSet(definitionID);
				if(currentValue.isObject())
					valueID = currentValue.getLong();
			}
		}

		if(valueID != oldValueID)
			resetChildren();
		
		return true;
		
	}

	public int getAssociatedEventID() { 
	
		update();
		return definitionID; 
		
	}

	public String getAssociatedEventIDDescription() { return "value"; }

}