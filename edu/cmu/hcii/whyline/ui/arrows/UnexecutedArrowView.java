package edu.cmu.hcii.whyline.ui.arrows;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.whyline.bytecode.Branch;
import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.bytecode.Invoke;
import edu.cmu.hcii.whyline.qa.UnexecutedInstruction;
import edu.cmu.hcii.whyline.source.TokenRange;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.qa.AnswerUI;
import edu.cmu.hcii.whyline.ui.qa.Visualization;
import edu.cmu.hcii.whyline.ui.qa.VisualizationUI;
import edu.cmu.hcii.whyline.ui.source.FilesView;
import edu.cmu.hcii.whyline.ui.source.FilesView.ArrowBox;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class UnexecutedArrowView extends ArrowView {

	private final UnexecutedInstruction unexecuted;
	
	private final ArrowBox arrows;
	private final FilesView files;

	protected int left;
	protected int baseline;
	private final GlyphVector glyphs;
	protected final Rectangle2D glyphBounds;
	protected final int descent;

	protected TokenRange toRange;
	protected TokenRange fromRange;

	public UnexecutedArrowView(FilesView.ArrowBox arrows, UnexecutedInstruction cause, UnexecutedInstruction effect, int number) {
		
		super(arrows.getFilesView().getWhylineUI(), number);
		
		this.files = arrows.getFilesView();
		this.unexecuted = cause;
		
		cause.explain();
		
		Instruction inst = cause.getInstruction();
		
		String event = 
			inst instanceof Branch ? "the conditional on" :
			inst instanceof Invoke ? "the call to " + ((Invoke)inst).getJavaMethodName()+ "() on" :
			"";
		
		String label = "why didn't " + event + " " + inst.getLineNumber() + " execute?";
		
		this.arrows = arrows;

		Graphics2D g = (Graphics2D)whylineUI.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		glyphs = UI.getSmallFont().createGlyphVector(g.getFontRenderContext(), label);
		glyphBounds = glyphs.getLogicalBounds();
		descent = g.getFontMetrics(UI.getSmallFont()).getDescent();

		fromRange = cause.getInstruction().getFile().getTokenRangeFor(cause.getInstruction());
		toRange = effect.getInstruction().getFile().getTokenRangeFor(effect.getInstruction());
		
	}

	public final List<TokenRange> getViableTargetTokenRanges() { 
		
		List<TokenRange> ranges = new ArrayList<TokenRange>(2);
		ranges.add(toRange);
		ranges.add(fromRange);
		return ranges;

	}

	public boolean containsLocalPoint(int x, int y) {

		return x >= getLocalLeft() && x < getLocalRight() && y >= getLocalTop() && y <= getLocalBottom();
	
	}

	protected void clicked() {

		AnswerUI answerUI = whylineUI.getQuestionsUI().getAnswerUIVisible();
		if(answerUI != null) {

			VisualizationUI viz = answerUI.getSituationSelected().getVisualizationUI();
			if(viz != null) {
			
				viz.setSelection(viz.getVisualization().getUnexecutedInstructionView(unexecuted), true, "arrow");
				whylineUI.selectUnexecutedInstruction(unexecuted, true, "arrow");
				
			}
			
		}
		
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

	public void paintAboveChildren(Graphics2D g) {
		
		boolean selected = whylineUI.getArrowOver() == dependencyNumber;

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

			g.setColor(UI.ERROR_COLOR);
			Area toArea = files.outline(g, toRange);
			Area fromArea = files.outline(g, fromRange);
			
			if(toArea != null && fromArea != null) {

				Rectangle2D toBounds = toArea.getBounds2D();
				Rectangle2D fromBounds = fromArea.getBounds2D();				
				
				Line2D line = Util.getLineBetweenRectangleEdges(
						fromBounds.getMinX(), fromBounds.getMaxX(),
						fromBounds.getMinY(), fromBounds.getMaxY(), 
						toBounds.getMinX(), toBounds.getMaxX(),
						toBounds.getMinY(), toBounds.getMaxY()
				);

				int xOff = 0;
				int yOff = 0;
	
				Util.drawQuadraticCurveArrow(g, (int)line.getX1(), (int)line.getY1(), (int)line.getX2(), (int)line.getY2(), xOff, yOff, true, UI.SELECTED_STROKE);
				
			}
			
		}

	}
	
}
