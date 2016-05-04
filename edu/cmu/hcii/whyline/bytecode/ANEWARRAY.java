package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;


/**
 * @author Andrew J. Ko
 *
 */
import edu.cmu.hcii.whyline.trace.EventKind;

// Creates a reference-valued array
public final class ANEWARRAY extends ArrayAllocation {

	private ClassInfo classInfo;
	
	public ANEWARRAY(CodeAttribute method, ClassInfo classInfo) {
		super(method);
		this.classInfo = classInfo;
	}

	public final int getOpcode() { return 189; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(classInfo.getIndexInConstantPool());
		
	}

	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

	public QualifiedClassName getClassnameOfTypeProduced() {
		
		return QualifiedClassName.get("[L" + classInfo.getName() + ";");
		
	}

}