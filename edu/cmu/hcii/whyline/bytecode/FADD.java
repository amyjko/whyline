package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class FADD extends BinaryComputation {

	public FADD(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 98; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "added"; }

	public EventKind getTypeProduced() { return EventKind.FLOAT_PRODUCED; }

	public String getOperator() { return "+"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "F"; }

}
