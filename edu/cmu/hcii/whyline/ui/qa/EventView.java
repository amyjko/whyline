package edu.cmu.hcii.whyline.ui.qa;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.GlyphVector;

import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.views.View;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class EventView extends View implements Comparable<EventView> {

	protected final Visualization visualization;
	private final Explanation explanation;
	private GlyphVector firstLineGlyphs, secondLineGlyphs;	
	protected double widthOfLabel;
	protected double widthOfFirstLine, widthOfSecondLine;
	protected double heightOfLabel;
	
	private boolean hidden = false;
	private boolean familiar = true;

	private boolean initialized = false;
	protected boolean ancestorIsCollapsed = false;
	private boolean followsHiddenEvents = false;
	
	public EventView(Visualization visualization, Explanation explanation) {

		this.visualization = visualization;
		this.explanation = explanation;
		
		visualization.associateExplanationWithView(explanation, this);

		updateDescription();

	}
	
	// Calls an overridden helper. Only calls it once.
	public final void initializeVisibility() {
		
		if(isInitialized()) return;
		setHidden(isHiddenInitially());
		setFamiliar(isFamiliarInitially());
		
	}
	
	protected boolean isHiddenInitially() { return true; }
	protected boolean isFamiliarInitially() { 	return visualization.getTrace().classIsReferencedInFamiliarSourceFile(visualization.getTrace().getInstruction(explanation.getEventID()).getClassfile().getInternalName()); }
	
	protected void setFamiliar(boolean familiar) { this.familiar = familiar; }
	
	protected void paintSelectionBorder(Graphics2D g) {

		Stroke stroke = g.getStroke();
		g.setStroke(UI.SELECTED_STROKE);
		drawRoundBoundaries(UI.getHighlightColor(), g, UI.getRoundedness(), UI.getRoundedness());
		g.setStroke(stroke);

	}
	
	// A "hidden" block doesn't draw itself or take up space in layout, but it does draw its children.
	public void setHidden(boolean hidden) {

		boolean changed = this.hidden != hidden;
		this.hidden = hidden;

		if(changed) {
			EventBlockView<?> blockView = getBlockView();
			if(blockView != null) blockView.numberOfVisibleBlockChildren += hidden ? -1 : 1;
			updateDescription();
		}
		
	}

	public boolean isHidden() { return hidden; }
	
	public boolean isFamiliar() { return familiar; }
	
	protected final boolean isInitialized() { 
		
		boolean i = initialized;
		if(!i) {
			initialized = true;
		}
		return i;
		
	}

	protected final void updateDescription() {
		
		String first = determineFirstLine();
		
		Graphics2D g = (Graphics2D)visualization.getWhylineUI().getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		firstLineGlyphs = UI.getSmallFont().createGlyphVector(g.getFontRenderContext(), first);

		String second = determineSecondLine();
		if(second !=null)
			secondLineGlyphs = UI.getSmallFont().createGlyphVector(g.getFontRenderContext(), second);
		
		java.awt.geom.Rectangle2D firstLineBounds = firstLineGlyphs.getLogicalBounds(); 
		java.awt.geom.Rectangle2D secondLineBounds = secondLineGlyphs == null ? null : secondLineGlyphs.getLogicalBounds(); 

		widthOfFirstLine = firstLineBounds.getWidth();
		widthOfSecondLine = secondLineBounds == null ? 0 : secondLineBounds.getWidth();
		widthOfLabel = Math.max(widthOfFirstLine, widthOfSecondLine);
		heightOfLabel = firstLineBounds.getHeight() + (secondLineBounds == null ? 0 : secondLineBounds.getHeight());		
		
		if(isHidden()) {
			setLocalWidth(UI.HIDDEN_EVENT_SIZE, false);
			setLocalHeight(UI.HIDDEN_EVENT_SIZE, false);
		}
		else {
			setLocalWidth(widthOfLabel + UI.PADDING_WITHIN_EVENTS * 2, false);
			setLocalHeight(heightOfLabel + UI.PADDING_WITHIN_EVENTS * 2, false);
		}
		
	}
	
	public abstract String determineFirstLine();

	public abstract String determineSecondLine();
	
	protected abstract Color determineBorderColor();

	protected GlyphVector getFirstLineGlyphs() { return firstLineGlyphs; }
	
	public EventBlockView<?> getBlockView() { 
		
		View parent = getParent();
		if(parent instanceof EventBlockView) return (EventBlockView<?>)parent;
		else return null;

	}

	public ThreadBlockView getThreadBlockView() { 
		
		EventBlockView<?> parent = getBlockView();
		while(parent != null && !(parent instanceof ThreadBlockView)) parent = parent.getBlockView();
		return (ThreadBlockView)parent;
		
	}

	public EventBlockView<?> getNearestVisibleBlockView() {
		
		EventBlockView<?> parent = getBlockView();
		while(parent != null && (parent.isCollapsed() || parent.isHidden())) parent = parent.getBlockView();
		return parent;
		
	}
	
	protected void markAncestorIsCollapsed(boolean isCollapsed) {
		
		ancestorIsCollapsed = isCollapsed;
		
	}

	/**
	 * This is a cached version of a recursive method, to speed up checking.
	 * 
	 * @return True if this is in a block that is collapsed, or has an ancestor that is collapsed.
	 */
	public final boolean ancestorIsCollapsed() { return ancestorIsCollapsed; }

	public final EventBlockView<?> getEldestCollapsedAncestor() {

		EventBlockView<?> blockView = getBlockView();
		// If there is no parent, return nothing.
		if(blockView == null) return null;
		// If the parent has a collapsed ancestor, return it.
		else if(blockView.ancestorIsCollapsed()) return blockView.getEldestCollapsedAncestor();
		// Otherwise, if the parent is itself collapsed, return the parent.
		else if(blockView.isCollapsed()) return blockView;
		// Otherwise, return nothing.
		else return null;

	}

	public double getLabelOffset() { return widthOfLabel + UI.PADDING_WITHIN_EVENTS; }
	
	public double getWidthBasedOnBlocksCollapsedState() {

		return
			ancestorIsCollapsed() ? 0 :
			isHidden() ? UI.HIDDEN_EVENT_SIZE :
			widthOfLabel + UI.PADDING_WITHIN_EVENTS * 2;
		
	}
	
	public double getAppropriateTop() {
		
		return (getParent().getLocalHeight() - getLocalHeight()) / 2;
		
	}

	public final Explanation getExplanation() { return explanation; }
	public final int getEventID() { return explanation.getEventID(); }
	
	// These are the points that will be used when determining where to place the time controller when an event is selected.
	public int getGlobalSelectionPointX() { return (int)(getGlobalLeft() + getGlobalWidth() / 2); }
	public int getGlobalSelectionPointY() { return (int)(getGlobalTop() + getGlobalHeight() / 2); }

	public int getGlobalXToPointTo() { return (int)getGlobalLeft(); }
	public int getGlobalYToPointTo() { return (int)(getGlobalTop() + getGlobalHeight() / 2); }
	
	public boolean containsGlobalTimeControllerX(int timeControllerX) {
		
		return timeControllerX >= getGlobalLeft() && timeControllerX <= getGlobalRight();
		
	}
	
	public boolean needsToBeExplained() { 
		
		return !(explanation instanceof ExplanationBlock) && explanation.needsToBeExplained(); 
		
	}
	
	public void setAppearsAfterHiddenEvent(boolean follows) { this.followsHiddenEvents  = follows; }
	
	public void paintBelowChildren(Graphics2D g) {
		
		// Draw the elision
		if(!isHidden() && (followsHiddenEvents || needsToBeExplained())) {
			
			double diametersPerDot = UI.DIAMETERS_PER_ELISION; 
			int diameter = UI.ELISION_DIAMETER;
			int space = UI.ELISION_PADDING;

			int rightEdge = (int)getVisibleLocalLeft();
			int leftEdge = rightEdge - space;
			int y = (int)(getVisibleLocalTop() + getVisibleLocalHeight() / 2 - diameter / 2);

			// How many dots will fit, assuming one diameter before right edge, after left edge?
			// Satisfy this constraint
			// 	diam + diam  + ( count * diametersPerDot * diam ) = available space
			int count = (int) ((space - 2 * diameter) / (diameter * diametersPerDot));
			
			g.setColor(UI.getControlTextColor());

			leftEdge += diameter * (diametersPerDot / 2 + 1);
			for(int i = 0; i < count; i++) {
				g.fillOval(leftEdge, y, diameter, diameter);
				leftEdge += diameter * diametersPerDot;
			}
			
		}
		
	}

	public void paintAboveChildren(Graphics2D g) {
		
		if(!isHidden()){
		
			if(visualization.getSelectedEventView() == this) {
				paintSelectionBorder(g);
			}
			else {
				Color color = determineBorderColor();
				if(color != null)
					drawRoundBoundaries(color, g, UI.getRoundedness(), UI.getRoundedness());
			}
	
			g.setColor(UI.getControlTextColor());
			g.setFont(UI.getSmallFont());
	
			// Cross hatch events of unfamiliar code
			if(!familiar) 
				paintCrosshatch(g, (int)getVisibleLocalWidth());

			paintLabel(g);
			
		}
				
	}
	
	protected void paintCrosshatch(Graphics2D g, int width) {

		g = (Graphics2D) g.create();

		int top = (int)getVisibleLocalTop();
		int left = (int) getVisibleLocalLeft();
		int height = (int)getVisibleLocalHeight();

		int stoppingPoint = left + width;
		if(getNumberOfChildren() > 0) stoppingPoint = (int) (left + getFirstChild().getLocalLeft());

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
		Util.drawCrosshatch(g, Color.black, left, stoppingPoint, top, height, UI.getCrosshatchSpacing(), 0);
		
	}
	
	protected void paintLabel(Graphics2D g) {
		
		float y = (int)(getVisibleLocalBottom() - (getVisibleLocalHeight() - heightOfLabel) / 2) - 3; 

		if(secondLineGlyphs == null)
			g.drawGlyphVector(firstLineGlyphs, (int)getVisibleLocalLeft() + UI.PADDING_WITHIN_EVENTS, y);
		else {
			
			g.drawGlyphVector(firstLineGlyphs, (int)getVisibleLocalLeft() + UI.PADDING_WITHIN_EVENTS + (int)(widthOfLabel - widthOfFirstLine) / 2, y - (int)(heightOfLabel / 2));
			g.drawGlyphVector(secondLineGlyphs, (int)getVisibleLocalLeft() + UI.PADDING_WITHIN_EVENTS + (int)(widthOfLabel - widthOfSecondLine) / 2, y);
			
		}
		
	}
	
	public int compareTo(EventView view) {
		
		int thisViewsEvent = explanation.getEventID();
		int otherViewsEvent = view.explanation.getEventID();
		
		// What if these are different views that refer to the same event? For example, a thread block
		// may start with the same event as the first block in the thread block? 
		// In this case, the compare to is based on the block depth of the event explanations.
		if(this != view && thisViewsEvent == otherViewsEvent)
			return explanation.getBlockDepth() - view.explanation.getBlockDepth();
		// Otherwise, its based on the temporal ordering of the explanation's events.
		else
			return thisViewsEvent - otherViewsEvent;
		
	}
		
	private boolean pointIsOverLabel(int x, int y) { return x < getLocalLeft() + widthOfLabel + 2 * UI.PADDING_WITHIN_EVENTS; }
	
	public boolean handleMouseDown(int localX, int localY, int mouseButton) { 

		if(!isHidden() && pointIsOverLabel(localX, localY)) {
			visualization.getVisualizationUI().setSelection(this, false, UI.CLICK_EVENT_UI);
			return true;
		}
		else return false;
		
	}

	public int getRow() { return getEventID() < 0 ? -1 : visualization.getRowForThread(visualization.getTrace().getThreadID(getEventID())); }
	
	public String toString() { return getClass().getSimpleName() + "(" + (getEventID() < 0 ? "" : visualization.getTrace().eventToString(getEventID())) + ")"; }
	
}