package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class LXOR extends BinaryComputation {

	public LXOR(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 131; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "logical XORed"; }

	public EventKind getTypeProduced() { return EventKind.LONG_PRODUCED; }

	public String getOperator() { return "^"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "L"; }

}
