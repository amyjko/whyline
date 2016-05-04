package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IUSHR extends BinaryComputation {

	public IUSHR(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 124; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "shifted right (unsigned)"; }

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getOperator() { return ">>>"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
