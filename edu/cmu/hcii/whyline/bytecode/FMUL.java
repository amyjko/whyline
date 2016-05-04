package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class FMUL extends BinaryComputation {

	public FMUL(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 106; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "multiplied"; }

	public EventKind getTypeProduced() { return EventKind.FLOAT_PRODUCED; }
	
	public String getOperator() { return "*"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "F"; }

}
