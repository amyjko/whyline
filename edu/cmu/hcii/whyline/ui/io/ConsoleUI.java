package edu.cmu.hcii.whyline.ui.io;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.hcii.whyline.io.*;
import edu.cmu.hcii.whyline.qa.QuestionMenu;
import edu.cmu.hcii.whyline.qa.TextMenuFactory;
import edu.cmu.hcii.whyline.ui.Tooltips;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.UserTimeListener;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.views.DynamicComponentWithSelection;
import edu.cmu.hcii.whyline.ui.views.View;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public final class ConsoleUI extends DynamicComponentWithSelection<TextualOutputEventView> implements UserTimeListener {

	private final WhylineUI whylineUI;

	private int margin = 4;

	private final Map<TextualOutputEvent,TextualOutputEventView> viewsByModel = new HashMap<TextualOutputEvent,TextualOutputEventView>(); 
	
	private View lines = new View() {

		public boolean handleMouseClick(int x, int y, int mouseButton) {

			if(!whylineUI.canAskOutputQuestions()) return false;
			
			if(whylineUI.getVisualizationUIVisible() != null) {
				whylineUI.setQuestion(null);
				return true;
			}
			
			java.awt.geom.Point2D point = localToGlobal(new java.awt.geom.Point2D.Double(x, y));
			
			QuestionMenu menu = TextMenuFactory.getTextualOutputMenu(whylineUI, null); 
			return showPopup(menu.generatePopupMenu(), (int)point.getX(), (int)point.getY());

		}
		
		public boolean handleMouseMove(int x, int y) {

			if(!canUpdateSelection()) return false;
			setSelection(null, false);
			
			return true;

		}
		
		public void handleMouseNoLongerDirectlyOver(int x, int y) {

			if(!canUpdateSelection()) return;
			setSelection(null, false);

		}

		public void handleMouseExit() {
			
			if(!canUpdateSelection()) return;
			setSelection(null, false);
			
		}
	
		public void paintAboveChildren(Graphics2D g) {

			if(!whylineUI.canAskOutputQuestions()) return;

			if(whylineUI.getVisualizationUIVisible() == null) {
			
				if(whylineUI.getInputEventID() == 0) {
	
					Util.drawCallout(g, null, "after the program started...", 0, 0);
	
				}
				else if(whylineUI.getEventAtInputTime() instanceof TextualOutputEvent) {
	
					TextualOutputEventView view = viewsByModel.get(whylineUI.getEventAtInputTime());
					
					if(view != null) {
	
						int left = (int) view.getGlobalRight() + UI.getPanelPadding();
						int top = (int) view.getGlobalTop() - UI.getPanelPadding();
		
						Util.drawCallout(g, UI.CONSOLE_OUT_ICON, "after this was printed...", left, top);
						
					}

				}
				
			}
			
			if(getNumberOfChildren() == 0 && whylineUI.getTrace().isDoneLoading()) {
				
				g.setFont(UI.getLargeFont());
				g.setColor(UI.getControlTextColor());
				g.drawString("The program didn't produce textual output", 10, g.getFontMetrics(UI.getLargeFont()).getHeight() + 10);
				
			}			

		}
		
	};
	
	private int rightEdge, bottomEdge;
	
	public ConsoleUI(WhylineUI whylineUI) {

		super(whylineUI, Sizing.SCROLL_OR_FIT_IF_SMALLER, Sizing.SCROLL_OR_FIT_IF_SMALLER);

		this.whylineUI = whylineUI;
		
		setView(lines);

		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		
		setToolTipText(Tooltips.CONSOLE);
		
		setBackground(UI.getConsoleBackColor());
		
	}

	private int indexOfLastPrintParsed = -1;
	private TextualOutputEventView previousConsoleLine = null;
	
	private void parseOutputHistory() {

		Graphics g = whylineUI.getGraphics();
		if(g == null) return;
		FontMetrics metrics = g.getFontMetrics(UI.getFixedFont());
		IOHistory<TextualOutputEvent> output = whylineUI.getTrace().getPrintHistory();

		// Construct output history
		for(int i = indexOfLastPrintParsed + 1; i < output.getNumberOfEvents(); i++) {

			TextualOutputEvent event = output.getEventAtIndex(i);

			int newLinesLeft = previousConsoleLine == null ? UI.getBorderPadding() : (int)previousConsoleLine.getLocalRight();
			int newLinesTop = previousConsoleLine == null ? UI.getBorderPadding() : (int)previousConsoleLine.getLocalTop();

			// If the last string of tokens ends with a new line, then start this stream on the next line and at the margin
			if(previousConsoleLine != null && previousConsoleLine.getEvent().getStringPrinted().endsWith("\n")) {

				newLinesTop += previousConsoleLine.getLocalHeight();
				newLinesLeft = UI.getBorderPadding();
			
			}

			TextualOutputEventView newLine = new TextualOutputEventView(this, event, newLinesLeft, newLinesTop);
			
			viewsByModel.put(event, newLine);

			if(newLine.getLocalWidth() > rightEdge) rightEdge = (int)newLine.getLocalWidth();
			lines.addChild(newLine);
			
			previousConsoleLine = newLine;
			
		}
		
		// So that the scroll bars appear properly.
		int width = (int)lines.getRightmostChildsRight();
		int height = (int)lines.getBottommostChildsBottom();

		lines.setPreferredSize(width, height);
		
		int characterWidth = metrics.charWidth('e');
		
		indexOfLastPrintParsed = output.getNumberOfEvents() - 1;
		
	}
	
	public WhylineUI getWhylineUI() { return whylineUI; }

	public boolean canUpdateSelection() {
		
		return whylineUI.canAskOutputQuestions() && !whylineUI.userIsAskingQuestion() && whylineUI.getVisualizationUIVisible() == null;

	}
	
	private void updateToTime(int time) {
		
		synchronized(whylineUI.getTrace().getIOHistory()) {
					
			// If we haven't parsed, or the last print parsed happened before the time we're setting to, parse! 
			if(indexOfLastPrintParsed < 0 || whylineUI.getTrace().getIOHistory().getEventAtIndex(indexOfLastPrintParsed).getEventID() < time)
				parseOutputHistory();

		}

		View rightmostVisibleEvent = null;
		View lastVisibleEvent = null;
		
		// Hide any views that happened after the event we're setting to.
		for(View event : lines.getChildren()) {
			
			boolean shouldBeHidden = ((TextualOutputEventView)event).getEvent().getEventID() > time; 
			((TextualOutputEventView)event).setHidden(shouldBeHidden);
			if(!shouldBeHidden) {
				lastVisibleEvent = event;
				if(rightmostVisibleEvent == null || rightmostVisibleEvent.getLocalRight() < event.getLocalRight())
					rightmostVisibleEvent = event;
			}
			
		}
		
		lines.setPreferredSize(rightmostVisibleEvent == null ? 0 : rightmostVisibleEvent.getLocalRight(), lastVisibleEvent == null ? 0 : lastVisibleEvent.getLocalBottom());

		if(lastVisibleEvent != null) getViewport().scrollRectToVisible(lastVisibleEvent.getGlobalBoundaries());
		
	}
	
	public int getVerticalScrollIncrement() { return getGraphics().getFontMetrics(UI.getFixedFont()).getHeight(); }
	public int getHorizontalScrollIncrement() { return getGraphics().getFontMetrics(UI.getFixedFont()).charWidth(' '); } 
	
	public void inputTimeChanged(int time) { updateToTime(time); }
	public void outputTimeChanged(int time) { }

	public void handleNewSelection(TextualOutputEventView selection, boolean scroll, String ui) { 

		// Its extremely annoying to have the source file change on hovers...
//		if(selection != null)
//			whylineUI.selectEvent(selection.getEvent().getEventID(), false);
		
		repaint(); 
		
	}

}