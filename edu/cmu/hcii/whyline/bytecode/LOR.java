package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LOR extends BinaryComputation {

	public LOR(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 129; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "logical ORed"; }

	public EventKind getTypeProduced() { return EventKind.LONG_PRODUCED; }

	public String getOperator() { return "||"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "L"; }

}
