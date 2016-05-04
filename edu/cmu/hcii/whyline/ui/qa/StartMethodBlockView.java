package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;
import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.qa.StartMethodBlock;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.views.View;

/**
 * @author Andrew J. Ko
 *
 */
public class StartMethodBlockView extends EventBlockView<StartMethodBlock> {

	public StartMethodBlockView(Visualization visualization, StartMethodBlock block) {
		
		super(visualization, block);
		
	}

	protected void initializeVisibilityHelper() {

	}

	public String determineFirstLine() { 
		
		MethodInfo method = visualization.getTrace().getInstruction(getEventID()).getMethod();
		String methodName = method.getJavaName();
		return methodName; 
		
	}

	public String determineSecondLine() { return null; }

	protected double getPaddingAfterLabel() { 

		boolean hasVisibleArgument = false;
		for(View child : getChildren()) {
			if(child instanceof ArgumentEventView) {
				if(!((ArgumentEventView)child).isHidden()) {
					hasVisibleArgument = true;
					break;
				}
			}
			// If we've reached a non argument, then there isn't one.
			else break;
		}

		return hasVisibleArgument ? 0 : visualization.PAREN_WIDTH + visualization.PAREN_AND_BRACE_WIDTH;
	
	}

	protected final void paintExpanded(Graphics2D g) {

		super.paintExpanded(g);

		InvocationBlockView.paintParensAndBraces(this, visualization, g);
		
	}

	protected Color determineBorderColor() { return UI.CONTROL_COLOR; }
	
}
