package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.LoopPath;
import edu.cmu.hcii.whyline.bytecode.Branch;

public final class BranchBlock extends ExplanationBlock {

	private LoopPath path;
	
	public BranchBlock(Answer answer, int eventID) {
		
		super(answer, eventID);
			
	}

	public LoopPath getLoopPath() {
		
		Branch branch = (Branch)answer.trace.getInstruction(eventID); 
		
		// If this is a loop event, find which loop it matches.
		if(branch.isLoop()) {

			if(path == null) {
			
				LoopPath match = null;
				for(LoopPath path : branch.getLoopPaths()) {
	
					if(path.matches(answer.trace, eventID)) { match = path; break; }
					
				}
	
				path = match;
				
			}
			return path;
			
		}
		else return null;
		
	}
	
}
