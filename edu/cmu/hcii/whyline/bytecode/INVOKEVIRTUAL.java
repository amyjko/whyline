package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class INVOKEVIRTUAL extends Invoke {

	public INVOKEVIRTUAL(CodeAttribute method, MethodrefInfo methodInfo) {
		super(method, methodInfo);
	}
	
	public final int getOpcode() { return 182; }
	public int byteLength() { return 3; }
	
	public void toBytes(DataOutputStream code) throws IOException {

		code.writeByte(getOpcode());
		code.writeShort(methodInfo.getIndexInConstantPool());

	}

}