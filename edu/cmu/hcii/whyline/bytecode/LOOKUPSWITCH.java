package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import edu.cmu.hcii.whyline.trace.EventKind;

/*

lookupswitch	
<0-3 byte pad\>	
defaultbyte1	
defaultbyte2	
defaultbyte3	
defaultbyte4	
npairs1	
npairs2	
npairs3	
npairs4	
match-offset pairs...	

*/
/**
 * @author Andrew J. Ko
 *
 */ 
public final class LOOKUPSWITCH extends TableBranch {

	private final int defaultOffset;
	private Instruction defaultTarget;
	
	private final Pair[] pairs;
	private final Vector<Instruction> targets;
	
	private class Pair {
		
		public final int key, offset;

		public Pair(int key, int offset) {
			
			this.key = key;
			this.offset = offset;
			
		}
		
	}
	
	public LOOKUPSWITCH(CodeAttribute method, int defaultOffset, int numberOfPairs) {

		super(method);
	
		this.defaultOffset = defaultOffset;
		pairs = new Pair[numberOfPairs];
		targets = new Vector<Instruction>(numberOfPairs);
		
	}

	public SortedSet<Instruction> createSuccessorsCache() {
		
		SortedSet<Instruction> successors = new TreeSet<Instruction>();
		successors.addAll(targets);
		successors.add(defaultTarget);
		return successors;

	}

	public final int getOpcode() { return 171; }
	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void setPair(int index, int key, int offset) {
		
		pairs[index] = new Pair(key, offset);
		
	}
	
	public void toBytes(DataOutputStream code) throws IOException {

		code.write(getOpcode());
		int numberOfZeroedBytesToWrite = numberOfPaddedBytes();
		for(int i = 0; i < numberOfZeroedBytesToWrite; i++) code.writeByte(0);
		code.writeInt(defaultTarget.getByteIndex() - getByteIndex());
		code.writeInt(pairs.length);
		for(int i = 0; i < pairs.length; i++) {
			code.writeInt(pairs[i].key);
			code.writeInt(targets.get(i).getByteIndex() - getByteIndex());
		}
		
	}

	public void resolveTargets(Instruction[] instructionsByByteIndex) {

		defaultTarget = instructionsByByteIndex[getByteIndex() + defaultOffset];
		code.addIncomingBranchToInstruction(this, defaultTarget);
		
		for(int i = 0; i < pairs.length; i++) {

			Instruction target= instructionsByByteIndex[getByteIndex() + pairs[i].offset];
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

	public int byteLength() {
		
		return 1 + numberOfPaddedBytes() + 4 + 4 + pairs.length * 8;
		
	}
	
	private int numberOfPaddedBytes() {

		// The rule is that index of default byte must be modulo 4. This calculates how many bytes we're off without any padding.
		int bytesOff = (getByteIndex() + 1) % 4;
		return bytesOff == 0 ? 0 : 4 - bytesOff;
		
	}
	
	public Iterable<Instruction> getNonDefaultTargets() { return targets; }
	public Instruction getTarget() { return defaultTarget; }

	public int getNumberOfNonDefaultTargets() { return targets.size(); }
	
	public EventKind getTypeProduced() { return null; }

	public String toString() {
		
		String prefix = super.toString();
		
		String postfix = " default => " + defaultTarget.getIndex() + ", ";
		for(int i = 0; i < pairs.length; i++)
			postfix = postfix + pairs[i].key + " => " + targets.get(i).getIndex() + ", ";
		return prefix + postfix;
			
	}
	
	public String getReadableDescription() { return "switch"; }

	public String getKeyword() { return "switch"; }

}