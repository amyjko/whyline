package edu.cmu.hcii.whyline.ui.arrows;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.source.TokenRange;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.qa.Visualization;
import edu.cmu.hcii.whyline.ui.source.FilesView;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class FileCausalArrow extends CausalArrowView {

	private final FilesView.ArrowBox arrows;
	protected final FilesView files;

	protected int left;
	protected int baseline;
	private final GlyphVector glyphs;
	protected final Rectangle2D glyphBounds;
	protected final int descent;

	protected TokenRange toRange;
	protected TokenRange fromRange;

	public FileCausalArrow(FilesView.ArrowBox arrows, Explanation value, Explanation from, Explanation to, Relationship relationship, int number) {

		super(arrows.getFilesView().getWhylineUI(), value, from, to, relationship, number);

		this.arrows = arrows;
		this.files = arrows.getFilesView();
		
		Graphics2D g = (Graphics2D)whylineUI.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		glyphs = UI.getSmallFont().createGlyphVector(g.getFontRenderContext(), label);
		glyphBounds = glyphs.getLogicalBounds();
		descent = g.getFontMetrics(UI.getSmallFont()).getDescent();

	}
	
	public final List<TokenRange> getViableTargetTokenRanges() { 
		
		List<TokenRange> ranges = new ArrayList<TokenRange>(2);
		
		if(toRange != null && toRange.first != null)
			ranges.add(toRange);

		if(fromRange != null && fromRange.first != null)
			ranges.add(fromRange);
		
		return ranges;

	}

	public boolean containsLocalPoint(int x, int y) {

		return x >= getLocalLeft() && x < getLocalRight() && y >= getLocalTop() && y <= getLocalBottom();
	
	}

	public final void layout() {
		
		Visualization viz = whylineUI.getVisualizationUIVisible() == null ? null : whylineUI.getVisualizationUIVisible().getVisualization();
		if(viz == null) return;
		
		int lineHeight = (int) (glyphBounds.getHeight() * 1.25);
		baseline = (dependencyNumber + 2) * lineHeight;
		
		left = arrows.getPadding();
		
		setLocalLeft(left, false);
		setLocalTop(baseline + descent - glyphBounds.getHeight(), false);
		setLocalWidth(glyphBounds.getWidth(), false);
		setLocalHeight(lineHeight, false);
				
	}

	public final void paintAboveChildren(Graphics2D g) {

		boolean selected = whylineUI.getArrowOver() == dependencyNumber;

		// We use the file color because we painted the identifier color for the background.
		g.setColor(UI.getFileColor());
		g.drawGlyphVector(glyphs, left, baseline);

		if(selected) {
			
			// Move to the global coordinate system, the common coordinate system for tokens
			g = (Graphics2D)g.create();
			g.translate(-getGlobalLeft() + getLocalLeft(), -getGlobalTop() + getLocalTop());

			int padding = 1;

			// Get the boundaries of this label.
			int labelLeft = (int)getGlobalLeft() - padding;
			int labelRight = labelLeft + (int)glyphBounds.getWidth() + padding * 2;
			int labelTop = (int)getGlobalTop() - padding; 
			int labelBottom = labelTop + (int)getGlobalHeight() + padding * 2;

			paintSelectedArrow(g, labelLeft, labelRight, labelTop, labelBottom);
			
		}
		
	}
	
	protected abstract void paintSelectedArrow(Graphics2D g, int labelLeft, int labelRight, int labelTop, int labelBottom);

}
