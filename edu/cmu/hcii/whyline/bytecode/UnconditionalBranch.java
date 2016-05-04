package edu.cmu.hcii.whyline.bytecode;

import java.util.*;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class UnconditionalBranch extends Branch {

	private int offset;
	private Instruction destination = null;
	
	public UnconditionalBranch(CodeAttribute method, int offset) {
		super(method);
		this.offset = offset;
	}

	public boolean isConditional() { return false; }
	
	public Instruction getDestination() { return destination; }

	public int getOffset() { return destination.getByteIndex() - getByteIndex(); }
	
	public Instruction getTarget() { return destination; }
	
	public void resolveTargets(Instruction[] instructionsByByteIndex) {

		destination = instructionsByByteIndex[getByteIndex() + offset];
		code.addIncomingBranchToInstruction(this, destination);
		
	}
	
	public void replaceTarget(Instruction oldTarget, Instruction newTarget) {

		if(destination == oldTarget) destination = newTarget;
		else throw new RuntimeException("Didn't pass in the old target!");

	}

	public SortedSet<Instruction> createSuccessorsCache() {
		
		SortedSet<Instruction> successors = new TreeSet<Instruction>();
		successors.add(destination);
		return successors;

	}

	public boolean couldJumpTo(Instruction instruction) {
		
		return instruction == destination;
		
	}
	
	protected boolean determineIfLoop() { return destination.getIndex() < getIndex() && canReachInMethod(this); }
	
	public String getTypeDescriptorOfArgument(int argIndex) { return null; }

	public EventKind getTypeProduced() { return null; }
	
	public String toString() { return super.toString() + " " + getTarget().getIndex(); }
	
}