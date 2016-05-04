package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class SetArrayValue extends Definition {
	
	public SetArrayValue(CodeAttribute method) {
		super(method);
	}

	public final int getNumberOfOperandsConsumed() { return 3; }
	public final int getNumberOfOperandsProduced() { return 0; }
	public final int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public String getLocalIDName() {
		
		return "array";
		
	}

	public String getTypeDescriptorOfArgument(int argIndex) { 
		
		if(argIndex == 0) return "Ljava/lang/Object;";
		else if(argIndex == 1) return "I";
		else return getTypeDescriptorOfArray();
		
	}

	public abstract String getTypeDescriptorOfArray();
	
	public String toString() { return super.toString() + " [arrayref, index, value]"; }

	public String getReadableDescription() { return "[...] ="; }

	public String getAssociatedName() { return "[...]"; }

}