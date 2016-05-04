package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.trace.Value;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ArrayElementState extends ReferenceState implements Comparable<ArrayElementState>  {

	private final ArrayElementSeriesState series;
	private final int index;
	private int definitionID;
	private long valueID = 0;
	private Value currentValue = null;

	public ArrayElementState(ArrayElementSeriesState series, int index) {
		
		super(series.getTrace(), series.getArrayID());

		this.series = series;
		this.index = index;
				
	}
	
	public long getObjectIDForChildren() { 
	
		if(isPrimitiveArray()) return 0;
		
		int assignmentID = trace.getArrayAssignmentHistory().getIndexAssignmentBefore(series.getArrayID(), index, currentEventID);		
		
		if(assignmentID < 0)
			return 0;

		Value value = trace.getDefinitionValueSet(assignmentID);
		
		if(value == null || value.getLong() == 0)
			return 0;
		
		return value.getLong();
		
	}

	public boolean isLeaf() { return getObjectIDForChildren() <= 0; }

	public boolean isPrimitiveArray() { return series.getType().getArrayElementClassname().isPrimitive(); }
	
	protected boolean performUpdate() { 
			
		long oldValueID = valueID;
		
		currentValue = null;
		valueID = -1;

		definitionID = trace.getArrayAssignmentHistory().getIndexAssignmentBefore(series.getArrayID(), index, currentEventID);		
		if(definitionID > 0) {
			EventKind kind = trace.getKind(definitionID);
			if(kind == EventKind.SETARRAY) {
				currentValue = trace.getDefinitionValueSet(definitionID);
			}
		}

		if(valueID != oldValueID)
			resetChildren();
		
		return true;
			
	}

	public int getAssociatedEventID() { return -1; }

	public String getAssociatedEventIDDescription() { return "value"; }

	public int compareTo(ArrayElementState o) { return index - o.index; }

	public String toString() { 
		
		String name = "[" + index + "]";

		if(!isInstantiated())
			return "<html><strike>" + name + "</strike>";		

		String value;
		
		// Hasn't been initialized yet
		if(isUpdated()) {
			// Default or wasn't recorded
			if(currentValue == null)
				value = "<b>" + "default" + "</b>";
			else
				value = "<b>" + currentValue.getDisplayName(true) + "</b>";
		}
		else value = "...";
		
		return "<html>" + name + " = " + value; 
		
	}

}
