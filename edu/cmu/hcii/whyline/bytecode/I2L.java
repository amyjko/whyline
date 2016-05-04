package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class I2L extends Conversion {

	public I2L(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 133; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.LONG_PRODUCED; }

	public String getOperator() { return "(long)"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
