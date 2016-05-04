package edu.cmu.hcii.whyline.analysis;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;

/**
 * @author Andrew J. Ko
 *
 */
public class Reachability {

	public static boolean canClassReachInstruction(Classfile classfile, Instruction instruction) {

		return canClassReachInstruction(classfile, instruction, new HashSet<Instruction>());
		
	}
	
	private static boolean canClassReachInstruction(Classfile classfile, Instruction instruction, Set<Instruction> instructionsInspected) {

		if(instructionsInspected.contains(instruction)) return false;
		instructionsInspected.add(instruction);
		
		// If this instruction is in the given class, or a superclass of the given classfile, then maybe yes!
		if(instruction.getClassfile() == classfile || instruction.getClassfile().isSuperclassOf(classfile))
			return true;
		
		// Look at all of the callers to the method that contains the instruction.
		for(Invoke invoke : instruction.getMethod().getPotentialCallers())
			if(canClassReachInstruction(classfile, invoke, instructionsInspected)) return true;

		return false;
		
	}
	
}