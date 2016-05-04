package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LUSHR extends BinaryComputation {

	public LUSHR(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 125; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "shifted right (unsigned)"; }

	public EventKind getTypeProduced() { return EventKind.LONG_PRODUCED; }

	public String getOperator() { return ">>>"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "L"; }

}
