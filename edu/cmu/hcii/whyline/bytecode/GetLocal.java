package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class GetLocal extends Use {
	
	public GetLocal(CodeAttribute method) {
		super(method);
	}

	public abstract int getLocalID();
	
	public String getLocalIDName() {
		
		return getCode().getLocalIDNameRelativeToInstruction(getLocalID(), this);
		
	}

	public boolean getsMethodArgument() {

		return getLocalID() < getMethod().getLocalIDOfFirstNonArgument();
		
	}
	
	public abstract int getOpcode();

	public final int getNumberOfOperandsConsumed() { return 0; }
	public final int getNumberOfOperandsProduced() { return 1; }
	public final int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}
	
	public String getTypeDescriptorOfArgument(int argIndex) { return null; }

	public String toString() { return super.toString() + " " + getLocalIDName() + "(" + getLocalID() + ")"; }

	public String getReadableDescription() { return getLocalIDName(); }

	public String getAssociatedName() { return getLocalIDName(); }

}
