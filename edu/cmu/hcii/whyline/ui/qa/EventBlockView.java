package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.views.*;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class EventBlockView<T extends ExplanationBlock> extends EventView {

	protected final T block;
	
	private boolean isCollapsed = false;
	protected int numberOfVisibleBlockChildren = 0;
	
	public EventBlockView(Visualization visualization, T block) {
		
		super(visualization, block);
		
		visualization.associateBlockWithView(block, this);
		
		this.block = block;

		// A constant, so we set it here.
		setLocalTop(UI.BLOCK_VERTICAL_PADDING, false);

		setPercentToScaleChildren(UI.EVENT_BLOCK_SCALING);

		synchronizeWithModel();
		
		setCollapsed(true);
		
	}
	
	protected boolean isFamiliarInitially() { return visualization.getTrace().hasUserSourceFileFor(visualization.getTrace().getInstruction(getEventID()).getClassfile()); }
	
	public final double determineHeight(boolean animate) {
		
		double maxHeight = UI.MIN_THREAD_ROW_HEIGHT;
		for(View child : getChildren()) {
			if(child instanceof EventBlockView)
				maxHeight = Math.max(maxHeight, ((EventBlockView<?>)child).determineHeight(animate));
			else
				maxHeight = Math.max(maxHeight, UI.MIN_THREAD_ROW_HEIGHT);			
		}
		
		// If this is hidden and so are all of its children, we add no extra vertical space.
		if(isHidden() && numberOfVisibleBlockChildren == 0) {}
		else maxHeight += UI.BLOCK_VERTICAL_PADDING * 2;

		setLocalHeight(maxHeight, animate);
		
		return maxHeight;
		
	}
		
	public void setCollapsed(boolean collapsed) {

		boolean changed = this.isCollapsed != collapsed;
		this.isCollapsed = collapsed;

		for(View view : getChildren())
			((EventView)view).markAncestorIsCollapsed(isCollapsed || ancestorIsCollapsed);
		
		if(changed) updateDescription();
		
	}

	protected final void markAncestorIsCollapsed(boolean parentIsCollapsed) {
		
		boolean ancestorIsCollapsed = isCollapsed || parentIsCollapsed;
		
		for(View view : getChildren())
			((EventView)view).markAncestorIsCollapsed(ancestorIsCollapsed);
		super.markAncestorIsCollapsed(parentIsCollapsed);
		
	}

	public boolean isCollapsed() { return isCollapsed; }
	
	public void uncollapseAncestors() {

		EventBlockView<?> blockView = this;
		while(blockView != null) {
			
			blockView.setCollapsed(false);
			blockView = blockView.getBlockView();
			
		}
		
	}
	
	public final double getWidthBasedOnBlocksCollapsedState() {
		
		return
			ancestorIsCollapsed() ? 0 :
			isCollapsed ? getGlobalOffsetForFirstView() :
			Math.max(getGlobalOffsetForFirstView(), getRightmostChildsRight() * getPercentToScaleChildren());
			
	}
	
	public final double getGlobalOffsetForFirstView() {

		// Otherwise, if its hidden with no children visible, it shouldn't take up any space
		if(isHidden()) return 0;
		// If its not hidden, but its collapsed, the next event should be after the description vector.
		else if(isCollapsed) return UI.PADDING_WITHIN_EVENTS * 2 + widthOfLabel;
		// If its not collapsed, it should be at least after the description, plus a custom amount depending on the type.
		else return UI.PADDING_WITHIN_EVENTS + widthOfLabel + UI.PADDING_WITHIN_EVENTS + getPaddingAfterLabel();
		
	}

	protected double getPaddingAfterLabel() { return 0; }
	
	/**
	 * We use this both for the initial layout of events, and for updates to the event block.
	 * We just remove all the children, and count on the fact that the visualization is keeping
	 * track of all views for explanations in this answer.
	 */
	public void synchronizeWithModel() {
		
		// Start from scratch.
		removeChildren();

		// Add views for all of the children
		for(Explanation explanation : block.getEvents()) {

			EventView child = explanation instanceof ExplanationBlock ? 
					visualization.getViewOfBlock((ExplanationBlock)explanation) :
					visualization.getViewOfExplanation(explanation);
					
			// If we have no view of this, create one and add it after the previews view.
			if(child == null)
				child = visualization.createViewFor(explanation);

			addChild(child);

		}
		
		// Mark the ancestor is collapsed state for all of the new children and resynchronize the number of children not hidden.
		numberOfVisibleBlockChildren = 0;
		for(View view : getChildren()) {
			((EventView)view).markAncestorIsCollapsed(isCollapsed || ancestorIsCollapsed);
			if(view instanceof EventBlockView && !((EventBlockView<?>)view).isHidden()) numberOfVisibleBlockChildren++;
		}
		
	}
	
	public Visualization getVisualization() { return visualization; }
	
	public T getBlock() { return block; }

	// Instead of the center, choose the left of the label to the left
	public int getGlobalSelectionPointX() { return (int)(getGlobalLeft() + UI.PADDING_WITHIN_EVENTS + widthOfLabel / 2); }

	// Instead of the vertical center, point to the label's vertical center.
	public int getGlobalYToPointTo() { return (int) (getGlobalTop() + heightOfLabel / 2); }
	
	// Instead of the full width, just use the part with the label.
	public boolean containsGlobalTimeControllerX(int timeControllerX) {
		
		double globalLeftOfLabel = getGlobalLeft() + UI.PADDING_WITHIN_EVENTS;
		double globalRightOfLabel = globalLeftOfLabel + widthOfLabel;
		
		return timeControllerX >= globalLeftOfLabel && timeControllerX <= globalRightOfLabel;
		
	}

	protected boolean thisOrChildOfThisIsSelected() {
		
		EventView selection = visualization.getSelectedEventView();
//		return selection != null && (selection == this || (!(selection instanceof EventBlockView) && selection.getBlockView() == this));
		return selection == this;
		
	}

	public final void paintAboveChildren(Graphics2D g) {
		
		if(!isHidden() && !isFamiliar())
			paintCrosshatch(g, (int)widthOfLabel + UI.PADDING_WITHIN_EVENTS);
		
	}

	public final void paintBelowChildren(Graphics2D g) {
		
		super.paintBelowChildren(g);
		
		if(!isHidden()) {

			g = (Graphics2D)g.create();
			if(isCollapsed) paintCollapsed(g);
			else paintExpanded(g);

		}
		
	}
	
	public final void paintChildren(Graphics2D g) {
		
		if(!isCollapsed && !ancestorIsCollapsed()) super.paintChildren(g);
		
	}
	
	protected void paintExpanded(Graphics2D g) {
		
		EventView selection = visualization.getSelectedEventView();
		boolean selected = selection == this;

		// If this is selected, draw the selection stroke.
		if(selected)
			drawSelection(g);

		// We only draw an unselected event block border when:
		// (1) Its not a thread block
		// (2) There's some selected event view that is not this block view
		// (3) The selection's block view is this block view
		else if(getBlockView() != null && selection != null && selection.getNearestVisibleBlockView() == this)
			drawRoundBoundaries(determineBorderColor(), g, UI.getRoundedness(), UI.getRoundedness());

//		g.setColor(thisOrChildOfThisIsSelected() ? UIConstants.getControlTextColor() : determineBorderColor());
		g.setColor(UI.getControlTextColor());
		paintLabel(g);
		
	}

	protected void drawSelection(Graphics2D g) {
		
		// Vertically centered
		double y = getVisibleLocalBottom() - (getVisibleLocalHeight() - heightOfLabel) / 2;

		Stroke stroke = g.getStroke();
		g.setStroke(UI.SELECTED_STROKE);
		g.setColor(UI.getHighlightColor());
		g.drawRoundRect((int)getVisibleLocalLeft(), (int)(y - heightOfLabel), (int)widthOfLabel + UI.PADDING_WITHIN_EVENTS * 2, (int)heightOfLabel, UI.getRoundedness(), UI.getRoundedness());
		g.setStroke(stroke);

	}

	private void paintCollapsed(Graphics2D g) {

		boolean selected = visualization.getSelectedEventView() == this;
		
		Color borderColor = determineBorderColor();
		if(borderColor != null) {

			fillRoundBoundaries(borderColor, g, UI.getRoundedness(), UI.getRoundedness());

			if(selected)
				paintSelectionBorder(g);
			else
				drawRoundBoundaries(borderColor, g, UI.getRoundedness(), UI.getRoundedness());
		
		}

		g.setColor(UI.getControlTextColor());
		paintLabel(g);
		
	}
	
}