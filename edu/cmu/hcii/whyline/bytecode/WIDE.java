package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class WIDE extends Instruction {

	private int opcode, index, constant;
	
	public WIDE(CodeAttribute method, int opcode, int index, int constant) {
		super(method);
		this.opcode = opcode;
		this.index = index;
		this.constant = constant;
	}

	public final int getOpcode() { return 196; }

	public int byteLength() {
		
		return opcode == Opcodes.IINC ? 6 : 4;
		
	}
	
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(opcode);
		code.writeShort(index);
		if(opcode == Opcodes.IINC) code.writeShort(constant);
		
	}

	public String getTypeDescriptorOfArgument(int argIndex) { return null; }
	
	public EventKind getTypeProduced() { return null; }
	
	public String getReadableDescription() { return "wide"; }

	public String getAssociatedName() { return null; }

}