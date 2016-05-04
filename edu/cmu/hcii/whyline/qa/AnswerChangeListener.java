package edu.cmu.hcii.whyline.qa;

import java.util.Set;

public interface AnswerChangeListener {

	public void eventBlocksChanged(Set<ExplanationBlock> changedBlocks);
	public void threadBlockAdded(ThreadBlock top);
	
}
