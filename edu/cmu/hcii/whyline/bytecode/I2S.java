package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class I2S extends Conversion {

	public I2S(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 147; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.SHORT_PRODUCED; }

	public String getOperator() { return "(short)"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
