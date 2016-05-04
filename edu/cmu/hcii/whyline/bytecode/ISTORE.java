package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ISTORE extends SetLocal {

	private int index;
	
	public ISTORE(CodeAttribute method, int offset) {
		super(method);
		this.index = offset;
	}

	public int getLocalID() { return index; }

	public final int getOpcode() { return 54; }
	public int byteLength() { return 2; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(index);
		
	}

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
