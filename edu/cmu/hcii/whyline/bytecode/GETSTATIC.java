package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class GETSTATIC extends Use implements FieldrefContainer {

	private FieldrefInfo fieldInfo;
	
	public GETSTATIC(CodeAttribute method, FieldrefInfo fieldInfo) {
		super(method);
		this.fieldInfo = fieldInfo;
	}

	public final int getOpcode() { return 178; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public FieldrefInfo getFieldref() { return fieldInfo; }

	public void toBytes(DataOutputStream code) throws IOException {

		code.writeByte(getOpcode());
		code.writeShort(fieldInfo.getIndexInConstantPool());

	}

	public EventKind getTypeProduced() { 

		return NameAndTypeInfo.typeCharacterToClass(fieldInfo.getTypeDescriptor().charAt(0));
			
	}

	public String toString() { return super.toString() + " " + fieldInfo.toString(); }

	public String getTypeDescriptorOfArgument(int argIndex) { return "Ljava/lang/Object;"; }

	public String getReadableDescription() { return fieldInfo.getName(); }

	public String getAssociatedName() { return getFieldref().getName(); }

}
