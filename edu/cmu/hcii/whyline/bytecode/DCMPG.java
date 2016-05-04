package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DCMPG extends BinaryComputation {

	public DCMPG(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 152; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "compared"; }

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getOperator() { return ">"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "D"; }

}
