package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IINC extends SetLocal {

	private final int index, constant;
	
	public IINC(CodeAttribute method, int index, int constant) {

		super(method);
		this.index = index;
		this.constant = constant;
	
	}

	public final int getOpcode() { return 132; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(index);
		code.writeByte(constant);
		
	}

	public int getLocalID() { return index; }

	public int getIncrement() { return constant; }
		
	public String getTypeDescriptorOfArgument(int argIndex) { return null; }

	public String toString() { return super.toString() + " + " + constant; }
	
}
