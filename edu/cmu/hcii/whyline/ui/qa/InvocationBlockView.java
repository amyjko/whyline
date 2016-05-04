package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;
import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.qa.InvocationBlock;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.views.View;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class InvocationBlockView extends EventBlockView<InvocationBlock> {
	
	public InvocationBlockView(Visualization visualization, InvocationBlock block) {
		
		super(visualization, block);
		
	}

	protected boolean isFamiliarInitially() { 

		MethodrefInfo methodref = ((Invoke)visualization.getTrace().getInstruction(getBlock().getEventID())).getMethodInvoked();
		Trace trace = visualization.getTrace();
		
		Classfile classfile = trace.getClassfileByName(methodref.getClassName());
		boolean methodSourceIsFamiliar = classfile != null && trace.hasUserSourceFileFor(classfile);
		return methodSourceIsFamiliar;
		
	}

	public final static void paintParensAndBraces(EventBlockView<?> view, Visualization visualization, Graphics2D g) {

		int leftParenX = (int) (view.getLocalLeft() + UI.PADDING_WITHIN_EVENTS * 2 + view.widthOfLabel);
		int parenY = (int) (view.getLocalBottom() - (view.getLocalHeight() - (visualization.PAREN_ASCENT)) / 2 - visualization.PAREN_DESCENT);
		
		ArgumentEventView lastVisibleArgument = null;
		for(View child : view.getChildren())
			if(child instanceof ArgumentEventView && !((ArgumentEventView)child).isHidden()) {
				lastVisibleArgument = (ArgumentEventView)child;
			}
		
		int rightParenX = (lastVisibleArgument == null ? leftParenX  + (int)visualization.PAREN_WIDTH : (int) (view.getLocalLeft() + lastVisibleArgument.getLocalRight() + UI.PADDING_WITHIN_EVENTS));
		
		g.setFont(UI.getSmallFont());
		g.setColor(UI.getControlTextColor());
		g.drawGlyphVector(visualization.LEFT_PAREN, leftParenX, parenY);
		g.drawGlyphVector(visualization.RIGHT_PAREN, rightParenX, parenY);
		g.drawGlyphVector(visualization.CLOSING_BRACE, (float)view.getVisibleLocalRight() + UI.PADDING_WITHIN_EVENTS, parenY);

	}
	
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
				
		paintParensAndBraces(this, visualization, g);
		
	}

	public String determineFirstLine() { 
	
		Invoke invoke = ((Invoke)visualization.getTrace().getInstruction(getEventID()));
		String methodName = invoke.getJavaMethodName();
		String full = methodName + (isCollapsed() ? "()" : ""); 
		return Util.elide(full, UI.MAX_EVENT_LENGTH);
		
	}

	public String determineSecondLine() { return null; }

	protected Color determineBorderColor() { return getBlock().invocationWasInstrumented() ? UI.CONTROL_COLOR : UI.UNFAMILIAR_COLOR; }
		
}
