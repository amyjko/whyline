package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LALOAD extends GetArrayValue {

	public LALOAD(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 47; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.LONG_PRODUCED; }

}
