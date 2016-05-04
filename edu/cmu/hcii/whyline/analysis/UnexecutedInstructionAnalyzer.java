package edu.cmu.hcii.whyline.analysis;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class UnexecutedInstructionAnalyzer {

	private final Trace trace;

	private final Instruction[] unexecuted;
	private final Question<?> question;
	
	// This is either empty, of size one, or many. This determines much of whether a call counts as "executed".
	private final ExpectedObject objectExpectation;
	
	private final Set<UnexecutedInstruction> checked = new HashSet<UnexecutedInstruction>();
	
	private final Set<UnexecutedInstruction> instructionsToAdd = new HashSet<UnexecutedInstruction>();
	private final List<UnexecutedInstruction> instructionsToAnalyze = new Vector<UnexecutedInstruction>();

	// Independent of a particular instance.
	private UnexecutedInstructionAnalyzer(Question<?> question, Instruction[] unexecuted, ExpectedObject expectation) {
	
		this.trace = question.getTrace();
		this.unexecuted = unexecuted;
		this.question = question;
		this.objectExpectation = expectation;
		
	}

	public static  UnexecutedAnswer explain(Question<?> question, Instruction[] unexecuted, ExpectedObject expectation) {

		UnexecutedInstructionAnalyzer analyzer = new UnexecutedInstructionAnalyzer(question, unexecuted, expectation);
		return analyzer.explain();
		
	}
	
	// An iterative algorithm for determining one or more reasons why an instruction didn't execute.
	private UnexecutedAnswer explain() {
		
		instructionsToAnalyze.clear();
		instructionsToAdd.clear();
		
		UnexecutedInstruction[] unexecutedInstructions = new UnexecutedInstruction[unexecuted.length];
		int i = 0;
		for(Instruction un : unexecuted) {
			UnexecutedInstruction unInst = question.getUnexecutedInstruction(un, objectExpectation);
			unexecutedInstructions[i++] = unInst;
			instructionsToAnalyze.add(unInst);
		}

		// We don't need to compute the whole answer. Instead, we just compute until we reach familiar source, and at least three levels.
		while(instructionsToAnalyze.size() > 0) {
			
			boolean  foundFamiliar = false;
			
			// For each instruction not executed...
			for(UnexecutedInstruction ine : instructionsToAnalyze) {

				// Have we already checked this instruction not executed? If so, we're done analyzing this path.
				if(checked.contains(ine)) {}
				// Otherwise,
				else {

					foundFamiliar = trace.classIsReferencedInFamiliarSourceFile(ine.getInstruction().getClassfile().getInternalName());
					
					// Remember that we've checked it.
					checked.add(ine);

					// Have the instruction not executed find a reason why it wasn't executed.
					ine.explain();
									
					assert ine.getReason() != null : "There must be a reason why " + ine.getInstruction() + " didn't execute.";
				
					// Add all of the instruction not executed's incoming instructions not executed.
					instructionsToAdd.addAll(ine.getIncoming());

				}
				
			}

			// Now that we've analyzed all of the instructions to analyze, clear it it!
			instructionsToAnalyze.clear();
			
			// Then add all the new instructions to analyze.
			instructionsToAnalyze.addAll(instructionsToAdd);
			instructionsToAdd.clear();
			
			if(foundFamiliar)
				break;
			
		}
		
		return new UnexecutedAnswer(question, unexecutedInstructions);
		
	}
	
	private boolean DEBUG = false;
	private void debug(String message, int depth) {
		
		if(!DEBUG) return;
		
		for(int i = 0; i < depth; i++)
			System.out.print("  ");
		System.out.println("" + message);
		
	}

}