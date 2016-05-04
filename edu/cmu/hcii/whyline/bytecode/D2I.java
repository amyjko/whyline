package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class D2I extends Conversion {

	public D2I(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 142; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getOperator() { return "(int)..."; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "D"; }

}
