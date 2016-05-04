package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class GetArrayValue extends Use {

	public GetArrayValue(CodeAttribute method) {
		super(method);
	}

	public int getNumberOfOperandsConsumed() { return 2; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public StackDependencies.Producers getArrayReferenceProducer() { return getProducersOfArgument(0); }
	public StackDependencies.Producers getArrayIndexProducer() { return getProducersOfArgument(1); }

	public String getTypeDescriptorOfArgument(int argIndex) { 
		
		if(argIndex == 0) return "Ljava/lang/Object;";
		else if(argIndex == 1) return "I";
		else return null;
		
	}

	public String getReadableDescription() { return "[...]"; }

	public String getAssociatedName() { return "[...]"; }

}