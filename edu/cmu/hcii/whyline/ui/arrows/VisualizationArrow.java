package edu.cmu.hcii.whyline.ui.arrows;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.source.TokenRange;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.qa.*;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class VisualizationArrow extends CausalArrowView {

	private final Visualization visualization;
	
	private final char shortcut;
	private  GlyphVector glyphs;
	private  Shape curve;
	private  Shape arrowhead;
	private Rectangle2D glyphBounds;
	private int ascent, lineHeight;
	
	private final EventView fromView, toView;
	
	private int fromX, fromY;
	private int toX, toY;
	private int labelX, labelY;
	
	public VisualizationArrow(Visualization viz, Explanation value, Explanation from, Explanation to, int dependencyNumber, Relationship relationship) {

		super(viz.getWhylineUI(), value, from, to, relationship, dependencyNumber);

		this.visualization = viz;
	
		this.shortcut = dependencyNumber < 1 ? UI.UP_WHITE_ARROW : Visualization.getCharacterShortcutForNumber(dependencyNumber);
		
		Trace trace = whylineUI.getTrace();
		
		int causeID = this.from.getEventID();
		EventKind kind = whylineUI.getTrace().getKind(causeID);

		fromView = viz.getViewOfExplanation(this.from);
		toView = viz.getViewOfExplanation(this.to);
				
	}

	public void layout() {

		Font font = UI.getSmallFont();
		Graphics2D g = (Graphics2D)whylineUI.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		glyphs = font.createGlyphVector(g.getFontRenderContext(), label);
		FontMetrics metrics = g.getFontMetrics(font);
		ascent = metrics.getAscent();
		lineHeight = (int) (metrics.getHeight() * 1.25);

		fromX = fromView.getGlobalSelectionPointX();
		fromY = (int) fromView.getGlobalTop();
		
		// If the from view is not visible, add some space to make the arrow originate from the center of the elision.
		if(fromView.isHidden() || fromView.ancestorIsCollapsed()) {
			fromX = (int) (fromView.getGlobalLeft() + UI.ELISION_PADDING / 2);
			fromY += (int) (fromView. getLocalHeight() / 2);
		}
		
		toX = (int) toView.getGlobalLeft();
		toY = (int) (toView.getGlobalTop() - (visualization.getNumberOfArrows() - dependencyNumber) * lineHeight);
		
		labelX = toX;
		labelY = toY - lineHeight / 2 + ascent;
				
		Rectangle2D glyphBoundsTemp = glyphs.getLogicalBounds();
		glyphBounds = new Rectangle2D.Double(labelX, labelY - ascent, glyphBoundsTemp.getWidth(), glyphBoundsTemp.getHeight());

		curve = Util.getCurve(fromX, fromY, toX, toY, -UI.PADDING_BETWEEN_EVENTS, -UI.MIN_THREAD_ROW_HEIGHT / 4, true);
		arrowhead = Util.getArrowhead(fromX, fromY, toX, toY, -UI.PADDING_BETWEEN_EVENTS, -UI.MIN_THREAD_ROW_HEIGHT / 4);
		
		Rectangle2D curveBounds = curve.getBounds2D();
		Rectangle2D arrowBounds = arrowhead.getBounds2D();

		Rectangle2D bounds = new Rectangle2D.Double();
		bounds.setRect(curveBounds);
		bounds.add(arrowBounds);
		bounds.add(glyphBounds);
		
		setLocalLeft(bounds.getMinX(), false);
		setLocalTop(bounds.getMinY(), false);
		setLocalWidth(bounds.getWidth(), false);
		setLocalHeight(bounds.getHeight(), false);
		
	}

	public boolean containsLocalPoint(int x, int y) {

		return curve.contains(x, y) || arrowhead.contains(x, y) || glyphBounds.contains(x, y);
	
	}

	public void paintAboveChildren(Graphics2D g) {

		g = (Graphics2D)g.create();
		
		g.setColor(UI.getControlTextColor());
		g.drawGlyphVector(glyphs, labelX, labelY);

		boolean selected = whylineUI.getArrowOver() == dependencyNumber;

		if(selected) {
			g.setColor(UI.getControlTextColor());
			g.drawLine(labelX, labelY + 2, (int) (labelX + glyphBounds.getWidth()), labelY + 2);
		}

		g.setColor(relationship.getColor(selected));
		g.setStroke(relationship.getStroke(selected));
		
		g.draw(curve);
		if(selected) {
			Rectangle2D bounds =arrowhead.getBounds(); 
			double scale = 1.5;
			int x = (int) (bounds.getMinX());
			int y = (int) (bounds.getCenterY());
			AffineTransform scaleT = new AffineTransform();
			scaleT.scale(scale, scale);
			scaleT.translate(-x, -y);
			Shape scaledArrowhead = scaleT.createTransformedShape(arrowhead);
			g.translate(x, y);
			g.fill(scaledArrowhead);
			g.translate(-x, -y);
			
		}
		else
			g.fill(arrowhead);
		
	}

	public List<TokenRange> getViableTargetTokenRanges() { return Collections.<TokenRange>emptyList(); }

}
