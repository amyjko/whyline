package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IREM extends BinaryComputation {

	public IREM(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 112; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "got the remainder of the division of"; }

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getOperator() { return "%"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
