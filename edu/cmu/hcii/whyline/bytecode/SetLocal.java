package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class SetLocal extends Definition {

	public SetLocal(CodeAttribute method) {
		super(method);
	}

	public abstract int getLocalID();

	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public List<GetLocal> getPotentialUses() { return getCode().getLocalDependencies().getPotentialUsesOfLocalIDAtOrAfter(this, getLocalID()); }
	
	public boolean setsMethodArgument() {

		return getLocalID() < getMethod().getLocalIDOfFirstNonArgument();
		
	}

	public String getLocalIDName() {

		return getCode().getLocalIDNameRelativeToInstruction(getLocalID(), getCode().getInstruction(getIndex() + 1));
		
	}

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public String getAssociatedName() { return getLocalIDName(); }

	public String toString() { return super.toString() + " " + getLocalIDName() + "(" + getLocalID() + ")"; }

}
