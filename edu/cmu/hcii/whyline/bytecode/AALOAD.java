package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */
public final class AALOAD extends GetArrayValue {

	public AALOAD(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 50; }
	public int byteLength() { return 1; }
	
	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }
	
}