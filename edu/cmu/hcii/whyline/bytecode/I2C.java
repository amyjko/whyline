package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class I2C extends Conversion {

	public I2C(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 146; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.CHARACTER_PRODUCED; }

	public String getOperator() { return "(char)"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
