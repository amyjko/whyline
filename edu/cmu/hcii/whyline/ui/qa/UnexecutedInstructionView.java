package edu.cmu.hcii.whyline.ui.qa;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.qa.UnexecutedInstruction;
import edu.cmu.hcii.whyline.qa.UnexecutedInstruction.Reason;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.views.View;
import edu.cmu.hcii.whyline.util.IntegerVector;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public final class UnexecutedInstructionView extends View {

	private final Visualization visualization;
	private final UnexecutedInstruction instructionNotExecuted;
	private int row, column;
	
	// A cached value that comes from the InstructionsNotExecutedView just before these views are painted.
	public boolean selectedOrPointedToFromSelection = false;
	
	private final float STROKE_WIDTH = 2;
	public static final int ICON_SIZE = 50;
	public static final int STANDARD_WIDTH = 150;
	public static int LABEL_HEIGHT = 0;

	public static GlyphVector CHECKMARK, XMARK, PARENS, NO, CALLERS, IF;
	public static Rectangle2D CHECKMARK_BOUNDS, XMARK_BOUNDS, PARENS_BOUNDS, NO_BOUNDS, CALLERS_BOUNDS, IF_BOUNDS;
	public static int VISIBLE_GLYPH_HEIGHT;
	public static int GLYPH_ASCENT;
	
	private final Stroke unselectedStroke = new java.awt.BasicStroke(STROKE_WIDTH, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
	private final Stroke selectedStroke = new java.awt.BasicStroke(STROKE_WIDTH * 4, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
	
	private GlyphVector classnameGlyphs, subjectGlyphs, eventGlyphs;
	private Rectangle2D classnameBounds, subjectBounds, eventBounds;
	
	public UnexecutedInstructionView(Visualization viz, UnexecutedInstruction i) {

		this.visualization = viz;
		this.instructionNotExecuted = i;

		update();
		
	}
	
	public void setGridLocation(int row, int column) {

		this.row = row;
		this.column = column;
		
	}
	
	public void update() {
		
		if(LABEL_HEIGHT == 0) {

			Graphics2D g = (Graphics2D)visualization.getWhylineUI().getGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
			LABEL_HEIGHT = g.getFontMetrics(UI.getSmallFont()).getHeight() * 3;
			
			Font full = UI.getSmallFont().deriveFont(36.0f);
			Font half = UI.getSmallFont().deriveFont(12.0f);

			NO = half.createGlyphVector(g.getFontRenderContext(), "NO");
			NO_BOUNDS = NO.getLogicalBounds();
			CALLERS = half.createGlyphVector(g.getFontRenderContext(), "CALLERS");
			CALLERS_BOUNDS = CALLERS.getLogicalBounds();
			IF = full.createGlyphVector(g.getFontRenderContext(), "if");
			IF_BOUNDS = IF.getLogicalBounds();
			CHECKMARK = full.createGlyphVector(g.getFontRenderContext(), String.valueOf(UI.CHECKMARK));
			CHECKMARK_BOUNDS = CHECKMARK.getLogicalBounds();
			XMARK = full.createGlyphVector(g.getFontRenderContext(), String.valueOf(UI.XMARK));
			XMARK_BOUNDS = XMARK.getLogicalBounds();
			PARENS = full.createGlyphVector(g.getFontRenderContext(), "()");
			PARENS_BOUNDS = PARENS.getLogicalBounds();
			
			VISIBLE_GLYPH_HEIGHT = (int) PARENS.getVisualBounds().getHeight();
			GLYPH_ASCENT = g.getFontMetrics(full).getAscent();
			
		}

		Instruction instruction = instructionNotExecuted.getInstruction();

		final String subjectLabel, eventLabel;
		String classnameLabel = Util.elide(instruction.getClassfile().getSimpleName(), 16);

		String lineLabel = "line " + instruction.getLineNumber().getNumber();
		String truncatedMethodName = Util.elide(instruction.getMethod().getJavaName(), 10) +"()";

		Reason reason = instructionNotExecuted.getReason();
		
		switch(reason) {
		
		case DID_EXECUTE : 
			
			subjectLabel = truncatedMethodName;
			eventLabel = "did execute";
			break;
		
		case INSTRUCTIONS_BRANCH_DID_NOT_EXECUTE :

			subjectLabel = lineLabel + "'s";
			eventLabel = (instructionNotExecuted.getDecidingInstructions().size() > 1 ? "conditionals" : "conditional") + " didn't execute";
			break;
			
		case UNREACHABLE : 

			subjectLabel = lineLabel + " didn't execute";
			eventLabel = truncatedMethodName + " has no known callers";
			break;

		case WRONG_WAY :

			subjectLabel = lineLabel + "'s";
			eventLabel = "conditional went wrong way";
			break;

		case METHOD_DID_NOT_EXECUTE :

			subjectLabel = lineLabel + " didn't execute";
			eventLabel = truncatedMethodName + " wasn't called"; 
			break;
			
		case EXCEPTION_CAUGHT :

			subjectLabel = truncatedMethodName;
			eventLabel = "exception thrown";
			break;

		default :

			subjectLabel = "line " + instruction.getLineNumber().getNumber() + " didn't execute";
			eventLabel = "unknown reason";

		}

		Graphics2D g = (Graphics2D)visualization.getWhylineUI().getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		FontRenderContext context = g.getFontRenderContext();
		
		classnameGlyphs = UI.getSmallFont().deriveFont(Font.ITALIC).createGlyphVector(context, classnameLabel);
		subjectGlyphs = UI.getSmallFont().deriveFont(Font.BOLD).createGlyphVector(context, subjectLabel);
		eventGlyphs = UI.getSmallFont().createGlyphVector(context, eventLabel);

		classnameBounds = classnameGlyphs.getLogicalBounds();
		subjectBounds = subjectGlyphs.getLogicalBounds();
		eventBounds = eventGlyphs.getLogicalBounds();
		
		setLocalWidth(STANDARD_WIDTH / 2, false);
		setLocalHeight(XMARK_BOUNDS.getHeight() + LABEL_HEIGHT, false);
		
	}

	public int getRow() { return row; }

	public int getColumn() { return column; }

	public UnexecutedInstruction getUnexecutedInstruction() { return instructionNotExecuted; }
	
	public Instruction getInstruction() { return instructionNotExecuted.getInstruction(); }
	
	public boolean handleMouseDown(int localX, int localY, int mouseButton) { 

		visualization.getVisualizationUI().setSelection(this, false, UI.CLICK_UNEXECUTED_UI);
		return true;
		
	}

	private static final AlphaComposite transparent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
	private static final AlphaComposite opaque = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
	
	public void paintBelowChildren(Graphics2D g) {

		g = (Graphics2D)g.create();

		boolean selected = visualization.getSelectedUnexecutedInstructionView() == this;
		
		///////// DRAW THE ARROWS
		
		// Draw a line to the decision event, if there is one.
		if(instructionNotExecuted.getReason() == UnexecutedInstruction.Reason.WRONG_WAY) {

			IntegerVector events = null;
			
			if(instructionNotExecuted.getDecidingEventID() >= 0) {
				events = new IntegerVector(1);
				events.append(instructionNotExecuted.getDecidingEventID());
			}
			else if(instructionNotExecuted.getDecidingEvents() != null)
				events = instructionNotExecuted.getDecidingEvents();

			if(events != null) {
			
				for(int i = 0; i < events.size(); i++) {
					
					EventView view = visualization.getViewOfEvent(events.get(i));
					if(view != null) {
	
						Point2D from = localToLocal(view, new Point2D.Double(view.getLocalRight(), view.getLocalTop() + view.getLocalHeight() / 2));
						g.setColor(UI.CONTROL_COLOR);
						
						Point2D to = Util.getIntersectionOfSegmentAndBox(
							getLocalHorizontalCenter(), getLocalTop() + ICON_SIZE / 2, 
							getLocalLeft(), getLocalTop(), getLocalRight(), getLocalTop() + ICON_SIZE,
							from.getX(), from.getY());
						
						Util.drawQuadraticCurveArrow(g, (int)from.getX(), (int)from.getY(), (int)to.getX(), (int)to.getY(), -10, -10, true, UI.UNSELECTED_STROKE);
					
					}
					else System.err.println("Couldn't find view of event ID " + events.get(i));
	
				}
				
			}
			
		}

		if(selected) {
			
			g.setColor(UI.ERROR_COLOR);
			g.setStroke(UI.UNSELECTED_STROKE);

			for(UnexecutedInstruction in : instructionNotExecuted.getIncoming()) {

				UnexecutedInstructionView view = visualization.getUnexecutedInstructionView(in);
				if(view == null) {}
				else if(view != this) {

					Point2D from = localToLocal(view, new Point2D.Double(view.getLocalHorizontalCenter(), view.getLocalVerticalCenter()));
					from = Util.getIntersectionOfSegmentAndBox(
						from.getX(), from.getY() - view.getLocalHeight() / 2 + ICON_SIZE / 2, 
						from.getX() - view.getLocalWidth() / 2, from.getY() - view.getLocalHeight() / 2, from.getX() + view.getLocalWidth() / 2, from.getY() + ICON_SIZE / 2,
						getLocalHorizontalCenter(), getLocalVerticalCenter());

					Point2D to = Util.getIntersectionOfSegmentAndBox(
						getLocalHorizontalCenter(), getLocalTop() + ICON_SIZE / 2, 
						getLocalLeft(), getLocalTop(), getLocalRight(), getLocalTop() + ICON_SIZE,
						from.getX(), from.getY());

					Util.drawQuadraticCurveArrow(g, (int)from.getX(), (int)from.getY(), (int)to.getX(), (int)to.getY(), -10, -10, true, UI.SELECTED_STROKE);

				}
				
			}
			
			g.setComposite(transparent);

			for(UnexecutedInstruction out : instructionNotExecuted.getOutgoing()) {
				
				UnexecutedInstructionView view = visualization.getUnexecutedInstructionView(out);
				if(view == null) {}
				else if(view != this) {

					Point2D to = localToLocal(view, new Point2D.Double(view.getLocalHorizontalCenter(), view.getLocalVerticalCenter()));
					to = Util.getIntersectionOfSegmentAndBox(
						to.getX(), to.getY() - view.getLocalHeight() / 2 + ICON_SIZE / 2, 
						to.getX() - view.getLocalWidth() / 2, to.getY() - view.getLocalHeight() / 2, to.getX() + view.getLocalWidth() / 2, to.getY() + ICON_SIZE / 2,
						getLocalHorizontalCenter(), getLocalVerticalCenter());

					Point2D from = Util.getIntersectionOfSegmentAndBox(
							getLocalHorizontalCenter(), getLocalTop() + ICON_SIZE / 2, 
							getLocalLeft(), getLocalTop(), getLocalRight(), getLocalTop() + ICON_SIZE,
							to.getX(), to.getY());

					Util.drawQuadraticCurveArrow(g, (int)from.getX(), (int)from.getY(), (int)to.getX(), (int)to.getY(), -10, -10, true, UI.UNSELECTED_STROKE);

				}
				
			}

		}
		
		int middle = (int)(getVisibleLocalLeft() + getVisibleLocalWidth() / 2);

		double heightOfLabels = eventBounds.getHeight() + subjectBounds.getHeight() + classnameBounds.getHeight();

		int x1 = (int)(middle - ICON_SIZE / 2 + STROKE_WIDTH);
		int x2 = (int)(middle + ICON_SIZE / 2 - STROKE_WIDTH);
		int y1 = (int)(getVisibleLocalTop());
		int y2 = (int)(getVisibleLocalTop() + ICON_SIZE);

		int width = (int) getVisibleLocalWidth();
		
		if(selected) {
			g.setComposite(opaque);
			g.setStroke(UI.SELECTED_STROKE);
			g.setColor(UI.getHighlightColor());
			g.drawRoundRect((int)getVisibleLocalLeft(), (int)getVisibleLocalTop(), (int)getVisibleLocalWidth(), ICON_SIZE, UI.getRoundedness(), UI.getRoundedness()); 
			g.setStroke(UI.UNSELECTED_STROKE);			
		}
		else {
			g.setComposite(transparent);
		}

		////////// DRAW THE LABELS
		
		switch(instructionNotExecuted.getReason()) {

			case DID_EXECUTE :

				g.setColor(UI.CORRECT_COLOR);
				g.drawGlyphVector(CHECKMARK, getCenterXOf(CHECKMARK_BOUNDS), y2);
				break;
			
			case INSTRUCTIONS_BRANCH_DID_NOT_EXECUTE :

				g.setColor(UI.getControlTextColor());
				g.drawGlyphVector(IF, (int)getLocalHorizontalCenter(), getCenterYOf(IF_BOUNDS));
				g.setColor(Color.red);
				g.drawGlyphVector(XMARK, (int)(getLocalHorizontalCenter() - XMARK_BOUNDS.getWidth()), getCenterYOf(XMARK_BOUNDS));
				break;
				
			case UNREACHABLE :
				
				int lineHeight = (int) NO_BOUNDS.getHeight();
				g.setColor(Color.red);
				g.drawGlyphVector(NO, getCenterXOf(NO_BOUNDS), getCenterYOf(NO_BOUNDS) - lineHeight);
				g.drawGlyphVector(CALLERS, getCenterXOf(CALLERS_BOUNDS), getCenterYOf(NO_BOUNDS));
				break;
				
			case METHOD_DID_NOT_EXECUTE :
				
				g.setColor(UI.getControlTextColor());
				g.drawGlyphVector(PARENS, (int)getLocalHorizontalCenter(), getCenterYOf(PARENS_BOUNDS));
				g.setColor(Color.red);
				g.drawGlyphVector(XMARK, (int)(getLocalHorizontalCenter() - XMARK_BOUNDS.getWidth()), getCenterYOf(XMARK_BOUNDS));
				break;
				
			case WRONG_WAY :

				g.setColor(UI.getControlTextColor());
				g.drawGlyphVector(IF, getCenterXOf(IF_BOUNDS), getCenterYOf(IF_BOUNDS));
				g.setColor(Color.red);
				g.drawGlyphVector(XMARK, getCenterXOf(XMARK_BOUNDS) + (int)IF_BOUNDS.getWidth(), getCenterYOf(XMARK_BOUNDS));
				g.setColor(Color.green);
				g.drawGlyphVector(CHECKMARK, getCenterXOf(CHECKMARK_BOUNDS) - (int)IF_BOUNDS.getWidth(), getCenterYOf(CHECKMARK_BOUNDS));
				break;

			case EXCEPTION_CAUGHT :

				g.setColor(Color.red);
				g.drawGlyphVector(XMARK, (int)(getLocalHorizontalCenter() - XMARK_BOUNDS.getWidth() / 2), getCenterYOf(XMARK_BOUNDS));
				break;
				
			default :
				
		}

		int labelX = (int)(getVisibleLocalLeft() + (getVisibleLocalWidth() - subjectBounds.getWidth()) / 2);
		int labelY = (int)(getVisibleLocalTop() + ICON_SIZE + subjectBounds.getHeight() + 5);

		int classnameX = (int)(getVisibleLocalLeft() + (getVisibleLocalWidth() - classnameBounds.getWidth()) / 2);
		
		int lineX = (int)(getVisibleLocalLeft() + (getVisibleLocalWidth() - eventBounds.getWidth()) / 2);

		g.setColor(UI.getControlTextColor());
		g.drawGlyphVector(classnameGlyphs, classnameX, labelY);
		g.drawGlyphVector(subjectGlyphs, labelX, labelY + (int)subjectBounds.getHeight());
		g.drawGlyphVector(eventGlyphs, lineX, labelY + (int)subjectBounds.getHeight() + (int)eventBounds.getHeight());
				
	}
	
	private int getCenterXOf(Rectangle2D glyphBounds) { return (int)(getVisibleLocalLeft() + (getVisibleLocalWidth() - glyphBounds.getWidth()) / 2); }
	private int getCenterYOf(Rectangle2D glyphBounds) { return (int)(getVisibleLocalTop() + ICON_SIZE - (ICON_SIZE - VISIBLE_GLYPH_HEIGHT) / 2); }
	
	private int getXOnTheRight() { return (int)(getVisibleLocalRight() - getVisibleLocalWidth() / 2 + PARENS_BOUNDS.getWidth() / 2); }
	private int getYOnTheRight() { return (int)(getVisibleLocalTop() + ICON_SIZE / 2); }

	private int getXOnTheLeft() { return (int)(getVisibleLocalLeft() + getVisibleLocalWidth() / 4 - UI.ARROWHEAD_WIDTH); }
	private int getYOnTheLeft() { return (int)(getCenterYOf(XMARK_BOUNDS) - XMARK_BOUNDS.getHeight() / 4); }
	
}