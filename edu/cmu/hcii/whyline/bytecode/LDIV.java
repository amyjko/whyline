package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LDIV extends BinaryComputation {

	public LDIV(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 109; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "divided"; }

	public EventKind getTypeProduced() { return EventKind.LONG_PRODUCED; }

	public String getOperator() { return "/"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "L"; }

}
