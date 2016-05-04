package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class CHECKCAST extends Computation {

	private final ClassInfo classInfo;
	
	public CHECKCAST(CodeAttribute method, ClassInfo classInfo) {
		super(method);
		this.classInfo = classInfo;
	}

	public final int getOpcode() { return 192; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(classInfo.getIndexInConstantPool());
		
	}

	public QualifiedClassName getTypeCast() { return classInfo.getName(); }
	
	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "Ljava/lang/Object;"; }

	public String getOperator() { return "(" + classInfo.getName().getSimpleName() + ")"; }

	public String toString() { return super.toString() + " " + classInfo.getName(); }
	
}
