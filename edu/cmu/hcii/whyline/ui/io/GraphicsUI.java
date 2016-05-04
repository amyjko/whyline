package edu.cmu.hcii.whyline.ui.io;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.*;

import edu.cmu.hcii.whyline.io.*;
import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.trace.nodes.ObjectState;
import edu.cmu.hcii.whyline.ui.*;
import edu.cmu.hcii.whyline.ui.views.*;
import edu.cmu.hcii.whyline.util.Named;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class GraphicsUI extends DynamicComponent implements UserTimeListener {

	private final WhylineUI whylineUI;

	private static final int SPACE_BETWEEN_WINDOWS = 10;
	
	private int selectedScale = 100;
	
	private final ArrayList<WindowView> windowViews = new ArrayList<WindowView>();
	
	private Named selection = null;
	
	private RenderEvent renderEventUnderMouse;
	private MouseStateInputEvent latestInput;

	private WindowView selectionWindow;
	private Shape selectionShape;
	private boolean selectionIsText;
	
	private final ChangeListener menuChangeListener;
	
	private final View windows = new View() {
		
		public void paintBelowChildren(Graphics2D g) {

			g.setColor(UI.getControlCenterColor().darker());
			for(int x = selectedScale; x < getLocalWidth(); x+= selectedScale)
				g.drawLine(x, 0, x, (int)getLocalBottom());
			for(int y = selectedScale; y < getLocalHeight(); y+= selectedScale)
				g.drawLine(0, y, (int)getLocalRight(), y);
			
		}
		
		// Paint the selections and the input
		public void paintAboveChildren(Graphics2D g) {

			g = (Graphics2D)g.create();

			double scale = getScale();
			g.scale(scale, scale);

			Graphics2D selectionG = (Graphics2D)g.create();
			
			// Draw the selected shape
			if(selectionShape != null && selectionWindow != null) {

				Rectangle shapeBounds = selectionShape.getBounds();
				
				// This may be an event that was drawn much earlier in some other buffer. If so, we have to translate it to the proper position.
				if(renderEventUnderMouse != null && renderEventUnderMouse.getRenderers() != null) {

					java.util.SortedSet<DrawImageEvent> drawImagesSkipped = renderEventUnderMouse.getGraphicsState().getWindowState().getDrawImagesSkipped();
					if(drawImagesSkipped.size() > 0) {
						DrawImageEvent drawImage = drawImagesSkipped.first();
						drawImage.transformContextToDrawPrimitive(selectionG);
					}
					
				}

				selectionG.setColor(UI.getHighlightColor());
				selectionG.setStroke(selectionIsText ? new BasicStroke(2.0f) : UI.SELECTED_STROKE);
				int windowX = (int) selectionWindow.getLocalLeft();
				int windowY = (int) (selectionWindow.getLocalTop() + WindowView.TITLE_HEIGHT);
				selectionG.translate(windowX, windowY);
				
				selectionG.draw(selectionShape);
				
			}
			
			// Draw the most recent mouse cursor position.
			MouseStateInputEvent latestMouseEvent = whylineUI.getTrace().getMouseHistory().getMostRecentBeforeTime(whylineUI.getInputEventID());
			if(latestMouseEvent != null) {

				long source = latestMouseEvent.getSource();
				WindowView window = getViewOfWindowID(source);
				
				// Draw the latest mouse cursor position on top of the window.
				int mouseX = latestMouseEvent.getX() + (window == null ? 0 : (int)window.getLocalLeft());
				int mouseY = latestMouseEvent.getY() + (window == null ? 0 : (int)window.getLocalTop());

				g.translate(mouseX, mouseY);
				g.setColor(UI.getHighlightColor());
				g.fill(cursor);
				g.setColor(UI.getHighlightTextColor());
				g.draw(cursor);
				g.translate(-mouseX, -mouseY);

			}
			
			if(whylineUI.canAskOutputQuestions() && whylineUI.getVisualizationUIVisible() == null) {
				
				IOEvent ioEvent = whylineUI.getEventAtInputTime(); 

				String label = ioEvent == null ? "after the program started" : "after this " + ioEvent.getHTMLDescription() + "...";
				
				if(whylineUI.getInputEventID() == 0) {
					
					Util.drawCallout(g, null, label, 0, 0);
					
				}
				else if(ioEvent instanceof GetGraphicsOutputEvent) {

					long windowID = ((GetGraphicsOutputEvent)ioEvent).getObjectID();
					
					WindowView view = getViewOfWindowID(windowID);

					if(view != null) {
					
						Rectangle2D bounds = Util.getStringBounds(g, UI.getSmallFont(), label);
						int x = (int) (view.getLocalLeft());
						int y = (int) (view.getLocalBottom());
						
						Util.drawCallout(g, UI.REPAINT_ICON, label, x, y);
						
					}
					
				}
				else if(ioEvent instanceof KeyStateInputEvent) {
					
					KeyStateInputEvent keyEvent = (KeyStateInputEvent)ioEvent;
					
					long source = keyEvent.getSource();
					WindowView window = getViewOfWindowID(source);
					
					Rectangle2D bounds = Util.getStringBounds(g, UI.getSmallFont(), label);
					int x = window == null ? 0 : (int)window.getLocalLeft();
					int y = window == null ? 0 : (int)window.getLocalBottom();
					
					Util.drawCallout(g, 
							keyEvent.getType() == java.awt.event.KeyEvent.KEY_PRESSED ? 
								UI.KEY_DOWN_ICON : 
								UI.KEY_UP_ICON, 
							label, x, y);
					
				}
				else if(ioEvent instanceof MouseStateInputEvent) {

					MouseStateInputEvent mouseEvent = ((MouseStateInputEvent)ioEvent);
					
					long source = mouseEvent.getSource();
					WindowView window = getViewOfWindowID(source);
					
					int calloutX = window == null ? 0 : (int)window.getLocalLeft();
					int calloutY = window == null ? 0 : (int)window.getLocalBottom();
					
					ImageIcon iconToDraw = null;
					
					int state = mouseEvent.getType();
					
					if(state == MouseEvent.MOUSE_CLICKED)
						iconToDraw = UI.MOUSE_UP_ICON;
					else if(state == MouseEvent.MOUSE_DRAGGED)
						iconToDraw = UI.MOUSE_DRAG_ICON;
					else if(state == MouseEvent.MOUSE_PRESSED)
						iconToDraw = UI.MOUSE_DOWN_ICON;
					else if(state == MouseEvent.MOUSE_RELEASED)
						iconToDraw = UI.MOUSE_DOWN_ICON;
					else if(state == MouseEvent.MOUSE_MOVED || state == MouseEvent.MOUSE_ENTERED || state == MouseEvent.MOUSE_EXITED)
						iconToDraw = UI.MOUSE_MOVE_ICON;
					else if(state == MouseEvent.MOUSE_WHEEL)
						iconToDraw = UI.MOUSE_WHEEL_ICON;
					else
						System.err.println("Not rendering " + state);
					
					Util.drawCallout(g, iconToDraw, label, calloutX, calloutY);
					
				}
				
			}

			if(getNumberOfChildren() == 0 && whylineUI.getTrace().isDoneLoading()) {
				
				g.setFont(UI.getLargeFont());
				g.setColor(UI.getControlTextColor());
				g.drawString("The program didn't produce graphical output", 10, g.getFontMetrics(UI.getLargeFont()).getHeight() + 10);
				
			}			
			
		}

		private double viewXOnDown, viewYOnDown;
		private double xOnDown, yOnDown;
		
		public boolean handleMouseDown(int localX, int localY, int mouseButton) {

			viewXOnDown = getViewportX();
			viewYOnDown = getViewportY();

			getContainer().focusMouseOn(this);

			xOnDown = localX - getViewportX();
			yOnDown = localY - getViewportY();			

			return true;
			
		}

		public boolean handleMouseUp(int localX, int localY, int mouseButton) {
			
			getContainer().releaseMouseFocus();
			return true;
			
		}

		public boolean handleMouseDrag(int localX, int localY, int mouseButton) {

			double dx = xOnDown - (localX - getViewportX());
			double dy = yOnDown - (localY - getViewportY());

			setViewPosition((int)(viewXOnDown + dx), (int)(viewYOnDown + dy));

			repaint();
			return true;
			
		}

		public boolean handleWheelMove(int units) {
			
			whylineUI.setGraphicsScale(selectedScale - units);
			return true;
		
		}

		public void handleContainerResize() {
		
			updateScale();
						
		}
						
		public void handleMouseEnter() {
			
			setToolTipText(Tooltips.WINDOWS);
			
		}

		public boolean handleMouseMove(int x, int y) {
			
			removeHighlight();
			return true;

		}
		
		public boolean handleMouseClick(int x, int y, int button) {

			if(whylineUI.getVisualizationUIVisible() != null) {
				whylineUI.setQuestion(null);
				return true;
			}
			else if(whylineUI.isWhyline()) {
				
				QuestionMenu whyDidntMenu = new QuestionMenu(whylineUI, "Questions about graphics that classes didn't paint", "questions");
				QuestionMenu questionMenu = GraphicsMenuFactory.getWindowQuestions(whylineUI);
				whyDidntMenu.addMenu(questionMenu);
				
				JPopupMenu menu = whyDidntMenu.generatePopupMenu();
				return GraphicsUI.this.showPopup(menu, x, y);
				
			}
			else return true;
			
		}
		
	};
		
	public GraphicsUI(WhylineUI whylineUI, boolean isInput) {
		
		super(whylineUI, Sizing.SCROLL_OR_FIT_IF_SMALLER, Sizing.SCROLL_OR_FIT_IF_SMALLER);

		this.whylineUI = whylineUI;
		
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		setView(windows);

		// When a question is chosen, highlight graphics if we can.
		menuChangeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {

				MenuSelectionManager msm = (MenuSelectionManager)e.getSource();
                MenuElement[] path = msm.getSelectedPath();
                if(path.length > 0) {
                	
                	MenuElement last = path[path.length - 1];

                	// We want to ignore the popup menu that contains the menu with the subject.
                	if(last instanceof JPopupMenu && path.length - 2 >= 0) last = path[path.length - 2];

                	// What's the subject of the menu?
                	Object subject = null;
                	if(last instanceof QuestionMenu.Menu) subject = ((QuestionMenu.Menu)last).getSubject();
                	else if(last instanceof QuestionMenu.QuestionItem) subject = ((QuestionMenu.QuestionItem)last).getSubject();
                	
            		if(subject instanceof ObjectState)
            			setHighlight((ObjectState)subject);
            		else if(subject instanceof GraphicalEventAppearance)
            			setHighlight((GraphicalEventAppearance)subject);	
            		else if(subject instanceof RenderEvent)
            			setHighlight((RenderEvent)subject);	
            		
                }
                else removeHighlight();
				
			}
		};
		MenuSelectionManager.defaultManager().addChangeListener(menuChangeListener);
		
	}
	
	public void updateScale() {
		
		// Override selected scale if fit to window.
		if(whylineUI.isGraphicsFitToWindow())
			selectedScale = (int) ((100 * getViewportWidth()) / + windows.getRightmostChildsRight()); 
		else
			selectedScale = whylineUI.getGraphicsScale();
		
		double scale = getScale();
		windows.setPercentToScaleChildren(scale);
		windows.setPreferredSize(windows.getRightmostChildsRight() * scale, windows.getBottommostChildsBottom() * scale);
		
		updateSelectionState();

		repaint();
		
	}

	public void freeGlobalListeners() {
		
		MenuSelectionManager.defaultManager().removeChangeListener(menuChangeListener);
		
	}

	public void setHighlight(RenderEvent render) { setSelection(render); }
	public void setHighlight(GraphicalEventAppearance render) { setSelection(render); }
	public void setHighlight(ObjectState object) { setSelection(object); }
	public void removeHighlight() { setSelection(null); }
	
	/**
	 * Tells the graphics UI window to draw a highlight around the given object, if possible.
	 */
	private void setSelection(Named selection) {
		
		this.selection = selection;
		updateSelectionState();
		
	}
	
	private void updateSelectionState() {

		selectionShape = null;
		renderEventUnderMouse = null;
		// Find the object on screen.
		if(selection instanceof ObjectState) {

			for(WindowView window : windowViews) {
				
				if(window.getWindowState().getWindowID() == ((ObjectState)selection).getObjectID()) {
					
					selectionWindow = window;
					selectionShape = new Rectangle(0, 0, (int)window.getLocalWidth(), (int)window.getLocalHeight());
					
				}
				else {
					Rectangle rect = window.getWindowState().getEntityBoundsBefore(((ObjectState)selection).getObjectID(), whylineUI.getInputEventID());
					if(rect != null) {
						selectionShape = rect;
						selectionWindow = window;
						break;
					}
				}
			}
			
		}
		else if(selection instanceof GraphicalEventAppearance || selection instanceof RenderEvent) {
		
			RenderEvent render = selection instanceof GraphicalEventAppearance ? ((GraphicalEventAppearance)selection).renderer : (RenderEvent)selection;
			
			selectionShape = render.getUnclippedShape();
			selectionWindow = getViewOfWindowID(render.getGraphicsState().getWindowState().getWindowID());
			renderEventUnderMouse = render;
			
			selectionIsText = render instanceof DrawCharsEvent || render instanceof DrawCharacterSequenceEvent || render instanceof DrawStringEvent;

		}

		latestInput = whylineUI.getTrace().getMouseHistory().getMostRecentBeforeTime(whylineUI.getOutputEventID());

		repaint();

	}
	
	public void addWindowState(WindowState window) {

		WindowView newWindowView = new WindowView(this, window);
		
		windowViews.add(newWindowView);

		newWindowView.setLocalLeft(windowViews.size() * SPACE_BETWEEN_WINDOWS, false);
		newWindowView.setLocalTop(windowViews.size() * SPACE_BETWEEN_WINDOWS, false);

		windows.addChild(newWindowView);

		newWindowView.updateSize();
		
		windows.handleContainerResize();
		
	}
	
	public WhylineUI getWhylineUI() { return whylineUI; }
	
	// Ten pixels at a time.
	public int getVerticalScrollIncrement() { return 10; }
	public int getHorizontalScrollIncrement() { return 10; } 

	private double getScale() { 

		double chosenScale = selectedScale / 100.0;
		
		return Math.max(Math.min(chosenScale, 4.0), 0.1);
		
	}
		
	private void drawWindowsAtEventID(int eventID) {

		for(WindowView window : windowViews)
			window.setCurrentEventID(eventID);

		updateSelectionState();
		
	}

	public WindowView getViewOfWindowID(long windowID) {
		
		for(WindowView window : windowViews)
			if(window.getWindowState().getWindowID() == windowID) return window;
			
		return null;
		
	}
	
	public void inputTimeChanged(int time) { drawWindowsAtEventID(time); }
	public void outputTimeChanged(int time) {}

	private static final Polygon cursor = new Polygon();
	static {
		int RIGHT = 9;
		int BOTTOM = 15;
		cursor.addPoint((int)(RIGHT * 0.00), (int)(BOTTOM * 0.00));
		cursor.addPoint((int)(RIGHT * 1.00), (int)(BOTTOM * 0.60));
		cursor.addPoint((int)(RIGHT * 0.60), (int)(BOTTOM * 0.60));
		cursor.addPoint((int)(RIGHT * 0.85), (int)(BOTTOM * 0.95));
		cursor.addPoint((int)(RIGHT * 0.77), (int)(BOTTOM * 1.00));
		cursor.addPoint((int)(RIGHT * 0.60), (int)(BOTTOM * 1.00));
		cursor.addPoint((int)(RIGHT * 0.35), (int)(BOTTOM * 0.68));
		cursor.addPoint((int)(RIGHT * 0.00), (int)(BOTTOM * 0.90));
		cursor.addPoint((int)(RIGHT * 0.00), (int)(BOTTOM * 0.00));
	}

}