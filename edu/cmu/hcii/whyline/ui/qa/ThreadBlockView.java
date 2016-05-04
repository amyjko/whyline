package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;

import edu.cmu.hcii.whyline.qa.ThreadBlock;

/**
 * @author Andrew J. Ko
 *
 */
public class ThreadBlockView extends EventBlockView<ThreadBlock> {

	private final int row;
	
	public ThreadBlockView(Visualization visualization, ThreadBlock block, int row) {
		
		super(visualization, block);

		this.row = row;
		
	}

	protected boolean isHiddenInitially() { return false; }
	
	public String determineFirstLine() { return "thread"; }
	public String determineSecondLine() { return getVisualization().getWhylineUI().getTrace().getThreadName(((ThreadBlock)getExplanation()).getThreadID()); }

	protected Color determineBorderColor() { return null; }

	// A thread block's position is constant. It always starts on the left.
	public void positionIfFirst(int globalLeft) {

		setLocalLeft(globalLeft, false);

	}

	public ThreadBlockView getThreadBlockView() { return this; }

	public int getRow() { return visualization.getRowForThread(getBlock().getThreadID()); }

	// We don't want threads to be collapsable.
	public boolean handleMouseDoubleClick(int localX, int localY, int mouseButton) { return false; }

}
