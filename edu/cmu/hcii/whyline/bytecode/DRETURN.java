package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DRETURN extends AbstractReturn {

	public DRETURN(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 175; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.DOUBLE_PRODUCED; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "D"; }

}
