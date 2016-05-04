package edu.cmu.hcii.whyline.analysis;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;

import gnu.trove.TIntObjectHashMap;

/**
 * @author Andrew J. Ko
 *
 */
public class LocalDependencies {

	private final CodeAttribute code;

	private final TIntObjectHashMap<TIntObjectHashMap<List<GetLocal>>> usesByInstructionIndexLocalID;
	private final TIntObjectHashMap<TIntObjectHashMap<List<SetLocal>>> definitionsByInstructionIndexLocalID;

	public LocalDependencies(CodeAttribute code) {
		
		this.code = code;

		usesByInstructionIndexLocalID = new gnu.trove.TIntObjectHashMap<TIntObjectHashMap<List<GetLocal>>>();
		definitionsByInstructionIndexLocalID = new gnu.trove.TIntObjectHashMap<TIntObjectHashMap<List<SetLocal>>>();
	
	}

	public List<SetLocal> getPotentialDefinitionsOfGetLocal(GetLocal instruction) {
		return getPotentialDefinitionsOfLocalIDBefore(instruction, instruction.getLocalID());
	}
	
	public List<SetLocal> getPotentialDefinitionsOfIncrement(IINC instruction) {
		return getPotentialDefinitionsOfLocalIDBefore(instruction, instruction.getLocalID());
	}
	
	public List<SetLocal> getPotentialDefinitionsOfLocalIDBefore(Instruction instruction, int localID) {

		TIntObjectHashMap<List<SetLocal>> definitionsByInstruction = definitionsByInstructionIndexLocalID.get(localID);
		if(definitionsByInstruction == null) {
			definitionsByInstruction = new TIntObjectHashMap<List<SetLocal>>(4);
			definitionsByInstructionIndexLocalID.put(localID, definitionsByInstruction);
		}
		
		List<SetLocal> potentialDefs = definitionsByInstruction.get(instruction.getIndex());
		if(potentialDefs == null) {

			potentialDefs = new LinkedList<SetLocal>();
			definitionsByInstruction.put(instruction.getIndex(), potentialDefs);

			gnu.trove.TIntHashSet visited = new gnu.trove.TIntHashSet(64);

			Vector<Instruction> instructionsToAnalyze = new Vector<Instruction>(3);
			instructionsToAnalyze.add(instruction);

			while(!instructionsToAnalyze.isEmpty()) {

				Instruction instructionToAnalyze = instructionsToAnalyze.remove(instructionsToAnalyze.size() - 1);

				if(!visited.contains(instructionToAnalyze.getIndex())) {

					visited.add(instructionToAnalyze.getIndex());

					for(Instruction predecessor : instructionToAnalyze.getOrderedPredecessors())
						if(predecessor instanceof SetLocal && ((SetLocal)predecessor).getLocalID() == localID)
							potentialDefs.add((SetLocal)predecessor);
						else 
							instructionsToAnalyze.add(predecessor);
					
				}
				
			}
			
		}
			
		return potentialDefs;
		
	}
	
	public List<GetLocal> getPotentialUsesOfArgument(int index) {
		
		assert index < code.getMethod().getLocalIDOfFirstNonArgument() : "local " + index + " in " + code.getMethod() + " isn't an argument.";

		return getPotentialUsesOfLocalIDAtOrAfter(code.getFirstInstruction(), index);
		
	}

	// Caches results from previous calls with the given parameters.
	public List<GetLocal> getPotentialUsesOfLocalIDAtOrAfter(Instruction instruction, int localID) {
		
		TIntObjectHashMap<List<GetLocal>> usesByInstruction = usesByInstructionIndexLocalID.get(localID);
		if(usesByInstruction == null) {
			usesByInstruction = new TIntObjectHashMap<List<GetLocal>>(4);
			usesByInstructionIndexLocalID.put(localID, usesByInstruction);
		}
		
		List<GetLocal> potentialUses = usesByInstruction.get(instruction.getIndex());
		if(potentialUses == null) {

			potentialUses = new LinkedList<GetLocal>();
			usesByInstruction.put(instruction.getIndex(), potentialUses);
		
			if(instruction instanceof GetLocal && ((GetLocal)instruction).getLocalID() == localID)
				potentialUses.add((GetLocal)instruction);
	
			// Have to do this iteratively so we don't get a stack overflow if the method is too large.
			gnu.trove.TIntHashSet visited = new gnu.trove.TIntHashSet(64);

			Vector<Instruction> instructionsToAnalyze = new Vector<Instruction>(3);
	
			instructionsToAnalyze.add(instruction);

			while(!instructionsToAnalyze.isEmpty()) {

				Instruction instructionToAnalyze = instructionsToAnalyze.remove(instructionsToAnalyze.size() - 1);

				if(visited.contains(instructionToAnalyze.getIndex())) {

				}
				else {

					visited.add(instructionToAnalyze.getIndex());

					if(instructionToAnalyze.nextInstructionIsOnlySuccessor()) {

						Instruction successor = instructionToAnalyze.getNext();
						
						if(successor != null) {
						
							if(successor instanceof GetLocal && ((GetLocal)successor).getLocalID() == localID)
								potentialUses.add((GetLocal)successor);
		
							// If we reach a new definition of this local, stop
							if(successor instanceof SetLocal && ((SetLocal)successor).getLocalID() == localID) {}
							else instructionsToAnalyze.add(successor);
							
						}

					}
					else {
	
						for(Instruction successor : instructionToAnalyze.getOrderedSuccessors()) {
				
							if(successor instanceof GetLocal && ((GetLocal)successor).getLocalID() == localID)
								potentialUses.add((GetLocal)successor);
		
							// If we reach a new definition of this local, stop
							if(successor instanceof SetLocal && ((SetLocal)successor).getLocalID() == localID) {}
							else instructionsToAnalyze.add(successor);
							
						}
						
					}
					
				}
				
			}
			
		}
			
		return potentialUses;
		
	}
	
}