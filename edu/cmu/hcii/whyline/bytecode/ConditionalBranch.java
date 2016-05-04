package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class ConditionalBranch extends Branch {

	private int offset;
	private Instruction trueDestination, falseDestination;
	
	public ConditionalBranch(CodeAttribute method, int offset) {
		
		super(method);
		this.offset = offset;

	}

	public boolean couldJumpTo(Instruction instruction) {
		
		return instruction == trueDestination || instruction == falseDestination;
		
	}

	public SortedSet<Instruction> createSuccessorsCache() {
		
		SortedSet<Instruction> successors = new TreeSet<Instruction>();
		successors.add(trueDestination);
		successors.add(falseDestination);
		return successors;

	}
	
	public boolean isConditional() { return true; }
	
	public Instruction getTrueDestination() { return trueDestination; }	
	public Instruction getFalseDestination() { return falseDestination; }

	public int getOffset() { return getTrueDestination().getByteIndex() - getByteIndex(); }

	public Instruction getTarget() { return trueDestination; }
	
	public Instruction getInstructionAfterConditionalBlock() { 
		
		if(jumpsForward()) {
			Instruction before = getTarget().getPrevious();
			if(before instanceof GOTO) return ((GOTO)before).getTarget();
			else return getTarget();
		}
		else {
			return getNext();
		}
		
	}
		
	public boolean jumpsForward() { return getTarget().getIndex() > getIndex(); }
	
	public void resolveTargets(Instruction[] instructionsByByteIndex) {

		trueDestination = instructionsByByteIndex[getByteIndex() + offset];
		code.addIncomingBranchToInstruction(this, trueDestination);

		falseDestination = getNext();
		code.addIncomingBranchToInstruction(this, falseDestination);

	}

	public void replaceTarget(Instruction oldTarget, Instruction newTarget) {

		if(oldTarget == trueDestination) trueDestination = newTarget;
		else if(oldTarget == falseDestination) falseDestination = newTarget;
		else throw new RuntimeException("Didn't pass the old true or false target!");
			
	}

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(getOffset());

	}

	public boolean isForLoop() {
		
		UnconditionalBranch unconditional = getUnconditionalBranchPrecessessor();
		return unconditional != null && getTarget() == unconditional.getNext();
		
	}
	
	public EventKind getTypeProduced() { return null; }

	protected boolean determineIfLoop() { return trueDestination.getIndex() < getIndex() && canReachInMethod(this); }

	public final String getKeyword() { return isLoop() ? isForLoop() ? "for" : "while" : "if"; }

	public String toString() { return super.toString() + " ? " + getTrueDestination().getIndex(); }
	
}