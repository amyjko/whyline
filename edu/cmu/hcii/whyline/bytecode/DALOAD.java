package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DALOAD extends GetArrayValue {

	public DALOAD(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 49; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.DOUBLE_PRODUCED; }

}
