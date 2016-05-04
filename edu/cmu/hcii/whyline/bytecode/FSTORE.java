package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class FSTORE extends SetLocal {

	private int index;
	
	public FSTORE(CodeAttribute method, int index) {
		super(method);
		this.index = index;
	}

	public int getLocalID() { return index; }

	public final int getOpcode() { return 56; }
	public int byteLength() { return 2; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(index);
		
	}

	public String getTypeDescriptorOfArgument(int argIndex) { return "F"; }

}
