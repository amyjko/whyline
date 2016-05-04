package edu.cmu.hcii.whyline.ui.io;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

import javax.swing.JPopupMenu;

import edu.cmu.hcii.whyline.qa.GraphicsMenuFactory;
import edu.cmu.hcii.whyline.qa.QuestionMenu;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.views.*;
import edu.cmu.hcii.whyline.io.*;

/**
 * An interactive history of a AWT Window's graphical output events.
 * 
 * @author Andrew J. Ko
 *
 */
public final class WindowView extends View {
	
	private final GraphicsUI graphicsUI;
	private final WhylineUI whylineUI;
	
	public static final int TITLE_HEIGHT = 20;
	
	public WhylineUI getWhylineUI() { return whylineUI; }
	
	private int currentEventID;

	private BufferedImage currentImage;
	
	private final WindowState windowState;
	
	public WindowView(GraphicsUI graphicsUI, WindowState windowState) {

		this.graphicsUI = graphicsUI;
		this.whylineUI = graphicsUI.getWhylineUI();
		this.windowState = windowState;

	}	

	private SortedSet<GraphicalEventAppearance> eventsUnderMouse = new TreeSet<GraphicalEventAppearance>();

	public SortedSet<GraphicalEventAppearance> getEventsUnderMouse() { return eventsUnderMouse; }
	public GraphicalEventAppearance getEventUnderMouse() { return eventsUnderMouse.size() > 0 ? eventsUnderMouse.last() : null; }

	public void setCurrentEventID(int newEventID) {
		
		currentEventID = newEventID;
		updateSize();
		
	}
	
	public void updateSize() {

		int maxWidth = windowState.getMaxWidth();
		int maxHeight = windowState.getMaxHeight();
		
		if(currentImage != null && (maxWidth > currentImage.getWidth() || maxHeight > currentImage.getHeight()))
			currentImage = null;
		
		if(currentImage == null)
			currentImage = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);

		Rectangle newSize = windowState.drawWindowAtEventID(currentEventID, currentImage);
		
		setLocalLeft(newSize.x, false);
		setLocalTop(newSize.y, false);
		setLocalWidth(newSize.width, false);
		setLocalHeight(newSize.height + TITLE_HEIGHT, false);

		repaint();
		
	}
	
	public void paintBelowChildren(Graphics2D g) {		

		g = (Graphics2D)g.create();

		g.setColor(UI.getPanelDarkColor());
		g.fillRoundRect((int)getLocalLeft(), (int)getLocalTop(), (int)getLocalWidth(), TITLE_HEIGHT * 2, UI.getRoundedness(), UI.getRoundedness());

		g.setColor(UI.getPanelLightColor());
		g.drawRoundRect((int)getLocalLeft(), (int)getLocalTop(), (int)getLocalWidth(), TITLE_HEIGHT * 2, UI.getRoundedness(), UI.getRoundedness());

		g.setColor(UI.getPanelTextColor());
		g.setFont(UI.getMediumFont().deriveFont(Font.BOLD));
		g.drawString(whylineUI.getTrace().getDescriptionOfObjectID(windowState.getWindowID()), (int)getLocalLeft() + 5, (int)getLocalTop() + TITLE_HEIGHT - 5);

		if(currentImage != null) {

			Graphics2D clipped = (Graphics2D)g.create();
			clipped.clipRect((int)getLocalLeft(), (int)getLocalTop() + TITLE_HEIGHT, (int)getLocalWidth() + 1, (int)getLocalHeight() + 1 - TITLE_HEIGHT);
			clipped.drawImage(currentImage, (int)getLocalLeft(), (int)getLocalTop() + TITLE_HEIGHT, null);
			
		}

		g.setColor(UI.getControlBorderColor());
		g.drawRect((int)getLocalLeft(), (int)(getLocalTop() + TITLE_HEIGHT), (int)getLocalWidth(), (int)(getLocalHeight() - TITLE_HEIGHT));

	}
		
	private boolean canUpdateSelection() {

		return !whylineUI.userIsAskingQuestion() && whylineUI.canAskOutputQuestions();

	}
	
	public WindowState getWindowState() { return windowState; }
	
	// A mouse moves over this window. How do we know what GraphicalOutputEvents are underneath it? We actually have no
	// model of the structure of the output, because we compute it on demand above. And it's stored temporally. So even if we
	// iterated through all of it, computing the global coordinates could be a pain. But that's the only way we'll know if the mouse
	// is over a given event. So instead, we'll do a pretend paint, and then cache all of the state for each event within the event
	// (including the global position of the painted object, the most recent color, shape, clip event, etc.).
	public boolean handleMouseMove(int x, int y) { 

		if(whylineUI.getVisualizationUIVisible() != null) return false;
		
		if(!canUpdateSelection()) return false;

		eventsUnderMouse = windowState.getRenderEventsAtLocationAfterEventID((int)(x - getLocalLeft()), (int)(y - getLocalTop() - TITLE_HEIGHT), currentEventID);
		
		if(!eventsUnderMouse.isEmpty()) {
			graphicsUI.setHighlight(getEventUnderMouse());
		}
		
		return !eventsUnderMouse.isEmpty(); 
	
	}

	public boolean handleMouseDrag(int x, int y, int button) {
		
		setLocalLeft(x - getContainer().getMouseFocusX(), false);
		setLocalTop(y - getContainer().getMouseFocusY(), false);
		repaint();
		
		return true;
		
	}

	public boolean handleMouseUp(int x, int y, int button) {
		
		getContainer().releaseMouseFocus();
		return true;
		
	}
	
	public boolean handleMouseClick(int localX, int localY, int mouseButton) { 

		// Not done loading?
		if(!whylineUI.getTrace().isDoneLoading()) return false;
		
		// Already showing an answer?
		if(whylineUI.getVisualizationUIVisible() != null) return false;

		// Not the whyline?
		if(!whylineUI.isWhyline()) return false;
		
		if(localX >= getLocalLeft() && localX <= getLocalRight() && localY >= getLocalTop() && localY <= getLocalTop() + TITLE_HEIGHT) {
			
			bringToFront();
			getContainer().focusMouseOn(this);
			return true;
			
		}
		
		if(!canUpdateSelection()) return false;

		if(mouseButton != 1) return false;

		java.awt.geom.Point2D point = localToGlobal(new java.awt.geom.Point2D.Double(localX - getLocalLeft(), localY - getLocalTop()));

		// The lower time limit is the time of the most recent non-negligible input event.
		QuestionMenu questionMenu = GraphicsMenuFactory.getQuestionMenu(whylineUI, getEventsUnderMouse());
		
		if(questionMenu == null) return false;
		
		JPopupMenu menu = questionMenu.generatePopupMenu();
		return getContainer().showPopup(menu, (int)point.getX(), (int)point.getY());
		
	}
	
	public void handleMouseExit() {

		if(!canUpdateSelection()) return;

		eventsUnderMouse.clear();
		repaint();
		
	}

}