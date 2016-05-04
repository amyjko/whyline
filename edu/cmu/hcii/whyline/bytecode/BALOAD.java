package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */
public final class BALOAD extends GetArrayValue {

	public BALOAD(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 51; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.BYTE_PRODUCED; }

}
