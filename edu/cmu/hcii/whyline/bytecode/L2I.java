package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class L2I extends Conversion {

	public L2I(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 136; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getOperator() { return "(int)"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "L"; }

}
