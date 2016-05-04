package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ISHL extends BinaryComputation {

	public ISHL(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 120; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "shifted left"; }

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getOperator() { return "<<"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
