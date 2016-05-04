package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LCMP extends BinaryComputation {

	public LCMP(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 148; }
	public int byteLength() { return 1; }

	public String getPastTenseVerb() { return "compared"; }

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getOperator() { return getConsumers().getFirstConsumer().getReadableDescription(); }

	public String getTypeDescriptorOfArgument(int argIndex) { return "L"; }

}
