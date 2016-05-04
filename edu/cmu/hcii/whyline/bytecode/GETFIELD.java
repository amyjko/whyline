package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class GETFIELD extends Use implements FieldrefContainer {

	private FieldrefInfo fieldInfo;
	
	public GETFIELD(CodeAttribute method, FieldrefInfo fieldInfo) {
		super(method);
		this.fieldInfo = fieldInfo;
	}

	public final int getOpcode() { return 180; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 1; }
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

	public StackDependencies.Producers getReferenceProducer() { return getProducersOfArgument(0); }

	public String getTypeDescriptorOfArgument(int argIndex) { return "Ljava/lang/Object;"; }

	public String toString() { return super.toString() + " " + fieldInfo.toString(); }

	public String getReadableDescription() { return fieldInfo.getName(); }

	public String getAssociatedName() { return getFieldref().getName(); }

}
