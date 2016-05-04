package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class NEW extends Instantiation {

	private ClassInfo typeInfo;
	
	public NEW(CodeAttribute method, ClassInfo typeInfo) {
		super(method);
		this.typeInfo = typeInfo;
	}

	public final int getOpcode() { return 187; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(typeInfo.getIndexInConstantPool());
		
	}

	public String getTypeDescriptorOfArgument(int argIndex) { return null; }

	public ClassInfo getClassInstantiated() { return typeInfo; }
	
	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }
	
	public QualifiedClassName getClassnameOfTypeProduced() {
		
		return typeInfo.getName();
		
	}

	public String toString() { return super.toString() + " " + typeInfo; }
	
}