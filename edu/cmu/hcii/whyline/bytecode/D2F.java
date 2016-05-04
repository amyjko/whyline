package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class D2F extends Conversion {

	public D2F(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 144; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.FLOAT_PRODUCED; }

	public String getOperator() { return "(float)..."; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "D"; }

}
