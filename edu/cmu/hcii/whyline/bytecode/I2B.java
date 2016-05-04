package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class I2B extends Conversion {

	public I2B(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 145; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.BYTE_PRODUCED; }

	public String getOperator() { return "(byte)"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
