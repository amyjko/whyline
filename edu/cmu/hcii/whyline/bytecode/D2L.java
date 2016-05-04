package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class D2L extends Conversion {

	public D2L(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 143; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.LONG_PRODUCED; }

	public String getOperator() { return "(long)..."; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "D"; }

}
