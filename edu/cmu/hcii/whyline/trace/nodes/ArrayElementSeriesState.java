package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ArrayElementSeriesState extends ReferenceState  implements Comparable<ArrayElementSeriesState> {

	private final Trace trace;
	private final int first, last;
	private final long arrayID;
	private final QualifiedClassName type;
	
	public ArrayElementSeriesState(Trace trace, QualifiedClassName type, long arrayID, int startIndex, int endIndex) {
		
		super(trace, arrayID);

		this.arrayID = arrayID;
		this.trace = trace;
		this.type = type;
		this.first = startIndex;
		this.last = endIndex;

	}
	
	public long getObjectIDForChildren() { return 0; }

	public boolean isLeaf() { return false; }

	protected boolean performUpdate() { return false; }

	public int getAssociatedEventID() { return -1; }

	public String getAssociatedEventIDDescription() { return "value"; }

	protected void determineChildren() {

		for(int i = first; i <= last; i++)
			addChild(new ArrayElementState(this, i));
		
	}

	public String toString() { 
		
		return "[" + first + " - " + last + "]";
		
	}
		
	public int compareTo(ArrayElementSeriesState o) {
		return first - o.first;
	}

	public long getArrayID() { return arrayID; }

	public QualifiedClassName getType() { return type; }

}