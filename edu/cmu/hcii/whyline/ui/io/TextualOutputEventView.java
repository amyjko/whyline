package edu.cmu.hcii.whyline.ui.io;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import edu.cmu.hcii.whyline.io.TextualOutputEvent;
import edu.cmu.hcii.whyline.qa.QuestionMenu;
import edu.cmu.hcii.whyline.qa.TextMenuFactory;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.views.View;

/**
 * @author Andrew J. Ko
 *
 */
public final class TextualOutputEventView extends View {

	private final ConsoleUI consoleUI;
	public final TextualOutputEvent event;
	private final ArrayList<String> lines = new ArrayList<String>(1);
	public int lineHeight, ascent;
	public boolean hidden = false;
	
	public TextualOutputEventView(ConsoleUI console, TextualOutputEvent event, int left, int top) {

		this.consoleUI = console;
		this.event = event;
		
		setLocalLeft(left, false);
		setLocalTop(top, false);
		
		Font font = UI.getFixedFont();
		Graphics2D g = (Graphics2D)consoleUI.getWhylineUI().getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		FontMetrics metrics = g.getFontMetrics(font);
		lineHeight = metrics.getHeight();
		ascent = metrics.getAscent();

		String linePrinted = event.getStringPrinted().replace("\t", "    ");
		String[] lineText = linePrinted.split("\n");
		if(linePrinted.equals("\n")) { lineText = new String[1]; lineText[0] = ""; }
		int width = 0;
		for(String line : lineText) {
			
			GlyphVector glyphs = font.createGlyphVector(g.getFontRenderContext(), line);
			lines.add(line);
			
			Rectangle2D bounds = glyphs.getLogicalBounds();
			int lineWidth = (int)bounds.getWidth();
			if(lineWidth > width) width = lineWidth;
			
		}

		setLocalWidth(width, false);
		setLocalHeight(lineHeight * lines.size(), false);
		
		lines.trimToSize();
		
	}
	
	private boolean isNotInteractive() { return hidden; } 
	
	public boolean handleMouseClick(int x, int y, int mouseButton) {

		if(!consoleUI.getWhylineUI().canAskOutputQuestions() || consoleUI.getWhylineUI().getVisualizationUIVisible() != null)
			return false;

		if(isNotInteractive())
			return false;
		
		consoleUI.setSelection(this, false);

		WhylineUI whylineUI = consoleUI.getWhylineUI();
		QuestionMenu menu = TextMenuFactory.getTextualOutputMenu(whylineUI, event);
		
		java.awt.geom.Point2D point = getParent().localToGlobal(new java.awt.geom.Point2D.Double(x, y));
		
		return getContainer().showPopup(menu.generatePopupMenu(), (int)point.getX(), (int)point.getY());

	}
	
	public boolean handleMouseMove(int x, int y) { 
		
		if(!consoleUI.canUpdateSelection()) return false;

		if(isNotInteractive()) return false;

		consoleUI.setSelection(this, false);

		return true;
		
	}
		
	public TextualOutputEvent getEvent() { return event; }

	public void setHidden(boolean hidden) {
		
		this.hidden = hidden;
		
	}
		
	public void paintBelowChildren(Graphics2D g) {

		if(hidden) return;
		
		g.setColor(UI.getConsoleTextColor());
		g.setFont(UI.getFixedFont());
		
		int y = (int)(getLocalTop() + ascent);
		for(String gv : lines) {
			
			g.drawString(gv, (int)getLocalLeft(), y);
			y += lineHeight;
			
		}
				
	}
	
	public void paintAboveChildren(Graphics2D g) {

		if(consoleUI.getSelection() == this || consoleUI.getWhylineUI().getEventAtInputTime() == event) {

			Stroke oldStroke = g.getStroke();
			g.setStroke(UI.SELECTED_STROKE);
			g.setColor(UI.getHighlightColor());
			g.drawRoundRect((int)getLocalLeft(), (int)getLocalTop(), (int)getLocalWidth() - 1, (int)getLocalHeight() - 1, 5, 5);
			g.setStroke(oldStroke);
			
		}

	}

}