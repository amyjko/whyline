package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;

import edu.cmu.hcii.whyline.qa.LoopBlock;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.views.View;

/**
 * @author Andrew J. Ko
 *
 */
public class LoopBlockView extends EventBlockView<LoopBlock> {

	public LoopBlockView(Visualization visualization, LoopBlock block) {
		
		super(visualization, block);
		
	}
		
	public String determineFirstLine() { return "while"; /*UIConstants.INFINITY; infinity unicode*/ }
	public String determineSecondLine() { return null; }

	protected Color determineBorderColor() { return UI.CONTROL_COLOR; }

	public void synchronizeWithModel() {

		super.synchronizeWithModel();
	
		for(View child : getChildren()) {
			
			if(child instanceof BranchBlockView) {

				boolean redundant = getBlock().isBranchBlockRedundant(((BranchBlockView)child).getBlock());
				((BranchBlockView)child).setIsRedundantLoopBlock(redundant);
				
			}
			
		}

	}

}
