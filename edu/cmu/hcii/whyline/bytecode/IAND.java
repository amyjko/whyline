package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IAND extends BinaryComputation {

	public IAND(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 126; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "logical ANDed"; }

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getOperator() { return "&&"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
