package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Andrew J. Ko
 * 
 */
public abstract class AbstractReturn extends Instruction {

	public AbstractReturn(CodeAttribute method) {
		super(method);
	}
	
	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	// By definition, return instructions have no successors. They're the last instruction executed!
	public final SortedSet<Instruction> createSuccessorsCache() {
		
		return new TreeSet<Instruction>();
		
	}

	public final boolean nextInstructionIsOnlySuccessor() { return false; }

	public String getReadableDescription() { return "return"; }

	public String getAssociatedName() { return getMethod().getInternalName() + "()"; }

}
