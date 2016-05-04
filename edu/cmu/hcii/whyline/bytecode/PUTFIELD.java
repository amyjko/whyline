package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class PUTFIELD extends Definition implements FieldrefContainer {

	private FieldrefInfo fieldInfo;
	
	public PUTFIELD(CodeAttribute method, FieldrefInfo fieldInfo) {
		super(method);
		this.fieldInfo = fieldInfo;
	}

	public final int getOpcode() { return 181; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 2; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public FieldrefInfo getFieldref() { return fieldInfo; }

	public String getLocalIDName() {
		
		return fieldInfo.getName();
		
	}

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(fieldInfo.getIndexInConstantPool());
		
	}

	public String getTypeDescriptorOfArgument(int argIndex) { 
		
		if(argIndex == 0) return "Ljava/lang/Object;";
		else if(argIndex == 1) return fieldInfo.getTypeDescriptor();
		else return null;
		
	}

	public String toString() { return super.toString() + " " + fieldInfo.toString(); }

	public String getAssociatedName() { return getFieldref().getName(); }

}