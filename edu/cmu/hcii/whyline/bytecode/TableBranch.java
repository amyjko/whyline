package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.analysis.AnalysisException;


/**
 * 
 * @author Andrew J. Ko
 *
 */  
public abstract class TableBranch extends Branch {

	public TableBranch(CodeAttribute method) {
		super(method);
	}
	
	public boolean isConditional() { return true; }

	public abstract Iterable<Instruction> getNonDefaultTargets();
	public abstract int getNumberOfNonDefaultTargets();

	protected boolean determineIfLoop() { return false; }
	
	public boolean couldJumpTo(Instruction instruction) {

		for(Instruction target : getNonDefaultTargets()) 
			if(target == instruction) return true;
		return false;
		
	}

	// The approach is to find the instruction to which one or more cases
	// jump to after finished. If there are none, then we cannot determine the
	// instruction after, and we might as well return the first instruction of the last case.
	public final Instruction getInstructionAfterTable() throws AnalysisException {

		int furthestTarget = 0;
		
		for(Instruction target : getNonDefaultTargets())
			if(target.getIndex() > furthestTarget)
				furthestTarget = target.getIndex();			

		Instruction furthestTargetWithinTable = null;

		Instruction currentInstruction = this;
		while(currentInstruction.getIndex() < furthestTarget) {

			currentInstruction = currentInstruction.getNext();
			if(currentInstruction instanceof Branch) {

				Instruction target = ((Branch)currentInstruction).getTarget();
				if(furthestTargetWithinTable == null) furthestTargetWithinTable = target;
				else if(target.getIndex() > furthestTargetWithinTable.getIndex())
					furthestTargetWithinTable = target;			
				
			}
			
		}

		// If we found no branches, or the furthest branch we did find was still not as far as the furthest target,
		// make the furthest target the last instruction after the table.
		if(furthestTargetWithinTable == null || furthestTargetWithinTable.getIndex() < furthestTarget)
			furthestTargetWithinTable = getCode().getInstruction(furthestTarget);
		
		return furthestTargetWithinTable;
			
	}
	
	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}