package edu.cmu.hcii.whyline.bytecode;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.analysis.LoopPath;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class Branch extends Instruction {

	private List<LoopPath> paths; 
		
	public Branch(CodeAttribute method) {
		super(method);
	}

	public abstract boolean isConditional();
	
	public abstract Instruction getTarget();

	/**
	 * Called after a method is done being parsed in order to resolve any absolute references into symbolic pointers.
	 */
	public abstract void resolveTargets(Instruction[] instructionsByByteIndex) throws AnalysisException;

	public abstract SortedSet<Instruction> createSuccessorsCache();
	
	public final boolean nextInstructionIsOnlySuccessor() { return false; }

	public abstract void replaceTarget(Instruction oldTarget, Instruction newTarget);
	
	private enum LoopAnalysisState { UNKNOWN, YES, NO };
	protected LoopAnalysisState isLoop = LoopAnalysisState.UNKNOWN;
	
	public final boolean isLoop() { 

		if(isLoop == LoopAnalysisState.UNKNOWN) {

			boolean isLoopFlag = determineIfLoop();
			if(isLoopFlag) isLoop = LoopAnalysisState.YES;
			else isLoop = LoopAnalysisState.NO;
			
		}

		return isLoop == LoopAnalysisState.YES; 
		
	}
	
	protected abstract boolean determineIfLoop();
	
	public final Instruction getSuccessorAfterLoop() {
		
		for(Instruction successor : getOrderedSuccessors())
			if(successor.getIndex() > getIndex())
				return successor;

		return null;
			
	}
	
	public List<LoopPath> getLoopPaths() {
		
		if(isLoop()) {
			
			if(paths == null) paths = generatePaths(this);

		}
		
		return paths == null ? Collections.<LoopPath>emptyList() : Collections.<LoopPath>unmodifiableList(paths);
		
	}
	
	private static Vector<LoopPath> generatePaths(Branch loop) {

		Set<Instruction> visited = new HashSet<Instruction>(20);
		
		LoopPath currentPath = new LoopPath();

		Vector<LoopPath> paths = new Vector<LoopPath>();
		
		Instruction instructionAfterLoop = loop.getSuccessorAfterLoop();
		
		generatePaths(instructionAfterLoop, loop, currentPath, visited, paths);
		
		return paths;
		
	}
	
	private static void generatePaths(Instruction afterLoop, Instruction inst, LoopPath currentPath, Set<Instruction> visited, Vector<LoopPath> paths) {
		
		// Visit each instruction only once. Once we reach the same instruction, add the path.
		if(inst == afterLoop || visited.contains(inst)) {
			paths.add(currentPath);
			return;
		}
		visited.add(inst);
		
		for(Instruction successor : inst.getOrderedSuccessors()) {
			
			// Fork the current path at each branch.
			LoopPath path = inst instanceof Branch ? path = new LoopPath(currentPath, (Branch)inst, successor) : currentPath;
			
			generatePaths(afterLoop, successor, path, visited, paths);
			
		}
		
	}
	
	public abstract String getKeyword();
	
	public String getAssociatedName() { return null; }

}