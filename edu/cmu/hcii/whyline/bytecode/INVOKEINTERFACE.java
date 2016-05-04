package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class INVOKEINTERFACE extends Invoke {

	private int count;
	
	public INVOKEINTERFACE(CodeAttribute method, InterfaceMethodrefInfo methodInfo, int count) {
		super(method, methodInfo);
		this.count = count;
	}
	
	public final int getOpcode() { return 185; }
	public int byteLength() { return 5; }

	public void toBytes(DataOutputStream code) throws IOException {

		code.writeByte(getOpcode());
		code.writeShort(methodInfo.getIndexInConstantPool());
		code.writeByte(count);
		code.writeByte(0);
		
	}
		
}