package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class I2D extends Conversion {

	public I2D(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 135; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.DOUBLE_PRODUCED; }

	public String getOperator() { return "(double)"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
