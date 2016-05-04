package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IADD extends BinaryComputation {

	public IADD(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 96; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "added"; }

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getOperator() { return "+"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
