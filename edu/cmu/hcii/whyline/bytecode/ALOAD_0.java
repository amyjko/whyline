package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */
public final class ALOAD_0 extends GetLocal {

	public ALOAD_0(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 0; }

	public final int getOpcode() { return 42; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }

}
