package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class MULTIANEWARRAY extends ArrayAllocation {

	private ClassInfo typeInfo;
	private int numberOfDimensions;
	
	public MULTIANEWARRAY(CodeAttribute method, ClassInfo typeInfo, int numberOfDimensions) {
		super(method);
		this.typeInfo = typeInfo;
		this.numberOfDimensions = numberOfDimensions;
	}

	public final int getOpcode() { return 197; }
	public int byteLength() { return 4; }

	public int getNumberOfDimensions() { return numberOfDimensions; }
	
	public int getNumberOfOperandsConsumed() { return numberOfDimensions; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(typeInfo.getIndexInConstantPool());
		code.writeByte(numberOfDimensions);
		
	}

	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

	public QualifiedClassName getClassnameOfTypeProduced() {
		
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < numberOfDimensions; i++)
			builder.append("[");

		builder.append("L");
		builder.append(typeInfo.getName());
		builder.append(";");
		return QualifiedClassName.get(builder.toString());
		
	}

}