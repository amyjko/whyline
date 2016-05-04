package edu.cmu.hcii.whyline.qa;

import java.util.Vector;

public final class LoopBlock extends ExplanationBlock {

	public LoopBlock(Answer answer, int eventID) {
		
		super(answer, eventID);
		
	}

	private final class LoopPathPatternSequence {
		
		public BranchBlock first, last;
		
		
		public LoopPathPatternSequence(BranchBlock first, BranchBlock last) {
			
			this.first = first;
			this.last = last;			
			
		}
		
		public boolean containsBlockButNotFirst(BranchBlock branchBlock) {
			
			return branchBlock.getBlock() == LoopBlock.this && branchBlock != first && branchBlock.getEventID() <= last.getEventID();
			
		}
		
	}

	private final Vector<LoopPathPatternSequence> pathPatterns = new Vector<LoopPathPatternSequence>();
	
	protected void handleNewEvent(Explanation newEvent) {

		// Go through all of the branch blocks in this loop block and find the sequences of paths.
		pathPatterns.removeAllElements();
		
		BranchBlock startOfSequence = null;
		BranchBlock previousBranchBlock = null;
		
		for(Explanation event : events) {
			
			if(event instanceof BranchBlock) {

				if(startOfSequence == null)
					startOfSequence = (BranchBlock)event;
				else if(startOfSequence.getLoopPath() != ((BranchBlock)event).getLoopPath()) {
				
					pathPatterns.add(new LoopPathPatternSequence(startOfSequence, previousBranchBlock));
					startOfSequence = (BranchBlock)event;
				
				}

				previousBranchBlock = (BranchBlock)event;
				
			}
			
		}

		// If we never found a different loop path, add one final sequence.
		if(startOfSequence != null)
			pathPatterns.add(new LoopPathPatternSequence(startOfSequence, previousBranchBlock));
		
	}
	
	// Redundant if its 
	public boolean isBranchBlockRedundant(BranchBlock branchBlock) {
		
		for(LoopPathPatternSequence sequence : pathPatterns)
			if(sequence.containsBlockButNotFirst(branchBlock)) return true;
		
		return false;
		
	}
	
}
