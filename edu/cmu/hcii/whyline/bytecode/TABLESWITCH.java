package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import edu.cmu.hcii.whyline.analysis.AnalysisException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
tableswitch	
<0-3 byte pad\>	
defaultbyte1	
defaultbyte2	
defaultbyte3	
defaultbyte4	
lowbyte1	
lowbyte2	
lowbyte3	
lowbyte4	
highbyte1	
highbyte2	
highbyte3	
highbyte4	
jump offsets...	
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class TABLESWITCH extends TableBranch {

	private int def, low, high;
	private List<Integer> offsets;
	
	private Instruction defaultTarget;
	private List<Instruction> targets;
	
	public TABLESWITCH(CodeAttribute method, int def, int low, int high, List<Integer> offsets) {

		super(method);
	
		this.def = def;
		this.low = low;
		this.high = high;
		this.offsets = offsets;
	
	}

	public final int getOpcode() { return 170; }
	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public int byteLength() {
		
		return 1 + numberOfPaddedBytes() + 4 + 4 + 4 + (high - low + 1) * 4;
		
	}

	public void toBytes(DataOutputStream code) throws IOException {

		code.write(getOpcode());
		int numberOfZeroedBytesToWrite = numberOfPaddedBytes();
		for(int i = 0; i < numberOfZeroedBytesToWrite; i++) code.writeByte(0);
		code.writeInt(defaultTarget.getByteIndex() - getByteIndex());
		code.writeInt(low);
		code.writeInt(high);
		for(Instruction i : targets) code.writeInt(i.getByteIndex() - getByteIndex());
		
	}

	public SortedSet<Instruction> createSuccessorsCache() {
		
		SortedSet<Instruction> successors = new TreeSet<Instruction>();
		successors.addAll(targets);
		successors.add(defaultTarget);
		return successors;

	}

	public void resolveTargets(Instruction[] instructionsByByteIndex) throws AnalysisException {

		defaultTarget = instructionsByByteIndex[getByteIndex() + def];
		code.addIncomingBranchToInstruction(this, defaultTarget);

		targets = new Vector<Instruction>();

		// Determine the instruction after the table switch by checking for unconditional jumps at the end
		// of each case (presumed to be before the beginning of each case).
		for(Integer offset : offsets) {
			
			Instruction target = instructionsByByteIndex[getByteIndex() + offset];
			code.addIncomingBranchToInstruction(this, target);
			targets.add(target);
		
		}

	}

	// Warning: this doesn't update the new target's origin list. Instrumented code in general isn't suitable for analysis.
	public void replaceTarget(Instruction oldTarget, Instruction newTarget) {

		// REMEMBER! The old target could be ONE OR MORE of the default or any of the targets. They must ALL be replaced.
		
		if(oldTarget == defaultTarget) defaultTarget = newTarget;

		for(int i = 0; i < targets.size(); i++) {
			if(targets.get(i) == oldTarget)
				targets.set(i, newTarget);
		}

	}

	private int numberOfPaddedBytes() {
		
		int bytesOff = (getByteIndex() + 1) % 4;
		return bytesOff == 0 ? 0 : 4 - bytesOff;
		
	}

	public Iterable<Instruction> getNonDefaultTargets() { return targets; }
	public Instruction getTarget() { return defaultTarget; }

	public int getNumberOfNonDefaultTargets() { return targets.size(); }

	public EventKind getTypeProduced() { return null; }

	public String toString() {
		
		String prefix = super.toString();
		
		String postfix = "low = " + low + ", high = " + high + ": default => " + defaultTarget.getIndex() + ", ";
		for(int i = 0; i < offsets.size(); i++)
			postfix = postfix + i + " => " + targets.get(i).getIndex() + ", ";
		return prefix + postfix;
		
	}
	
	public String getReadableDescription() { return "switch"; }

	public String getKeyword() { return "switch"; }

}