package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class F2L extends Conversion {

	public F2L(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 140; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.LONG_PRODUCED; }

	public String getOperator() { return "(long)"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "F"; }

}
