
package edu.cmu.hcii.whyline.ui.io;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.TimerTask;

import javax.swing.ImageIcon;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.io.*;
import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.*;
import edu.cmu.hcii.whyline.ui.qa.EventView;
import edu.cmu.hcii.whyline.ui.qa.VisualizationUI;
import edu.cmu.hcii.whyline.ui.views.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class TimeUI extends DynamicComponent implements UserQuestionListener, UserTimeListener, UserFocusListener {

	private final WhylineUI whylineUI; 
	
	private BufferedImage ioEventMarkers;
	
	private double progress = 0.0;

	private static final int BUTTON_PADDING = 6;
	
	private final IOButton repaint, move, drag, press, release, print, read, scroll, keyup, keydown;
	private IOButton selection = null;
	
	public TimeUI(WhylineUI whylineUI) {

		super(whylineUI, Sizing.FIT, Sizing.FIT);
		
		this.whylineUI = whylineUI;
						
		setView(controller);
		
		move = new IOButton("mouse move events", UI.MOUSE_MOVE_ICON, (UI.ICON_SIZE + BUTTON_PADDING) * 0);
		drag = new IOButton("mouse drag events", UI.MOUSE_DRAG_ICON, (UI.ICON_SIZE + BUTTON_PADDING) * 1);
		press = new IOButton("mouse press events", UI.MOUSE_DOWN_ICON, (UI.ICON_SIZE + BUTTON_PADDING) * 2);
		release = new IOButton("mouse release events", UI.MOUSE_UP_ICON, (UI.ICON_SIZE + BUTTON_PADDING) * 3);
		scroll = new IOButton("scroll wheel events", UI.MOUSE_WHEEL_ICON, (UI.ICON_SIZE + BUTTON_PADDING) * 4);

		keyup = new IOButton("key up events", UI.KEY_UP_ICON, (UI.ICON_SIZE + BUTTON_PADDING) * 5);
		keydown = new IOButton("key down events", UI.KEY_DOWN_ICON, (UI.ICON_SIZE + BUTTON_PADDING) * 6);

		repaint = new IOButton("repaint events", UI.REPAINT_ICON, (UI.ICON_SIZE + BUTTON_PADDING) * 7);

		print = new IOButton("print to console events", UI.CONSOLE_OUT_ICON, (UI.ICON_SIZE + BUTTON_PADDING) * 8);
		read = new IOButton("read from console events", UI.CONSOLE_IN_ICON, (UI.ICON_SIZE + BUTTON_PADDING) * 9);

		controller.addChild(move);
		controller.addChild(drag);
		controller.addChild(press);
		controller.addChild(release);
		controller.addChild(scroll);
		controller.addChild(keyup);
		controller.addChild(keydown);
		controller.addChild(repaint);
		controller.addChild(print);
		controller.addChild(read);
		
		updateSize();
		
		setToolTipText(whylineUI.isWhyline() ? Tooltips.TIME_CURSOR : 
			"<html>Represents the program execution, from start to finish.<br>Drag to see the state of program output at different times");
		
	}
		
	public void setProgress(double percent) {
		
		progress = percent;
		repaint();
		
	}
	
	private double convertEventIndexToPosition(long eventIndex) {
		
		int numberOfEvents = whylineUI.getTrace().getNumberOfEvents();
		return (((double)eventIndex / numberOfEvents) * controller.getLocalWidth());
		
	}
	
	private void redrawIOEventMarkers() {

		if(!whylineUI.getTrace().isDoneLoading()) return;
		
		ioEventMarkers = new BufferedImage((int)Math.max(1, controller.getLocalWidth()), (int)Math.max(1, controller.getLocalHeight()), BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g = (Graphics2D)ioEventMarkers.getGraphics();
		
		int verticalCenter = (int)controller.getLocalHeight() / 2; 
		int maxTime = whylineUI.getTrace().getNumberOfEvents();

		g.setColor(UI.getControlTextColor());
		
		for(IOEvent event : whylineUI.getTrace().getIOHistory()) {

			if(event.segmentsOutput() && ioEventIsOfDesiredType(event)) {
			
				int left = (int)convertEventIndexToPosition(event.getEventID());
				g.fillRect(left, verticalCenter, 2,2);
				
			}
			
		}

	}

	private boolean breakpointDebuggerIsRunning() { 
	
		return whylineUI.getMode() == WhylineUI.Mode.BREAKPOINT && whylineUI.getBreakpointDebugger().isRunning();
		
	}

	/**
	 * If an input is selected (I), scope to next output (R or T)
	 * If an output is selected (I), scope to next output (R or T)
	 */
	private void setInputOutputTimes(IOEvent inputEvent) {
				
		IOEvent outputEvent = determineSelectableIOEventAfter(inputEvent);
		
		whylineUI.setInputTime(inputEvent == null ? 0 : inputEvent.getEventID());
		// Always points to the last event in the trace.
		whylineUI.setOutputTime(whylineUI.getTrace().getNumberOfEvents() - 1);
		
	}

	/**
	 * Inputs are selectable (I)
	 * Outputs that are followed by outputs are selectable
	 * Outputs that are followed by input are NOT selectable
	 */
	private IOEvent determineSelectableIOEventAfter(IOEvent event) {

		if(event == whylineUI.getTrace().getIOHistory().getLastEvent()) return event;
		
		for(IOEvent io : whylineUI.getTrace().getIOHistory().getIteratorForEventsAfter(event))
			if(io.segmentsOutput() && ioEventIsOfDesiredType(io)) return io;
		
		return event;
		
	}
	
	private IOEvent determineSelectableIOEventAtOrBefore(IOEvent event) {

		if(event == null) return event;
		
		if(event.segmentsOutput() && ioEventIsOfDesiredType(event)) return event;
		
		for(IOEvent io : whylineUI.getTrace().getIOHistory().getIteratorForEventsBefore(event))
			if(io.segmentsOutput() && ioEventIsOfDesiredType(io)) return io;
		
		return null;
		
	}
	
	public void questionChanged(Question<?> question) { updateSize(); }
	
	public boolean isMinimized() {
		
		return !whylineUI.isWhyline() || whylineUI.getQuestionVisible() != null;

	}
	
	private void updateSize() {		
		
		setPreferredSize(new Dimension(0, isMinimized() ? UI.TIME_UI_HEIGHT / 3: UI.TIME_UI_HEIGHT));
		revalidate();
		
	}
		
	public void inputTimeChanged(int time) { 

		repaint();
	
	}
	public void outputTimeChanged(int time) { repaint(); }

	public int getHorizontalScrollIncrement() { return 0; }
	public int getVerticalScrollIncrement() { return 0; }
	
	public boolean ioEventIsOfDesiredType(IOEvent io) {

		if(selection == null) return true;
		
		if(io instanceof MouseStateInputEvent) {

			MouseStateInputEvent mouseEvent = (MouseStateInputEvent)io;
			int id = mouseEvent.getType();
			
			switch(id) {
				case MouseEvent.MOUSE_PRESSED :
					return selection == press;
				case MouseEvent.MOUSE_DRAGGED :
					return selection == drag;
				case MouseEvent.MOUSE_CLICKED :
				case MouseEvent.MOUSE_RELEASED :
					return selection == release;
				case MouseEvent.MOUSE_WHEEL :
					return selection == scroll;
				case MouseEvent.MOUSE_MOVED :
					return selection == move;
				default :
					return false;
			}

		}
		else if(io instanceof KeyStateInputEvent) {
			
			KeyStateInputEvent keyEvent = (KeyStateInputEvent)io;
			int type = keyEvent.getType();
			
			switch(type) {
			case KeyEvent.KEY_PRESSED :
				return selection == keydown;
			case KeyEvent.KEY_RELEASED :
				return selection == keyup;
			case KeyEvent.KEY_TYPED :
				return selection == keyup;
			default:
				return false;
			}
			
		}
		else if(selection == repaint) return io instanceof GetGraphicsOutputEvent;
		else if(selection == print) return io instanceof TextualOutputEvent;
		
		else return false;
		
	}

	public void selectButtonBasedOnTrace() {
		
		Trace trace = whylineUI.getTrace();
		
//		if(trace.getRenderHistory().getNumberOfEvents() > 0) {
//			selection = press;
//		}
//		else {
//			selection = print;
//		}
		selection = null;
		
		validate();
		repaint();
		
	}

	
	private final View controller = new View() {
		
		public boolean handleMouseDown(int localX, int localY, int mouseButton) { 

			getContainer().focusMouseOn(this);
			return handleMouseDrag(localX, localY, mouseButton);
		
		}

		public boolean handleMouseDrag(int localX, int localY, int mouseButton) { 

			if(breakpointDebuggerIsRunning()) return false;

			if(whylineUI.getMode() == WhylineUI.Mode.BREAKPOINT) {
				
				int time = (int)((localX / getLocalWidth()) * whylineUI.getTrace().getNumberOfEvents());
				whylineUI.setInputTime(time);
				whylineUI.getBreakpointDebugger().setPauseMode(true);
				
			}
			else {
			
				// Convert the position into a trace time, then find the IO event nearest it.
				int time = (int)((localX / getLocalWidth()) * whylineUI.getTrace().getNumberOfEvents());
				IOEvent io = whylineUI.getTrace().getIOHistory().getMostRecentBeforeTime(time);
	
				if(whylineUI.isQuestionVisible()) {
					Answer answer = whylineUI.getVisualizationUIVisible().getAnswer();
					if(io != null && answer != null) {
						Explanation explanation = answer.getExplanationFor(io.getEventID());
						answer.broadcastChanges();
						if(explanation != null)
							whylineUI.selectExplanation(explanation, false, UI.TIME_UI);
					}
				}
				else
					setInputOutputTimes(determineSelectableIOEventAtOrBefore(io));
				
			}
	
			return true; 
			
		}

		public boolean handleMouseUp(int localX, int localY, int mouseButton) { 

			getContainer().releaseMouseFocus();
			return true;
			
		}

		public boolean handleKeyPressed(KeyEvent e) {

			if(breakpointDebuggerIsRunning()) return false;
			
			Trace trace = whylineUI.getTrace();
			
			// Get the io event before the current one.
			if(e.getKeyCode() == KeyEvent.VK_LEFT) {
				
				// Get event before current 
				IOEvent ioEvent = whylineUI.getTrace().getIOHistory().getMostRecentBeforeTime(whylineUI.getInputEventID() - 1);
				setInputOutputTimes(determineSelectableIOEventAtOrBefore(ioEvent));
				return true;
				
			}
			// Get the io event after the current one.
			else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
				
				setInputOutputTimes(determineSelectableIOEventAfter(whylineUI.getEventAtInputTime()));
				return true;

			}
			else return false;
			
		}
		
		public void paintBelowChildren(Graphics2D g) {
			
			g = (Graphics2D)g.create();
			
			int left = (int) convertEventIndexToPosition(whylineUI.getInputEventID());
			int right = (int) convertEventIndexToPosition(whylineUI.getOutputEventID());
			
			int scopeLeft = left;
			int scopeRight = right;
	
			// If there's a question showing, show the scope used at the time of the question.
			if(whylineUI.isQuestionVisible()) {
				scopeLeft = (int) convertEventIndexToPosition(whylineUI.getQuestionVisible().getInputEventID());
				scopeRight = (int) convertEventIndexToPosition(whylineUI.getQuestionVisible().getOutputEventID());
			}
			
			int verticalCenter = (int)(getLocalHeight() / 2); 
			int boxWidth = (int)(getLocalHeight() / 4);
			
			Color lockedColor = Color.lightGray;
			Color draggingColor = Color.white;
			Color setColor = UI.getHighlightColor();
	
			Paint oldPaint = g.getPaint();
			Composite oldComposite = g.getComposite();
			
			if(whylineUI.getQuestionOver() != null) {

				Question<?> q = whylineUI.getQuestionOver();
				int x, w;
				if(q.isPhrasedNegatively()) {
					x = scopeLeft;
					w = scopeRight - scopeLeft;
				}
				else {
					x = 0;
					w = scopeLeft;
				}

				g.setColor(UI.getHighlightColor());
				g.fillRect(x, 0, w, (int)getLocalHeight());
				g.setColor(UI.getHighlightTextColor());
				
			}
			
			// Draw the time controller
			g.setColor(breakpointDebuggerIsRunning() ? UI.getRunningColor() : UI.getControlTextColor());
			g.fillRoundRect(scopeLeft, 0, 4, (int)getLocalHeight(), 5, 5);
			
			g.setComposite(oldComposite);
			g.setPaint(oldPaint);
							
			// If there is a question showing, show the eventID of the current selection.
			if(whylineUI.isQuestionVisible()) {

				g.setColor(UI.getControlTextColor());
				g.setStroke(UI.SELECTED_STROKE);
				g.drawLine(left, 0, left, (int)getLocalHeight());

				VisualizationUI viz = whylineUI.getVisualizationUIVisible();
				if(viz != null) {

					if(viz.getSelection() instanceof EventView) {
						
						int eventID = ((EventView)viz.getSelection()).getEventID();
						left = (int) convertEventIndexToPosition(eventID);
						
					}
					
				}
				
				g.setColor(UI.getHighlightColor());
				g.setStroke(UI.SELECTED_STROKE);
				g.drawLine(left, 0, left, (int)getLocalHeight());
				
			}
			
			// If there's no question showing, draw the description of the I/O event and the filter message.
			if(!isMinimized()) {

				g.setColor(UI.getControlTextColor());

				// Draw the event description
				int inputEventID = whylineUI.getInputEventID();
				IOEvent ioEvent = whylineUI.getEventAtInputTime();
				
				String eventDescription = 
					inputEventID == 0 ? 
						"program started..." : 
							(ioEvent == null ? "" : ioEvent.getHTMLDescription());

				boolean negative = false;
				if(whylineUI.getQuestionOver() != null) {
					negative = whylineUI.getQuestionOver().isPhrasedNegatively();
					eventDescription = (negative ? "after this " : "before this ") +  eventDescription + "...";
				}

				g.setFont(UI.getSmallFont());
				Rectangle2D bounds = g.getFontMetrics().getStringBounds(eventDescription, g);
				int labelX = 
					(int) (negative ? 
					scopeLeft - bounds.getWidth() - 10 :
					scopeLeft + 10);
				labelX = Math.max(0, labelX);
				labelX = (int) Math.min(labelX, (int)getLocalWidth() - bounds.getWidth() - 10);

				g.drawString(eventDescription, labelX, (int)getLocalBottom() - 10);
				
				
				// Draw the filter message
				int filterMessageX = (int) (getLastChild().getLocalRight() + UI.getPanelPadding()); 
				int filterMessageY = (int) (getLastChild().getLocalBottom()); 

				g.setColor(UI.getControlTextColor());
				g.setFont(UI.getSmallFont());
				g.drawString(selection == null ? "showing all i/o events" : "only showing " + selection.description, filterMessageX, filterMessageY);
				
			}

		}
		
		public void paintAboveChildren(Graphics2D g) {
				
			// Paint the i/o markers, but only in whyline mode
			if(whylineUI.isWhyline()) {
				if(ioEventMarkers == null) redrawIOEventMarkers();
				g.drawImage(ioEventMarkers, 0, 0, null);
			}
			
		}

		public void handleContainerResize() { redrawIOEventMarkers(); }

	};


	public void showEvent(int eventID) { 

		if(!whylineUI.isQuestionVisible()) return;
		
		// We only need to update this if the NEW most RECENT IO event is a DIFFERENT render event than the last one drawn.
		IOEvent e = whylineUI.getTrace().getIOHistory().getMostRecentBeforeTime(eventID);
		if(e instanceof RenderEvent) whylineUI.setInputTime(e == null ? 0 : e.getEventID());
		repaint(); 
		
	}
	public void showExplanation(Explanation subject) { showEvent(subject.getEventID()); }
	public void showFile(FileInterface subject) {}
	public void showInstruction(Instruction subject) {}
	public void showInstructions(Iterable<? extends Instruction> subject) {}
	public void showMethod(MethodInfo subject) {}
	public void showClass(Classfile subject) {}
	public void showUnexecutedInstruction(UnexecutedInstruction subject) {}

	private java.util.Timer flasher;
	private double flashingAmount = 0;
	private int flashingFrequency = 75;
	
	private double getPercentFlash() { return Math.abs(Math.sin(Math.PI * flashingAmount / 180.0 )); }
	
	public void startFlashingMessage() {

		if(flasher != null) return;
		flasher = new java.util.Timer("Flasher", true);
		flasher.schedule(new TimerTask() {
			public void run() {
				flashingAmount += 180 / (1000 / flashingFrequency);
				if(flashingAmount > 180)
					flashingAmount = 0;
				repaint();
			}
		}, 0, flashingFrequency);
		
	}
	
	public void stopFlashingMessage() {
		
		if(flasher != null) flasher.cancel();
		flasher = null;
		
	}
	
	private class IOButton extends View {
		
		private final ImageIcon icon;
		private final String description;
		
		public IOButton(String description, ImageIcon icon, int left) {

			this.description = description;
			this.icon = icon;
			
			setLocalLeft(left + BUTTON_PADDING, false);
			setLocalTop(BUTTON_PADDING, false);
			setLocalWidth(UI.ICON_SIZE, false);
			setLocalHeight(UI.ICON_SIZE, false);
			
		}
				
		public boolean handleMouseDown(int x, int y, int button) {
			
			if(isDisabled()) return false;
			
			if(selection == this) selection = null;
			else selection = this;
			redrawIOEventMarkers();
			repaint();
			return true;
			
		}
		
		private boolean isDisabled() { return whylineUI.isQuestionVisible() || !whylineUI.isWhyline(); }
		
		public void paintAboveChildren(Graphics2D g) {

			if(isDisabled()) return;
			
			Graphics2D scaled = (Graphics2D)g.create();

			// If something is selected and its not this, cross this out.
			if(selection != this) {
				scaled.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, UI.DESELECTED_ICON_TRANSPARENCY));
			}
			icon.paintIcon(TimeUI.this, scaled, (int)getVisibleLocalLeft(), (int)getVisibleLocalTop());

		}
		
	}

}