package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class RETURN extends AbstractReturn {

	public RETURN(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 177; }
	public int byteLength() { return 1; }
	public int getNumberOfOperandsConsumed() { return Opcodes.POPS_ALL_OPERANDS; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public EventKind getTypeProduced() { return null; }

	public String getTypeDescriptorOfArgument(int argIndex) { return null; }

}