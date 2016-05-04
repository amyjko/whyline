package edu.cmu.hcii.whyline.analysis;

import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.source.Token;
import edu.cmu.hcii.whyline.source.TokenRange;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class LocalUsesSearch implements SearchResultsInterface {

	private final WhylineUI whylineUI;
	private final Instruction instruction;
	private final int localID;
	
	public LocalUsesSearch(WhylineUI whylineUI, Instruction code, int localID) {

		this.whylineUI = whylineUI;
		this.instruction = code;
		this.localID = localID;
		
	}

	public String getCurrentStatus() { return "Done."; }

	public SortedSet<Token> getResults() {

		// We use the next instruction in case the given on is prior to the initialization of the local.
		String name = instruction.getCode().getLocalIDNameRelativeToInstruction(localID, instruction.getNext());

		SortedSet<Token> lines = new TreeSet<Token>();

		for(Instruction i : instruction.getCode().getInstructions()) {

			boolean match = false;
			if(i instanceof GetLocal && ((GetLocal)i).getLocalID() == localID) {
				
				String iName = ((GetLocal)i).getLocalIDName();
				if(iName == null || iName.equals(name))
					match = true;
				
			}
			else if(i instanceof SetLocal && ((SetLocal)i).getLocalID() == localID) {
				
				String iName = ((SetLocal)i).getLocalIDName();
				if(iName == null || iName.equals(name))
					match = true;
				
			}

			if(match) {
				TokenRange range = i.getFile().getTokenRangeFor(i);
				for(Token t : range)
					lines.add(t);
			}
			
		}
		
		return lines;
		
	}

	public String getResultsDescription() { return  "uses of " + instruction.getCode().getLocalIDNameRelativeToInstruction(localID, instruction); }

	public boolean isDone() { return true; }

}
