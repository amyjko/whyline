package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class INSTANCEOF extends Instruction {

	private ClassInfo typeInfo;
	
	public INSTANCEOF(CodeAttribute method, ClassInfo typeInfo) {
		super(method);
		this.typeInfo = typeInfo;
	}

	public final int getOpcode() { return 193; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(typeInfo.getIndexInConstantPool());
		
	}

	public EventKind getTypeProduced() { return EventKind.BOOLEAN_PRODUCED; }

	public ClassInfo getClassInfo() { return typeInfo; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "Ljava/lang/Object;"; }

	public String getReadableDescription() { return "instanceof " + typeInfo.getName().getSimpleName(); }

	public String getAssociatedName() { return null; }

}