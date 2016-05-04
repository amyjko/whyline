package edu.cmu.hcii.whyline.qa;

import gnu.trove.TLongHashSet;

public class ExpectedObject {

	private final TLongHashSet objectIDs;
	private final long objectID;	// Which object do we expect to find?
	private final int expectedArgument;	// Which argument do we expect to find it in?

	/**
	 * Expected the instruction to use a particular objectID for at least one of its arguments.
	 */
	public ExpectedObject(long objectID) {
		
		this.objectID = objectID;
		this.objectIDs = null;
		this.expectedArgument = -1;
		
	}
	
	/**
	 * We expect to find a particular object ID as a particular argument.
	 */
	public ExpectedObject(long objectID, int arg) {
		
		this.objectID = objectID;
		this.objectIDs = null;
		this.expectedArgument = arg;
		
	}

	/**
	 * We expect to find one or more of the given object IDs in any of the arguments.
	 */
	public ExpectedObject(TLongHashSet objectIDs) {

		this.objectID = -1;
		this.expectedArgument = -1;
		this.objectIDs = objectIDs;
		
	}
	
	public long getExpectedObjectID() { return objectID; }
	public boolean expectsObjectID(long id) { return objectIDs.contains(id); }

	public int getExpectedArgument() { return expectedArgument; }

	public boolean expectsSpecificArgument() { return expectedArgument >= 0; }
	
	public boolean expectsSpecificObjectID() { return objectID >= 0; }
	public boolean expectsOneOfManyObjectIDs() { return objectIDs != null; }
	
	public String toString() { return "expects id = " + objectID + " in " + (expectsSpecificArgument() ? "arg " + expectedArgument : " any arg ") + " of instruction."; }
	
}
