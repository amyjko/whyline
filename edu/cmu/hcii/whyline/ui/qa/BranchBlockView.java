package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;
import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.bytecode.Branch;
import edu.cmu.hcii.whyline.qa.BranchBlock;
import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class BranchBlockView extends EventBlockView<BranchBlock> {

	private boolean isRedundantLoopBlock;
	
	public BranchBlockView(Visualization visualization, BranchBlock block) {
		
		super(visualization, block);
			
	}

	public boolean isFamiliarInitially() { return visualization.getTrace().hasUserSourceFileFor(visualization.getTrace().getInstruction(getEventID()).getClassfile()); }
	
	protected void setIsRedundantLoopBlock(boolean isRedundant) {
		
		this.isRedundantLoopBlock = isRedundant;
		// Don't do this! The user didn't ask us to!
//		setCollapsed(isRedundant);
		updateDescription();
		
	}

	public String determineFirstLine() { 
		
		return ((Branch)visualization.getTrace().getInstruction(getExplanation().getEventID())).getKeyword(); 
		
	}

	public String determineSecondLine() { return null; }
	
	protected Color determineBorderColor() { 
		
		if(isRedundantLoopBlock) return UI.CONTROL_COLOR;
		else return UI.CONTROL_COLOR; 
		
	}
		
	protected void paintExpanded(Graphics2D g) {

		// Draw a vertical bar to represent the branch, distinguishing it from other types of blocks.
		g.setColor(determineBorderColor());
		int x = (int)(getVisibleLocalLeft() + widthOfLabel + UI.PADDING_WITHIN_EVENTS * 2);
		g.drawLine(x, (int)getVisibleLocalTop(), x, (int)getVisibleLocalBottom());

		super.paintExpanded(g);
		
	}
	
	public void drawSelection(Graphics2D g) {
		
		java.awt.Stroke stroke = g.getStroke();
		g.setStroke(UI.SELECTED_STROKE);
		g.setColor(UI.getHighlightColor());
		g.drawRoundRect((int)getLocalLeft(), (int)getLocalTop(), (int)widthOfLabel + UI.PADDING_WITHIN_EVENTS * 2, (int)getLocalHeight(), UI.getRoundedness(), UI.getRoundedness());
		g.setStroke(stroke);
		
	}
	
	protected void paintLabel(Graphics2D g) {

		g.drawGlyphVector(getFirstLineGlyphs(), (float)getVisibleLocalLeft() + UI.PADDING_WITHIN_EVENTS, (float)(getLocalTop() + heightOfLabel));

	}
	
}
