package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class RET extends Instruction {

	private int index;
	
	public RET(CodeAttribute method, int index) {
		super(method);
		this.index = index;
	}

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(index);
		
	}
	
	public final int getOpcode() { return 169; }
	public int byteLength() { return 2; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public EventKind getTypeProduced() { return null; }

	public String getTypeDescriptorOfArgument(int argIndex) { return null; }

	public String getReadableDescription() { return "return"; }

	public String getAssociatedName() { return getMethod().getInternalName() + "()"; }

}