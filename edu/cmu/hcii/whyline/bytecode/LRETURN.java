package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LRETURN extends AbstractReturn {

	public LRETURN(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 173; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return null; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "L"; }

}
