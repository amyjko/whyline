package edu.cmu.hcii.whyline.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import edu.cmu.hcii.whyline.io.*;
import edu.cmu.hcii.whyline.ui.events.*;
import edu.cmu.hcii.whyline.ui.qa.*;
import edu.cmu.hcii.whyline.ui.views.View;

/**
 * @author Andrew J. Ko
 *
 */
public class Actions {

	private final WhylineUI whylineUI;

	public final LoggedAction stepOver;
	public final LoggedAction stepInto;
	public final LoggedAction stepOut;
	public final LoggedAction stop;
	public final LoggedAction runToBreakpoint;
	public final LoggedAction clearBreakpoints;

	public final LoggedAction replayForSlicer;
	
	public final LoggedAction goToPreviousEvent;
	public final LoggedAction goToNextEvent;
	public final LoggedAction goToPreviousBlock;
	public final LoggedAction goToNextBlock;

	public final LoggedAction collapseBlock;
	public final LoggedAction addToExplanation;

	public final Action queryTrace;

	public final Action showHideThreads;
	
	public Actions(WhylineUI ui) {
		
		this.whylineUI = ui;
						
		stepOver = new LoggedAction(whylineUI, "over") { 
			protected AbstractUIEvent<?> act() { 
				whylineUI.getBreakpointDebugger().stepOver(); 
				return null;
			}
		};
		
		stepInto = new LoggedAction(whylineUI, "into") { 
			protected AbstractUIEvent<?> act() { 
				whylineUI.getBreakpointDebugger().stepInto(); 
				return null;
			}
		};
		
		stepOut = new LoggedAction(whylineUI, "out") { 
			protected AbstractUIEvent<?> act() { 
				whylineUI.getBreakpointDebugger().stepOut(); 
				return null;
			}
		};
		
		runToBreakpoint = new LoggedAction(whylineUI, "run") { 
			protected AbstractUIEvent<?> act() { 
				whylineUI.getBreakpointDebugger().runToBreakpoint(); 
				return null;
			}
		};
		
		stop = new LoggedAction(whylineUI, "stop") {
			protected AbstractUIEvent<?> act() { 
				whylineUI.getBreakpointDebugger().stop();
				return null;
			}
		};
		
		clearBreakpoints = new LoggedAction(whylineUI, "clear all breakpoints") {
			protected AbstractUIEvent<?> act() {
				whylineUI.getBreakpointDebugger().clearBreakpointsAndPrints();
				return new Note("cleared breakpoints");
			}
			public AbstractUIEvent<?> getLogEvent() { return null; }
		};

		replayForSlicer = new LoggedAction(whylineUI, "replay") {

			private boolean playing = false;
			private boolean stop = false;
			private Thread playbackThread;
		
			protected AbstractUIEvent<?> act() {

				if(playing) {
					stop = true;
					return null;
				}
				
				replayForSlicer.putValue(Action.NAME, "stop");
				playing = true;
				
				whylineUI.arrangeForPlayback();

				playbackThread = new Thread() {
					public void run() {

						final IOHistory<? extends IOEvent> history = whylineUI.getTrace().getMouseHistory();
						
						try {

							for(final IOEvent io : history) {
								if(stop) {
									stop = false;
									break;
								}
								SwingUtilities.invokeAndWait(new Runnable() {
									public void run() {
										
										int time = io.getEventID();
										float percent = 100.0f * time / whylineUI.getTrace().getNumberOfEvents();

										whylineUI.setInputTime(time);
										
									}
								});
							}

						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}

						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								whylineUI.arrangeForAsking();
								replayForSlicer.putValue(Action.NAME, "replay");
								playing = false;
							}
						});
						
					}
				};

				playbackThread.start();

				return null;
				
			}

		};
			
		goToPreviousEvent = new LoggedAction(whylineUI, "go to previous event") { 
			protected AbstractUIEvent<?> act() {
				VisualizationUI visualizationUI = whylineUI.getVisualizationUIVisible();
				View before = visualizationUI.getEventBefore(visualizationUI.getSelection());
				if(before != null) visualizationUI.setSelection(before, true, UI.PREVIOUS_EVENT_UI); 
				else Toolkit.getDefaultToolkit().beep();
				return null;
			}
		};
	
		goToNextEvent = new LoggedAction(whylineUI, "go to next event") { 
			protected AbstractUIEvent<?> act() { 
				VisualizationUI visualizationUI = whylineUI.getVisualizationUIVisible();
				View after = visualizationUI.getEventAfter(visualizationUI.getSelection());
				if(after != null) visualizationUI.setSelection(after, true, UI.NEXT_EVENT_UI); 
				else Toolkit.getDefaultToolkit().beep();
				return null;
			}
		};

		goToPreviousBlock = new LoggedAction(whylineUI, "go to previous block") { 
			protected AbstractUIEvent<?> act() { 
				VisualizationUI visualizationUI = whylineUI.getVisualizationUIVisible();
				View before = visualizationUI.getEnclosingBlock(visualizationUI.getSelection());				
				if(before != null) visualizationUI.setSelection(before, true, UI.PREVIOUS_BLOCK_UI);
				else  Toolkit.getDefaultToolkit().beep();
				return null;
			}
		};

		goToNextBlock = new LoggedAction(whylineUI, "go to next block") { 
			protected AbstractUIEvent<?> act() { 
				VisualizationUI visualizationUI = whylineUI.getVisualizationUIVisible();
				View after = visualizationUI.getNextEnclosedBlock(visualizationUI.getSelection());
				if(after != null) visualizationUI.setSelection(after, true, UI.NEXT_BLOCK_UI);
				else Toolkit.getDefaultToolkit().beep();
				return null;
			}
		};

		collapseBlock = new LoggedAction(whylineUI, "collapse block") { 
			protected AbstractUIEvent<?> act() { 
				VisualizationUI visualizationUI = whylineUI.getVisualizationUIVisible();
				visualizationUI.getVisualization().collapseSelectedBlockView();
				return null;
			}
		};

		addToExplanation = new LoggedAction(whylineUI, "add to explanation") { 
			protected AbstractUIEvent<?> act() { 
				VisualizationUI visualizationUI = whylineUI.getVisualizationUIVisible();
				visualizationUI.getVisualization().addNarrativeEntry(); 
				return null;
			}
		};

		queryTrace = new AbstractAction() {
			private String lastQuery = "";
			public void actionPerformed(ActionEvent e) {};
			
		};
		
		showHideThreads = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				
				Visualization viz = whylineUI.getQuestionsUI().getSituationVisible().getVisualizationUI().getVisualization();
				
				SituationUI situationUI = whylineUI.getQuestionsUI().getSituationVisible();
				if(situationUI != null)
					viz.setThreadsVisible(!viz.areThreadsVisible());

				if(situationUI.getVisualizationUI().getVisualization().getNumberOfThreadRows() == 1) {
					putValue(Action.SMALL_ICON, UI.HIDE_THREADS);
					setEnabled(false);
				}
				else 
					putValue(Action.SMALL_ICON, viz.areThreadsVisible() ? UI.HIDE_THREADS : UI.SHOW_THREADS);
				
			}
		};
		
	}
		
}