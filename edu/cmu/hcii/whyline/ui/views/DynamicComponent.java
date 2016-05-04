package edu.cmu.hcii.whyline.ui.views;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

import javax.swing.*;

import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.components.WhylineControlBorder;
import edu.cmu.hcii.whyline.ui.components.WhylineScrollPane;

/**
 * Adjusts to its parent's size by scaling its view.
 * 
 * @author Andrew J. Ko
 * 
 */
public abstract class DynamicComponent extends JComponent implements ViewContainer {
	
	private Frame frame;
	
	private View view;
	
	private final ArrayList<View> viewsUnderMouse = new ArrayList<View>();
	
	private View mouseFocus;
	
	private boolean requestFocusOnClick, requestFocusOnEnter;
	
	private java.awt.event.InputEvent mostRecentInputEvent;

	private java.awt.event.MouseEvent mostRecentMoveEvent;

	private int mostRecentMouseX, mostRecentMouseY;
	
	private int mouseFocusX, mouseFocusY;
	
	private WhylineScrollPane scroller;
	
	protected final FitView container = new FitView();
	
	public enum Sizing { SCROLL, FIT, SCROLL_OR_FIT_IF_SMALLER }
	
	public final Sizing widthSizing, heightSizing;
	
	public DynamicComponent(Frame frame, Sizing width, Sizing height) {
		
		this.frame = frame;
		
		this.widthSizing = width;
		this.heightSizing = height;

		setBorder(new WhylineControlBorder());
		
		setRequestFocusOnClick(false);
		setRequestFocusOnEnter(false);
		
		setLayout(new BorderLayout() {
			public void layoutContainer(Container parent) {

				super.layoutContainer(parent);

				layoutView();
				
			}
		});
		
		setBackground(UI.getControlBackColor());
		
		scroller = new WhylineScrollPane(container, 
				heightSizing != Sizing.FIT ? JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED : JScrollPane.VERTICAL_SCROLLBAR_NEVER, 
				widthSizing != Sizing.FIT ? JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		add(scroller, BorderLayout.CENTER);

		addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				repaint();
			}
			public void focusLost(FocusEvent e) {
				repaint();
			}
		});

		addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				mostRecentInputEvent = e;
				if(getView() != null) getView().handleKeyPressed(e);
			}

			public void keyReleased(KeyEvent e) {
				mostRecentInputEvent = e;
				if(getView() != null) getView().handleKeyReleased(e);
			}
			
			public void keyTyped(KeyEvent e) {
				mostRecentInputEvent = e;
				if(getView() != null) getView().handleKeyTyped(e);
			}
		});
		
		scroller.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				
				mostRecentInputEvent = e;
				if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
					
					for(View view : viewsUnderMouse) {

						if(view.handleWheelMove(e.getUnitsToScroll())) {
							return;
						}
					
					}
					
				}
				
			}
		});

		View.addRoot(DynamicComponent.this);
	
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				View.removeRoot(DynamicComponent.this);
			}
		});
		
		validate();
		
	}
	
	public boolean isMetaDown() { return mostRecentInputEvent == null ? false : mostRecentInputEvent.isMetaDown(); }
	public boolean isControlDown() { return mostRecentInputEvent == null ? false : mostRecentInputEvent.isControlDown(); }
	public boolean isAltDown() { return mostRecentInputEvent == null ? false : mostRecentInputEvent.isAltDown(); }
	public boolean isShiftDown() { return mostRecentInputEvent == null ? false : mostRecentInputEvent.isShiftDown(); }

	public boolean isModifierDown() {
		
		return mostRecentInputEvent == null ? false :
			mostRecentInputEvent.isShiftDown() ||
			mostRecentInputEvent.isAltDown() ||
			mostRecentInputEvent.isControlDown() ||
			mostRecentInputEvent.isMetaDown();
		
	}
	
	private void layoutView() {
		
		container.handleSizeChanges(false, true);
		if(getView() != null)
			getView().handleContainerResize();
		container.handleSizeChanges(false, true);

	}
	
	public void setRequestFocusOnClick(boolean onClick) { 
		
		requestFocusOnClick = onClick; 
		setFocusable(requestFocusOnClick || requestFocusOnEnter);
		
	}
	public void setRequestFocusOnEnter(boolean onEnter) { 
		
		requestFocusOnEnter = onEnter; 
		setFocusable(requestFocusOnClick || requestFocusOnEnter);
		
	}
	
	public JScrollPane getScrollPane() { return scroller; }
	
	private void updateViewsUnderMouse(int globalX, int globalY) {

		mostRecentMouseX = globalX;
		mostRecentMouseY = globalY;
		
		if(getView() == null) return;
		
		View lastViewOnTop = viewsUnderMouse.isEmpty() ? null : viewsUnderMouse.get(0);

		Set<View> viewsUnderMousePreviously = new HashSet<View>(viewsUnderMouse);

		viewsUnderMouse.clear();
		getView().collectViewsUnderMouseFromBottomToTop(viewsUnderMouse, globalX, globalY, getView().getLocalBoundaries());

		View newViewOnTop = viewsUnderMouse.isEmpty() ? null : viewsUnderMouse.get(0);

		// The set difference between the views that we're under the mouse and the views that are now under the mouse,
		// all deserve mouseExit events.
		Set<View> viewsExited = new HashSet<View>(viewsUnderMousePreviously);
		viewsExited.removeAll(viewsUnderMouse);
		for(View exitedView : viewsExited)
			exitedView.handleMouseExit();
		
		// All of the views that are in the new list but not in the old one deserve mouse enter events.
		Set<View> viewsEntered = new HashSet<View>(viewsUnderMouse);
		viewsEntered.removeAll(viewsUnderMousePreviously);
		for(View enteredView : viewsEntered)
			enteredView.handleMouseEnter();

		if(newViewOnTop != lastViewOnTop) {
			
			if(lastViewOnTop != null) {
				View parent = lastViewOnTop.getParent();
				if(parent != null)
					lastViewOnTop.handleMouseNoLongerDirectlyOver((int)parent.globalLeftToLocal(mostRecentMouseX), (int)parent.globalTopToLocal(mostRecentMouseY));
				else 
					lastViewOnTop.handleMouseNoLongerDirectlyOver(mostRecentMouseX, mostRecentMouseY);
			}
			if(newViewOnTop != null) {
				View parent = newViewOnTop.getParent();
				if(parent != null)
					newViewOnTop.handleMouseDirectlyOver((int)parent.globalLeftToLocal(mostRecentMouseX), (int)parent.globalTopToLocal(mostRecentMouseY));
				else 
					newViewOnTop.handleMouseDirectlyOver(mostRecentMouseX, mostRecentMouseY);
			}
			
		}
		
	}

	public final View getView() { return view; }
	
	public void setView(View newView) {
		
		view = newView;
		container.setView(newView);
		
	}

	public void focusMouseOn(View view) { 
		
		assert mouseFocus == null : "Mouse already focused on " + mouseFocus;
		
		mouseFocus = view;

		mouseFocusX = (int)view.globalLeftToLocal(mostRecentMouseX);
		mouseFocusY = (int)view.globalTopToLocal(mostRecentMouseY);
		
	}

	public void releaseMouseFocus() {
		
		mouseFocus = null;
		
	}

	public boolean mouseIsFocused() { return mouseFocus != null; }
	
	public int getMouseFocusX() { return mouseFocusX; }
	public int getMouseFocusY() { return mouseFocusY; }
	
	public abstract int getVerticalScrollIncrement();
	public abstract int getHorizontalScrollIncrement();
	
	public boolean viewIsUnderMouse(View view) { return viewsUnderMouse.contains(view); }

	public List<View> getViewsUnderMouse() { return Collections.<View>unmodifiableList(viewsUnderMouse); }
	
	private boolean popupCanceled = false;
	
	public boolean showPopup(JPopupMenu menu, int x, int y) {

		if(menu.getSubElements().length == 0)
			return false;
		
		Point p = SwingUtilities.convertPoint(container, x, y, this);

		menu.show(this, (int)p.getX(), (int)p.getY());
		
		return true;
				
	}

	public void setCursor(Cursor cursor) {
		
		super.setCursor(cursor);
		
	}
	
	public int getMouseX() { return mostRecentMouseX; }
	public int getMouseY() { return mostRecentMouseY; }

	protected JViewport getViewport() { return getScrollPane().getViewport(); }

	public int getViewportWidth() { 
		
		JViewport viewport = getViewport();
		return viewport.getWidth(); 
		
	}
	
	public int getViewportX() {

		JViewport viewport = getViewport();
		return (int)viewport.getViewPosition().getX();
		
	}

	public int getViewportHeight() { 
		
		JViewport viewport = getViewport();
		return viewport.getHeight();
		
	}

	public int getViewportY() {
		
		JViewport viewport = getViewport();
		return (int)viewport.getViewPosition().getY();
		
	}
	
	public void setViewPosition(int x, int y) {
		
		validate();
		
		JViewport viewport = getViewport();
		viewport.setViewPosition(new Point(Math.max(x, 0), Math.max(y, 0)));
		
	}
	
	// Call when something under the mouse moves that depends on the relative mouse position
	public void moveMouseAgain() {
		
		container.moveMouseAgain();
		
	}
	
	public void invokeRunnableLater(Runnable runnable) {

		SwingUtilities.invokeLater(runnable);
		
	}
		
	public final void rootViewChangedSize() {
		
		container.handleSizeChanges(true, false);
		
	}
	
	protected class FitView extends JComponent implements Scrollable, MouseMotionListener, MouseListener {

		private View view;
		
		private int alreadyHandlingSizeChanges = 0;
		
		public FitView() {
			
			addMouseMotionListener(this);
			addMouseListener(this);
			
		}

		public boolean isBackgroundSet() { return true; }
		public Color getBackground() { return DynamicComponent.this.getBackground(); }
		
		public void mouseDragged(MouseEvent event) {

			mostRecentInputEvent = event;

			updateViewsUnderMouse(event.getX(), event.getY());
			
			if(mouseFocus != null) {
				
				Point2D point = event.getPoint();
				if(mouseFocus.getParent() != null) point = mouseFocus.getParent().globalToLocal(new Point2D.Double(event.getX(), event.getY()));
				mouseFocus.handleMouseDrag((int)point.getX(), (int)point.getY(), event.getButton());

			}
			else
				mouseMoved(event);
			
		}

		public void moveMouseAgain() { if(mostRecentMoveEvent != null) mouseMoved(mostRecentMoveEvent); }
		
		public void mouseMoved(MouseEvent event) {

			mostRecentInputEvent = event;
			mostRecentMoveEvent = event;

			updateViewsUnderMouse(event.getX(), event.getY());
			for(View view : viewsUnderMouse) {
				
				Point2D point = event.getPoint();
				if(view.getParent() != null) point = view.getParent().globalToLocal(new Point2D.Double(event.getX(), event.getY()));
				if(view.handleMouseMove((int)point.getX(), (int)point.getY())) return;
			
			}
			
		}
		
		public void mouseClicked(MouseEvent event) {
			
			mostRecentInputEvent = event;

			updateViewsUnderMouse(event.getX(), event.getY());

			for(View view : viewsUnderMouse) {

				// Localize the point if necessary
				Point2D point = event.getPoint();
				if(view.getParent() != null) point = view.getParent().globalToLocal(new Point2D.Double(event.getX(), event.getY()));
				
				// Was it double or single?
				if(event.getClickCount() > 1) {
					if(view.handleMouseDoubleClick((int)point.getX(), (int)point.getY(), event.getButton())) return;
				}
				else if(view.handleMouseClick((int)point.getX(), (int)point.getY(), event.getButton())) return;
			}

		}

		public void mouseEntered(MouseEvent event) { 

			mostRecentInputEvent = event;

			if(requestFocusOnEnter) DynamicComponent.this.requestFocusInWindow();
			
			updateViewsUnderMouse(event.getX(), event.getY()); 
			
		}

		public void mouseExited(MouseEvent event) {

			mostRecentInputEvent = event;

			updateViewsUnderMouse(event.getX(), event.getY());

		}

		public void mousePressed(MouseEvent event) {
			
			if(requestFocusOnClick) DynamicComponent.this.requestFocusInWindow();

			assert event.getButton() < 4 : "The MouseEvent getButton() protocol changed! It no longer returns a 0-3 button number!";

			updateViewsUnderMouse(event.getX(), event.getY());

			if(mouseFocus != null) {
				
				Point2D point = event.getPoint();
				if(mouseFocus.getParent() != null) point = mouseFocus.getParent().globalToLocal(new Point2D.Double(event.getX(), event.getY()));
				mouseFocus.handleMouseDown((int)point.getX(), (int)point.getY(), event.getButton());

			}
			else {

				for(View view : viewsUnderMouse) {
					
					Point2D point = event.getPoint();
					if(view.getParent() != null) point = view.getParent().globalToLocal(new Point2D.Double(event.getX(), event.getY()));
					if(view.handleMouseDown((int)point.getX(), (int)point.getY(), event.getButton())) return;
				
				}
			
			}
			
		}

		public void mouseReleased(MouseEvent event) {

			updateViewsUnderMouse(event.getX(), event.getY());

			if(mouseFocus != null) {
				
				Point2D point = event.getPoint();
				if(mouseFocus.getParent() != null) point = mouseFocus.getParent().globalToLocal(new Point2D.Double(event.getX(), event.getY()));
				mouseFocus.handleMouseUp((int)point.getX(), (int)point.getY(), event.getButton());

			}
			else {

				for(View view : viewsUnderMouse) {
					
					Point2D point = event.getPoint();
					if(view.getParent() != null) point = view.getParent().globalToLocal(new Point2D.Double(event.getX(), event.getY()));
					if(view.handleMouseUp((int)point.getX(), (int)point.getY(), event.getButton())) return;
				
				}
				
			}

		}

		public Dimension getPreferredScrollableViewportSize() { return null; }
		public boolean getScrollableTracksViewportHeight() { return false; }
		public boolean getScrollableTracksViewportWidth() { return false; }
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return orientation == SwingConstants.VERTICAL ? getVerticalScrollIncrement() : getHorizontalScrollIncrement(); } 
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return getScrollableUnitIncrement(visibleRect, orientation, direction); }

		public void setView(View newView) {
			
			if(this.view != null) this.view.setNotifyContainerOfSizeChanges(false);
			this.view = newView;
			this.view.setNotifyContainerOfSizeChanges(true);
			repaint();
			
			setBackground(null);
			setOpaque(false);

			handleSizeChanges(true, false);
			
		}
		
		public void handleSizeChanges(boolean viewChanged, boolean viewportChanged) {
			
			if(alreadyHandlingSizeChanges > 0) return;
			
			if(view != null) {

				double preferredViewWidth = view.getPreferredWidth();
				double preferredViewHeight = view.getPreferredHeight();
				
				double newViewWidth = view.getLocalWidth();
				double newViewHeight = view.getLocalHeight();

				int viewportWidth = getViewportWidth() - 1;
				int viewportHeight = getViewportHeight() - 1;
				
				boolean needHorizontalBar = preferredViewWidth > viewportWidth;
				boolean needVerticalBar = preferredViewHeight > viewportHeight;
				
				if(widthSizing == Sizing.FIT) {
					newViewWidth = viewportWidth - 2;
				}
				else if(widthSizing == Sizing.SCROLL) {
					newViewWidth = preferredViewWidth;
				}
				else if(widthSizing == Sizing.SCROLL_OR_FIT_IF_SMALLER) {
					if(preferredViewWidth < viewportWidth)
						newViewWidth = viewportWidth - (needVerticalBar ? scroller.getVerticalScrollBar().getWidth() + 2 : 2);
					else
						newViewWidth = preferredViewWidth;
				}
				
				if(heightSizing == Sizing.FIT) {
					newViewHeight = viewportHeight - 2;
				}
				else if(heightSizing == Sizing.SCROLL) {
					newViewHeight = preferredViewHeight;
				}
				else if(heightSizing == Sizing.SCROLL_OR_FIT_IF_SMALLER) {
					if(preferredViewHeight < viewportHeight)
						newViewHeight = viewportHeight - (needHorizontalBar ? scroller.getHorizontalScrollBar().getHeight() + 2 : 2);
					else
						newViewHeight = preferredViewHeight;
				}
				
				// We need to prevent these calls from reaching this method again.
				alreadyHandlingSizeChanges++;

				view.setLocalWidth(newViewWidth, false);
				view.setLocalHeight(newViewHeight, false);

				Dimension newSize = new Dimension((int)newViewWidth, (int)newViewHeight); 
				setPreferredSize(newSize);
				
				setSize(newSize);
				DynamicComponent.this.validate();

				alreadyHandlingSizeChanges--;

				repaint();
				
			}
			
		}
		
		public final void paintComponent(Graphics g) {
			
			Graphics2D g2d = (Graphics2D)g;

			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			
	        View view = getView();
			if(view != null) {
			
				view.paintBelowChildren(g2d);
				view.paint(g2d);		
				view.paintAboveChildren(g2d);
			
			}
			
		}
		
	}
	
}
