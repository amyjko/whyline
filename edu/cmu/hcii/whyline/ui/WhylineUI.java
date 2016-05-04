package edu.cmu.hcii.whyline.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.analysis.*;
import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.io.*;
import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.trace.nodes.ObjectState;
import edu.cmu.hcii.whyline.ui.annotations.NarrativeUI;
import edu.cmu.hcii.whyline.ui.components.*;
import edu.cmu.hcii.whyline.ui.events.*;
import edu.cmu.hcii.whyline.ui.io.*;
import edu.cmu.hcii.whyline.ui.launcher.LauncherUI;
import edu.cmu.hcii.whyline.ui.qa.*;
import edu.cmu.hcii.whyline.ui.source.*;
import edu.cmu.hcii.whyline.util.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class WhylineUI extends WhylineWindow implements Asker {
	
	private static final Dimension MIN_SIZE = new Dimension(320, 240);
	private static final Dimension MAX_SIZE = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	
	private final Trace trace;

	private final PersistentState persistentState;

	private final Mode debuggingMode;
		
	private final Actions actions = new Actions(this);
	
	private final UserListeners listeners = new UserListeners();

	private final LauncherUI launcher;

	// I/O views
	private final ConsoleUI consoleUI;
	private final GraphicsUI graphicsUI;
	private final ExceptionsUI exceptionsUI;

	private final WhylinePanel outputUI;
	private final TimeUI timeUI;
	private final WhylineTabbedPane outputTabs;
	
	// Q&A
	private QuestionsUI questionsUI;
	private final QuestionTabsUI questionTabsUI;

	// Source
	private final FilesUI filesUI;

	// Static
	private final LineTablesUI resultsUI;
	private final DocumentationUI documentationUI;
	private final OutlineUI outlineUI;
	private final WhylineMultiStateButton<TextSearch.Mode> whatToSearch;

	// Dynamic
	private final ThreadsUI threadsUI;
	private final ObjectsUI objectsUI;
	private BreakpointConsoleUI printsUI;
	
	// Toolbar
	private final WhylineToolbar toolbar;
	private final JVMMemoryUI memoryUI;
	private final NarrativeUI narrativeUI;
	private WhylineButton stepInto, stepOver, stepOut, runToBreakpoint, stop;

	// Layout
	private final MultipleSplitPane staticSplit;
	private final MultipleSplitPane dynamicSplit;
	private final MultipleSplitPane center;
	private final MultipleSplitPane sourceEtc;

	// Other
	private final WhylinePanel main;
	private final Overlay overlay;
	private final WhylinePanel tasksPanel;
	private final GraphicsScaleSlider scaleSlider;
	private final WhylineButton backButton, forwardButton;
	private final WhylineButton showStaticInfo, showDynamicInfo;
	private final WhylineButton showJavaDoc;
	private final WhylineButton feedback;

	private boolean staticInfoShowing = false, dynamicInfoShowing = false;
	
	// Things under the mouse
	private int arrowNumberOver = -1;
	private Question<?> questionOver;
	
	// Save the UI state every minute.
	private Timer saver = new Timer(true);
	private Timer taskWatcher = new Timer(true);

	private BreakpointDebugger breakpointDebugger;
	private WhylineLabel runningState;
	private DynamicSlice currentSlice = null;
	
	private HashMap<Object,Task> tasksOngoing = new HashMap<Object,Task>(5);
	private HashSet<Task> tasksShowing = new HashSet<Task>(5);

	// We keep a cache of these for token views, since they represent a lot of the same text.
	private final HashMap<String, GlyphVector> glyphCache = new HashMap<String,GlyphVector>(1000);

	private final Stack<UndoableUIEvent<?>> undoStack = new Stack<UndoableUIEvent<?>>();
	private final ArrayList<Line> navigationHistory = new ArrayList<Line>();
	
	/**
	 * Points to the position in the navigation history. This makes a "forward" command possible. 
	 */
	private int undoStackIndex = -1;
	
	private Object selection;

	private int inputEventID = 0;
	private int outputEventID = 0;
	
	private ChangeListener popupMenuListener;
	private KeyEventPostProcessor keyEventPostProcessor;
	
	private int mostRecentEventIDSelected = -1;

	private Window getSelectedWindow(Window[] windows) {
	    Window result = null;
	    for (int i = 0; i < windows.length; i++) {
	        Window window = windows[i];
	        if (window.isActive()) {
	            result = window;
	        } else {
	            Window[] ownedWindows = window.getOwnedWindows();
	            if (ownedWindows != null) {
	                result = getSelectedWindow(ownedWindows);
	            }
	        }
	    }
	    return result;
	}
	
	public WhylineUI(LauncherUI launcher, File path, Mode mode) throws IOException, AnalysisException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		
		this.launcher = launcher;
		this.debuggingMode = mode;

		trace = new Trace(new LoadingListener(), path);
		
		persistentState = new PersistentState(this);

		// We set this really early since the size of lots of panes are proportional to the size of the window.
		setSize(persistentState.getWindowWidth(), persistentState.getWindowHeight());

		UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
				
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) { 
				saveIfNecessaryThenClose();
			}
		});
		
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				int width = Math.min(Math.max(MIN_SIZE.width, getWidth()), MAX_SIZE.width);
				int height = Math.min(Math.max(MIN_SIZE.height, getHeight()), MAX_SIZE.height);
				setSize(width, height);
				persistentState.updateWindowSize(width, height);
			}
		});
		
		feedback = new WhylineButton(new AbstractAction("feedback") {
			public void actionPerformed(ActionEvent e) {
				feedback();
			}
		}, UI.getSmallFont(), "Send feedback to the Whyline devs");
		
		timeUI = new TimeUI(this);
		filesUI = new FilesUI(this);
		graphicsUI = new GraphicsUI(this, false);
		consoleUI = new ConsoleUI(this);
		exceptionsUI = new ExceptionsUI(this);
		questionsUI = new QuestionsUI(this);
		objectsUI = new ObjectsUI(this);
		narrativeUI = new NarrativeUI(this);
		resultsUI = new LineTablesUI(this);
		whatToSearch = new WhylineMultiStateButton<TextSearch.Mode>(TextSearch.Mode.values());
		questionTabsUI = new QuestionTabsUI(this);
		threadsUI = new ThreadsUI(this);
		outlineUI = new OutlineUI(this);
		documentationUI = new DocumentationUI(this);
		memoryUI = new JVMMemoryUI();
		overlay = new Overlay();
		scaleSlider = new GraphicsScaleSlider(this);
				
		backButton = new WhylineButton(new AbstractAction(Character.toString(UI.LEFT_ARROW)) {
			public void actionPerformed(ActionEvent e) { back(); }
		}, "navigate to the previously selected file, event, line, etc.");

		forwardButton = new WhylineButton(new AbstractAction(Character.toString(UI.RIGHT_ARROW)) {
			public void actionPerformed(ActionEvent e) { forward(); }}, "navigate forward to the next file, event, line, etc. in the navigation history");
		
		updateBackForwardButtonState();
		
		Dimension maxSize = new Dimension(60, 25);
		
		showStaticInfo = new WhylineButton(new AbstractAction() {
			public void actionPerformed(ActionEvent e) { 
				showStaticInfo(!staticInfoShowing);
			}}, UI.getSmallFont(), "show information about source files");
		showStaticInfo.setEnabled(true);

		showDynamicInfo = new WhylineButton(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {  
				showDynamicInfo(!dynamicInfoShowing);
			}}, UI.getSmallFont(), "show information about call stacks, local variables, and object state");
		showDynamicInfo.setEnabled(false);
		
		showJavaDoc = new WhylineButton(new AbstractAction("<html><center>show<br><i>javadoc") {
			public void actionPerformed(ActionEvent e) {  
				showJavaDoc(getSelectedMethod());
			}}, maxSize, UI.getSmallFont(), "show javadocs for the currently selected method");
		
		listeners.addFocusListener(questionsUI);
		listeners.addFocusListener(filesUI.getFilesView());
		listeners.addFocusListener(timeUI);

		listeners.addQuestionListener(timeUI);

		listeners.addTimeListener(timeUI);
		listeners.addTimeListener(graphicsUI);

		WhylinePanel graphicsUIWithSlider = new WhylinePanel(new BorderLayout(UI.getPanelPadding(), UI.getPanelPadding()));
		graphicsUIWithSlider.add(graphicsUI, BorderLayout.CENTER);
		graphicsUIWithSlider.add(scaleSlider, BorderLayout.SOUTH);
		
		outputTabs = new WhylineTabbedPane();
		outputTabs.addTab("graphics", graphicsUIWithSlider);
		outputTabs.addTab("text", consoleUI);
		outputTabs.addTab("exceptions", exceptionsUI);
		
		outputUI = new WhylinePanel(new BorderLayout(0, UI.getPanelPadding()));
		outputUI.add(outputTabs, BorderLayout.CENTER);
		outputUI.add(timeUI, BorderLayout.SOUTH);
		outputUI.setMinimumSize(new Dimension(UI.getDefaultInfoPaneWidth(this), 0));
		
		Dimension infoBoxDimension = new Dimension(UI.getDefaultInfoPaneWidth(this), 0);
		
		staticSplit = new MultipleSplitPane(WhylineSplitPane.VERTICAL_SPLIT, outlineUI, resultsUI);
		staticSplit.setMinimumSize(new Dimension(50, 0));
		staticSplit.setPreferredSize(infoBoxDimension);
		
		dynamicSplit = new MultipleSplitPane(WhylineSplitPane.VERTICAL_SPLIT, outputUI, objectsUI, threadsUI);
		dynamicSplit.setMinimumSize(new Dimension(50, 0));
		dynamicSplit.setPreferredSize(infoBoxDimension);
		
		toolbar = new WhylineToolbar(WhylineToolbar.HORIZONTAL);
		toolbar.setBorder(new EmptyBorder(UI.getPanelPadding(), UI.getPanelPadding(), UI.getPanelPadding(), UI.getPanelPadding()));
		toolbar.setMinimumSize(new Dimension(0, UI.getToolbarHeight()));
		toolbar.setOpaque(true);
		toolbar.setBackground(UI.getPanelDarkColor());
		toolbar.add(new DebugMenu(this));
		toolbar.addSeparator();
		toolbar.add(backButton);
		toolbar.addSeparator();
		toolbar.add(forwardButton);
		
		if(isWhyline()) {
			toolbar.addSeparator();
			toolbar.add(showStaticInfo);
			toolbar.add(showDynamicInfo);
		}

		toolbar.add(feedback);
		
//		toolbar.addSeparator();
//		toolbar.add(showJavaDoc);
		
		sourceEtc = new MultipleSplitPane(JSplitPane.VERTICAL_SPLIT, filesUI);
		sourceEtc.add(filesUI, BorderLayout.CENTER);
						
		if(debuggingMode == Mode.WHYLINE) {

			center = new MultipleSplitPane(JSplitPane.HORIZONTAL_SPLIT, staticSplit,sourceEtc, dynamicSplit);

			listeners.addTimeListener(consoleUI);
				
			popupMenuListener = new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					handlePopupMenuChange(e);
				}
			};
			MenuSelectionManager.defaultManager().addChangeListener(popupMenuListener);
			
		}
		else if(debuggingMode == Mode.SLICER) {

			center = new MultipleSplitPane(JSplitPane.HORIZONTAL_SPLIT, staticSplit,sourceEtc);

			listeners.addTimeListener(consoleUI);
			listeners.addTimeListener(graphicsUI);

		}
		else {

			center = new MultipleSplitPane(JSplitPane.HORIZONTAL_SPLIT, staticSplit,sourceEtc, dynamicSplit);

			printsUI = new BreakpointConsoleUI(this);

			listeners.addTimeListener(consoleUI);
			listeners.addTimeListener(graphicsUI);
			listeners.addTimeListener(printsUI);

			breakpointDebugger =  new BreakpointDebugger(this);
			 
			stepOver = new WhylineButton(actions.stepOver, "step over calls on this line");
			stepOver.setIcon(UI.STEP_OVER);
			stepOver.setText(null);
			stepOver.setFocusable(false);
			stepInto = new WhylineButton(actions.stepInto, "step into the next call on this line");
			stepInto.setIcon(UI.STEP_INTO);
			stepInto.setText(null);
			stepInto.setFocusable(false);
			stepOut = new WhylineButton(actions.stepOut, "step out of the current call");
			stepOut.setIcon(UI.STEP_OUT);
			stepOut.setText(null);
			stepOut.setFocusable(false);
			runToBreakpoint = new WhylineButton(actions.runToBreakpoint, "run the program until reaching the next breakpoint");
			runToBreakpoint.setIcon(UI.RESUME);
			runToBreakpoint.setText(null);
			runToBreakpoint.setFocusable(false);
			stop = new WhylineButton(actions.stop, "stop the program");
			stop.setIcon(UI.STOP);
			stop.setText(null);
			stop.setFocusable(false);

			runningState = new WhylineLabel("stopped", UI.getSmallFont());
			runningState.setOpaque(true);
			runningState.setBorder(new EmptyBorder(3, 3, 3, 3) {
			    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			    	g.setColor(UI.getControlBorderColor());
	                g.drawRect(x, y, width - 1, height - 1);
			    }
			});
			breakpointDebugger.stop();

			toolbar.addSeparator();
			toolbar.add(new WhylineLabel("stepping", UI.getLargeFont()));
			toolbar.addSeparator();
			toolbar.add(runToBreakpoint);
			toolbar.add(stepInto);
			toolbar.add(stepOver);
			toolbar.add(stepOut);
			toolbar.add(stop);
			toolbar.addSeparator();
			toolbar.add(runningState);

			outputTabs.addTab("prints", printsUI);

			staticInfoShowing = true;
			dynamicInfoShowing = true;
			
		}

		tasksPanel = new WhylinePanel();
		tasksPanel.setLayout(new BoxLayout(tasksPanel, BoxLayout.X_AXIS));

		// After the tool specific tools, add these.
		toolbar.addSeparator();
		toolbar.add(tasksPanel);

		center.giveResizeWeightTo(sourceEtc);
		center.setBorder(new EmptyBorder(UI.getPanelPadding(), UI.getPanelPadding(), UI.getPanelPadding(), UI.getPanelPadding()));

		main = new WhylinePanel(new BorderLayout());
		main.add(center, BorderLayout.CENTER);
		if(mode == Mode.WHYLINE) {
			WhylinePanel toolbars = new WhylinePanel(new BorderLayout());
			toolbars.add(questionTabsUI, BorderLayout.NORTH);
			toolbars.add(toolbar, BorderLayout.SOUTH);
			main.add(toolbars, BorderLayout.SOUTH);
		}
		else {
			main.add(toolbar, BorderLayout.SOUTH);
		}
		
		getContentPane().setLayout(new OverlayLayout(getContentPane()));		
		getContentPane().add(overlay);
		getContentPane().add(main);

		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((int)(screenSize.getWidth() - getWidth()) / 2, (int)(screenSize.getHeight() - getHeight()) / 2);

		setVisible(true);

		toFront();
		requestFocus();

		arrangeForPlayback();
		
		log(new Note(Util.getDateString()));
		log(new ModeSet(mode));
		log(new Note("Loading trace..."));

		addTask(trace, "Loading trace");

		// Now that the window is open, we start loading the trace.
		trace.load(100);
		
		// Final consumer of backspace presses to handle undo.
		keyEventPostProcessor = new KeyEventPostProcessor() {
			public boolean postProcessKeyEvent(KeyEvent e) {
				
				boolean inField = e.getComponent() instanceof JTextField;
				if(e.getID() == KeyEvent.KEY_PRESSED) {
					
					if(e.getKeyCode() == KeyEvent.VK_F1) { 
						UI.setCreamColors();
						repaint();
					}
					
					boolean back = e.getKeyCode() == KeyEvent.VK_OPEN_BRACKET;
					boolean forward = e.getKeyCode() == KeyEvent.VK_CLOSE_BRACKET; 
					
					if(back || forward) {
						if(inField) {
							int position = ((JTextField)e.getComponent()).getCaretPosition();
							if(position > 0) return true;
							else {
								VisualizationUI viz = getVisualizationUIVisible();
								if(viz != null)
									viz.requestFocusInWindow();
								return true;
							}
						}
						boolean success = back ? back() : forward();
						if(!success) Toolkit.getDefaultToolkit().beep();
						return true;
					}
					else {
						if(inField) return true;
						switch(e.getKeyCode()) {
							case KeyEvent.VK_D :
								showJavaDoc(getSelectedMethod());
								break;
						}
					}
				}
				return true;
			
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(keyEventPostProcessor);
		
		taskWatcher.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				updateTasks();
			}
		}, 1000, 500);

		if(isWhyline()) {
			showStaticInfo(false);
			showDynamicInfo(false);
		}
		
	}

	private void feedback() {
		
		String result = JOptionPane.showInputDialog(WhylineUI.this, "<html>What would you like to tell the Whyline developers?<br>This will just send an e-mail containing the text you type here.");
		if(result != null) {
			
			int choice = JOptionPane.showConfirmDialog(WhylineUI.this, "<html>Is it okay to send <b>a screenshot of this window</b>?", "Screenshot?", JOptionPane.YES_NO_CANCEL_OPTION);
			if(choice != JOptionPane.CANCEL_OPTION) {
				try {
					Feedback.feedback(result, choice == JOptionPane.OK_OPTION ? WhylineUI.this : null);
					JOptionPane.showMessageDialog(WhylineUI.this, "<html>Feedback sent successfully!");
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(WhylineUI.this, "<html>Couldn't send the feedback. There was some sort of exception:<br><br>" + ex.getMessage());
				}
			}
		}

	}
	
	public void setBreakpointDebuggerState(String label, boolean isRunning) {
		
		runningState.setBackground(isRunning? UI.getRunningColor() : UI.getStoppedColor());
		runningState.setText(label);
		runningState.repaint();
		
	}
	
	public void showMemoryUsage() {
		
		if(memoryUI.getParent() == null) {
			
			toolbar.add(memoryUI);
			toolbar.addSeparator();
			toolbar.revalidate();

		}
		
	}
	
	public List<Line> getNavigationHistory() { return navigationHistory; }
	
	public void showTraceBreakdown() {
		
		WhylineWindow window = new WhylineWindow();
		window.getContentPane().add(new WhylineScrollPane(new TraceExplorerUI(trace)));
		window.setSize(320, 240);
		window.setVisible(true);
		
	}
	
	public GlyphVector getTokenGlyphs(Token token) {

		GlyphVector glyphs = glyphCache.get(token.getText());
		if(glyphs == null) {
			Graphics2D g2d = (Graphics2D)getGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			FontRenderContext context = g2d.getFontRenderContext();
			Font font = UI.getFixedFont();
			String text = token.getText();
			text = text.replace("\t", UI.TAB_SPACES);
			glyphs = font.createGlyphVector(context, text);
			if(!token.isComment())
				glyphCache.put(token.getText(), glyphs);
		}
		return glyphs;
		
	}
	
	public boolean isGraphicsFitToWindow() { return scaleSlider.isFitToWindow(); }
	public void setGraphicsScale(int percent) { scaleSlider.setValue(percent); }
	public int getGraphicsScale() { return scaleSlider.getValue(); }
		
	public OutlineUI getOutlineUI() { return outlineUI; }
	public QuestionsUI getQuestionsUI() { return questionsUI; }
	public FilesView getFilesView() { return filesUI.getFilesView(); }
	public TimeUI getTimeControllerUI() { return timeUI; }
	public NarrativeUI getNarrativeUI() { return narrativeUI; }
	public GraphicsUI getGraphicsUI() { return graphicsUI; }
	public ObjectsUI getObjectsUI() { return objectsUI; }
	public Actions getActions() { return actions; }
	public LineTablesUI getLinesUI() { return resultsUI; }
	public VisualizationUI getVisualizationUIVisible() { 
	
		if(questionsUI == null || questionsUI.getAnswerUIVisible() == null || questionsUI.getAnswerUIVisible().getSituationSelected() == null) return null;
		else return questionsUI.getAnswerUIVisible().getSituationSelected().getVisualizationUI(); 
		
	}
	
	public int getOutputEventID() { return outputEventID; }
	
	public int getInputEventID() { return inputEventID; }
	
	public int getCurrentEventID() { 
		
		VisualizationUI viz = getVisualizationUIVisible();
		if(viz == null) return getInputEventID();
		else {
			EventView selection = viz.getVisualization().getSelectedEventView();
			if(selection == null) return getInputEventID();
			else return selection.getEventID();
		}
		
	}
	
	public IOEvent getEventAtInputTime() { return trace.getIOHistory().getEventAtTime(inputEventID); }
	public IOEvent getEventAtOutputTime() { return trace.getIOHistory().getEventAtTime(outputEventID); }
	
	public BreakpointDebugger getBreakpointDebugger() { return breakpointDebugger; }

	public void setBreakpointPauseMode(boolean inPauseMode) {
		
		runToBreakpoint.setIcon(inPauseMode ? UI.PAUSE : UI.RESUME);
		runToBreakpoint.revalidate();
		
	}
	
	public Dimension getMinimumSize()   { return MIN_SIZE; }
	   
	public Trace getTrace() { return trace; }

	private void updateBackForwardButtonState() {
		
		forwardButton.setEnabled(undoStackIndex < undoStack.size() - 1);
		backButton.setEnabled(undoStackIndex > 0);
		
	}
	
	// Go back to the selection recently navigated to.
	public boolean back() {
		
		if(undoStack.isEmpty()) return false;
		
		UndoableUIEvent<?> selection;
		boolean last = false;
		if(undoStackIndex > 0)
			undoStackIndex--;
		selection = undoStack.get(undoStackIndex);
		selection.select(this);						
		
		updateBackForwardButtonState();
		
		return !last;
		
	}
	
	public boolean forward() {
		
		if(undoStackIndex < undoStack.size() - 1) {
			
			UndoableUIEvent<?> selection;
			undoStackIndex++;
			selection = undoStack.get(undoStackIndex);
			selection.select(this);

			updateBackForwardButtonState();
			return true;

		}
		else {
			return false;
		}
		
	}
	
	public void log(AbstractUIEvent<?>  event) {

		if(event instanceof UndoableUIEvent) {
			if(event.wasUserInitiated()) {
				
				while(undoStack.size() > undoStackIndex + 1)
					undoStack.pop();
				
				undoStack.push((UndoableUIEvent<?>)event);
				undoStackIndex++;

				updateBackForwardButtonState();

				try {
					Line line = ((UndoableUIEvent<?>)event).getCorrespondingLine(trace);
					if(line != null) {
						navigationHistory.add(0, line);
						resultsUI.updateHistory(line);
					}
				} catch(ParseException e) {
					e.printStackTrace();
				}
			}
		}
		persistentState.addNavigation(event);
		
	}

	public void addTask(Object key, String description) {
		
		Task task = new Task(this, description);
		tasksOngoing.put(key, task);
				
	}
	
	private void updateTasks() {
		
		for(Task task : tasksOngoing.values()) {
			if(!tasksShowing.contains(task) && task.getMillisecondsSinceCreation() >= 500) {
				tasksShowing.add(task);
				tasksPanel.add(task);
				tasksPanel.validate();
				tasksPanel.invalidate();
				tasksPanel.repaint();
			}
		}
		
	}

	public void updateTask(Object key, String message,double progress) {
		
		Task task = tasksOngoing.get(key);
		if(task != null) {
			if(message != null) task.setNote(message);
			if(progress >= 0) task.setProgress(progress);
			repaint();
		}

	}
		
	public void removeTask(Object key) {

		Task task = tasksOngoing.get(key);
		tasksOngoing.remove(key);
		if(task != null) {
			tasksShowing.remove(task);
			tasksPanel.remove(task);
			tasksPanel.revalidate();
		}

	}
	
	public void showStaticInfo(boolean yes) {

		staticInfoShowing = yes;
		showStaticInfo.setText((staticInfoShowing ? "hide" : "show") + " code info");
		if(trace.isDoneLoading()) {
			if(getQuestionVisible() == null)
				arrangeForAsking();
			else
				arrangeForInspecting();
		}

	}

	public void showDynamicInfo(boolean yes) {
		
		dynamicInfoShowing = yes;
		showDynamicInfo.setText((dynamicInfoShowing ? "hide" : "show") + " execution info");
		if(trace.isDoneLoading()) {
			arrangeForInspecting();
		}
		
	}

	public boolean isDynamicInfoShowing() { return dynamicInfoShowing; }
	public boolean isStaticInfoShowing() { return staticInfoShowing; }
	
	public TextSearch.Mode getSearchMode() { return TextSearch.Mode.valueOf(whatToSearch.getState().name()); } 
	
	public WhylineMultiStateButton<?> getSearchModeButton() { return whatToSearch; }
		
	public PersistentState getPersistentState() { return persistentState; }
	
	public void save(final boolean closeAfterwards) {

		String message = "What would you like to call it?";
		String desiredName = null;
		while(desiredName == null) {
			desiredName = JOptionPane.showInputDialog(this, message);
			File folder = trace.getSaveLocation(desiredName);
			if(folder.exists()) {
				message = "<html>A trace named <b>" + desiredName + "</b> already exists.<br>What would you like to call it instead?";
				desiredName = null;
			}
		}
		
		final String name = desiredName;
		
		if(name != null) {

			addTask(trace, "Saving");
			updateTask(trace, "Saving...", 0);

			Thread save = new Thread() {
				public void run() {
					try {
						boolean success = trace.save(name, new Util.ProgressListener() {
								public void progress(double percent) { updateTask(trace, null, percent); }
								public void notice(String notice) { updateTask(trace, notice, -1); }
							});
						if(!success)
							JOptionPane.showMessageDialog(WhylineUI.this, "Failed to save because the folder already exists.", "Couldn't save", JOptionPane.ERROR_MESSAGE);
						if(closeAfterwards)
							close();
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(WhylineUI.this, "Couldn't save the trace: " + e1.getMessage());
					}
					removeTask(trace);
				}
			};
			save.start();
		}

	}
		
	/**
	 * Returns true if the close was canceled.
	 */
	public boolean saveIfNecessaryThenClose() {

		if(!trace.isSaved()) {
			
			String[] options = { "Yes, save!", "No, just close the window", "Don't close this window"  };
			int answer = JOptionPane.showOptionDialog(WhylineUI.this, "Do you want me to save this trace to the whyline's workspace for later?", "Save this trace?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, "No");

			// If we cancel closing, we return immediately
			if(answer == 2) return true;
			// Otherwise, save before we close.
			if(answer == 0) {
				save(true);
				return false;
			
			}
			
		}

		// Afterwards, close and dispose.
		close();
		return false;
		
	}

	private void close() {

		trace.cancelLoading();
		
		saver.cancel();
		taskWatcher.cancel();

		saver = null;
		
		try {
			persistentState.write();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		// We need to remove this so that this window doesn't try to respond to menu events. And for garbage collection.
		MenuSelectionManager.defaultManager().removeChangeListener(popupMenuListener);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(keyEventPostProcessor);

		graphicsUI.freeGlobalListeners();
		memoryUI.dispose();
		
		setVisible(false);

		dispose();

		if(launcher != null)
			launcher.close(this);
		
	}
	
	public void arrangeForPlayback() {

		center.resetWith(outputUI);
		
		center.revalidate();

	}

	public void arrangeForAsking() {

		showDynamicInfo.setEnabled(false);
		
		if(debuggingMode == Mode.WHYLINE) {

			scaleSlider.setFitToWindow(false);

			if(staticInfoShowing)
				center.resetWith(staticSplit, outputUI);
			else
				center.resetWith(outputUI);
				
			center.giveResizeWeightTo(outputUI);

			sourceEtc.resetWith(filesUI);

			timeUI.setRequestFocusOnEnter(true);
			timeUI.requestFocusInWindow();

		}
		else if(debuggingMode == Mode.SLICER) {

			dynamicSplit.resetWith(outputUI, objectsUI, threadsUI);

			center.resetWith(staticSplit,sourceEtc);
			center.giveResizeWeightTo(sourceEtc);

		}
		else if(debuggingMode == Mode.BREAKPOINT) {
			
			scaleSlider.setFitToWindow(false);

			dynamicSplit.resetWith(outputUI, objectsUI, threadsUI);

			center.resetWith(staticSplit,sourceEtc, dynamicSplit);
			center.giveResizeWeightTo(sourceEtc);

		}
		
		center.revalidate();

		repaint();
		
	}
			
	/**
	 * Should only be called when in whyline mode.
	 */
	private void arrangeForInspecting() {
		
		assert debuggingMode == Mode.WHYLINE : "Can only arrange for inspecting " + Mode.WHYLINE + " mode";

		showDynamicInfo.setEnabled(true);
		
		scaleSlider.setFitToWindow(true);

		// Put the output back
		dynamicSplit.resetWith(outputUI, objectsUI, threadsUI);

		sourceEtc.setMinimumSize(new Dimension(UI.getDefaultInfoPaneWidth(this), 0));
		sourceEtc.resetWith(filesUI, questionsUI);
		sourceEtc.giveResizeWeightTo(filesUI);
		
		if(staticInfoShowing && dynamicInfoShowing)
			center.resetWith(staticSplit,sourceEtc, dynamicSplit);
		else if(staticInfoShowing)
			center.resetWith(staticSplit,sourceEtc);
		else if(dynamicInfoShowing)
			center.resetWith(sourceEtc, dynamicSplit);
		else
			center.resetWith(sourceEtc);
			
		center.giveResizeWeightTo(sourceEtc);
				
		timeUI.setRequestFocusOnEnter(false);
		
		center.revalidate();
		
	}
	
	public void addDynamicSlice(Line line, boolean onlyMostRecent) {

		assert debuggingMode == Mode.SLICER : "Can only add slices in dynamic slice mode. Currently in " + debuggingMode + " mode";
		
		clearDynamicSlice();
		
		currentSlice = new DynamicSlice(this, line, onlyMostRecent);
		
		resultsUI.addResults(currentSlice);
		
		validate();
		repaint();
		
	}
	
	public void clearDynamicSlice() {
		
		assert debuggingMode == Mode.SLICER : "Can only clear slices in dynamic slice mode.";

		// Clear file annotations
		filesUI.getFilesView().removeWindowsArrowsAndHighlights();
		
		validate();
		repaint();
		
	}
	
	private Question<?> questionInProgress = null;
	
	public void answer(final Question<?> question) { 

		if(questionInProgress != null) {
			
			JOptionPane.showMessageDialog(this, "Already answering a question. Wait until its finished to ask another!");
			return;
			
		}

		questionInProgress = question;
		addTask(questionInProgress, "Answering question");

		// Start by showing the progress bar. We wait, since we need to show the question area before proceeding.
		setQuestion(question);
		updateAnsweringStatus(questionInProgress, "Answering question...", 0.0);

		Thread answeringThread = new Thread("asker") {
			public void run() {

				// Compute all of the answers
				question.computeAnswer();

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
		
						updateTask(questionInProgress, "Done answering.", 0.0);
		
						// If there's only one answer, select it and uncollapse its group.
						updateTask(question, "Showing answer....", 0.0);
						questionsUI.getAnswerUIVisible().showSituation(question.getAnswer());

						removeTask(questionInProgress);
						questionInProgress = null;

					}
				});
			}
		};
		answeringThread.start();

	}

	public void problemAnswering(Question<?> question, AnalysisException e) {

		JOptionPane.showMessageDialog(this, "There was an error determine the answer. See the console for the error.");
		e.printStackTrace();
		removeTask(question);
		questionInProgress = null;

	}
	
	public void updateAnsweringStatus(Question<?> question, String status, double percentComplete) {
		
		updateTask(question, status, percentComplete);
		
	}

	public void doneAnswering() {}

	private int processingNotices = 0;
	
	public void processing(boolean isProcessing) {

		boolean wasWaiting = processingNotices > 0;
		
		if(isProcessing) processingNotices++;
		else processingNotices--;
		
		if(processingNotices > 0 != wasWaiting) {
			boolean done = processingNotices == 0;
			Cursor wait = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
			Cursor normal = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
			Cursor hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
			main.setCursor(done ? normal : wait);
			graphicsUI.setCursor(done ? hand : wait);
		}
		
	}
	
	public boolean userIsAskingQuestion() { 
		
		return MenuSelectionManager.defaultManager().getSelectedPath().length > 0; 
		
	}
	
	public void setQuestion(Question<?> question) {

		log(new QuestionSelected(question, true));
		
		questionsUI.setQuestion(question);
		questionTabsUI.addQuestion(question);

		for(UserQuestionListener listener : listeners.getQuestionListeners())
			listener.questionChanged(question);

		if(question == null)
			arrangeForAsking();
		else
			arrangeForInspecting();
		
	}
	
	public void removeQuestion(Question<?> question) {

		questionsUI.removeQuestion(question);
		questionTabsUI.removeQuestion(question);
		
	}
	
	public boolean isQuestionVisible() { return getQuestionVisible() != null; }
	
	public Question<?> getQuestionVisible() { return questionsUI == null ? null : questionsUI.getQuestionVisible(); }
			
	public int getMostRecentEventIDSelected() { return mostRecentEventIDSelected; }
	
	public void setArrowOver(int number) {
		
		if(this.arrowNumberOver == number) return;
		
		this.arrowNumberOver = number;
		
		filesUI.handleArrowOverChanged();
		questionsUI.handleArrowOverChanged();
		
	}
	
	public int getArrowOver() { return arrowNumberOver; }
	
	public Question<?> getQuestionOver() { return questionOver; }
	
	public void showJavaDoc(MethodInfo method) {
		
		String url = method.getJavaDocURL();
		url = Whyline.getJDKJavaDocPath() + url;
		Util.openURL(url);
				
	}
	
	public MethodInfo getSelectedMethod() { 
		
		if(selection instanceof MethodInfo) return (MethodInfo)selection;
		
		Instruction instruction = 
			selection instanceof Integer ?
					trace.getInstruction((Integer)selection) :
			selection instanceof Explanation ?
					trace.getInstruction(((Explanation)selection).getEventID()) : null
					;
					
		if(instruction == null) return null;
		else return instruction.getMethod();
		
	}
	
	public int getSelectedEventID() {
		
		if(selection instanceof Integer) return (Integer)selection;
		else if(selection instanceof Explanation) return ((Explanation)selection).getEventID();
		else return -1;
		
	}
	
	private void handleNewSelection(Object newSelection) {
		
		selection = newSelection;
		showJavaDoc.setEnabled(getSelectedMethod() != null);
		
	}
	
	public void selectLine(Line line, boolean isNavigation, String ui) {
		
		log(new LineNavigation(line, ui, isNavigation));
		
		handleNewSelection(line);

		getFilesView().showLine(line);

	}
	
	public void selectEvent(int eventID, boolean isNavigation, String ui) {

		if(eventID >= 0)
			log(new EventNavigation(eventID, ui, isNavigation));

		handleNewSelection(eventID);

		handleEventIDSelection(eventID, ui);

		for(UserFocusListener l : listeners.getFocusListeners())
			l.showEvent(eventID);

	}
	
	public void selectExplanation(Explanation subject, boolean userInitiated, String ui) {

		if(selection == subject) return;
		handleNewSelection(subject);

		log(new ExplanationNavigation(subject, ui, userInitiated));
		
		for(UserFocusListener l : listeners.getFocusListeners())
			l.showExplanation(subject);

		handleEventIDSelection(subject.getEventID(), ui);
		
	}
	
	private void handleEventIDSelection(int eventID, String ui) {

		Instruction instruction = trace.getInstruction(eventID);
		
		boolean newID = eventID != mostRecentEventIDSelected;

		mostRecentEventIDSelected = eventID; 
		
		objectsUI.showEventID(eventID);
		threadsUI.showEventID(eventID);

		documentationUI.showInstruction(instruction);

		if(newID) {
			Line line = instruction == null ? null : instruction.getLine();
			if(line == null)
				log(new NoLineHover(ui));
			else
				log(new LineHover(line, ui));
		}
		
	}
	
	public void selectInstruction(Instruction subject, boolean isNavigation, String ui) {

		if(selection == subject) return;
		handleNewSelection(subject);

		log(new InstructionNavigation(subject, ui, isNavigation));

		for(UserFocusListener l : listeners.getFocusListeners())
			l.showInstruction(subject);

		documentationUI.showInstruction(subject);

	}

	public UndoableUIEvent<UnexecutedInstruction> selectUnexecutedInstruction(UnexecutedInstruction subject, boolean isNavigation, String ui) {
		
		if(selection == subject) return null;
		handleNewSelection(subject);

		for(UserFocusListener l : listeners.getFocusListeners())
			l.showUnexecutedInstruction(subject);

		log(new UnexecutedInstructionNavigation(subject, ui, isNavigation));
		
		documentationUI.showInstruction(subject.getInstruction());

		return null;
		
	}
	
	public void selectInstructions(Iterable<Instruction> subject) {

		if(selection == subject) return;
		handleNewSelection(subject);

		for(UserFocusListener l : listeners.getFocusListeners())
			l.showInstructions(subject);

	}

	public UndoableUIEvent<MethodInfo> selectMethod(MethodInfo subject, boolean isNavigation, String ui) {
		
		if(selection == subject) return null;
		handleNewSelection(subject);

		for(UserFocusListener l : listeners.getFocusListeners())
			l.showMethod(subject);

		documentationUI.showMethod(subject);

		log(new MethodNavigation(subject, ui, isNavigation));
		return null;

	}

	public UndoableUIEvent<Classfile> selectClass(Classfile subject, boolean userInitiated, String ui) {
		
		if(selection == subject) return null;
		handleNewSelection(subject);

		for(UserFocusListener l : listeners.getFocusListeners())
			l.showClass(subject);

		documentationUI.showClass(subject);

		log(new ClassNavigation(subject, ui, userInitiated));
		return null;

	}

	public UndoableUIEvent<FileInterface> selectFile(FileInterface subject, boolean isNavigation, String ui) {
		
		if(selection == subject) return null;
		
		if(subject == null) return null;
		
		handleNewSelection(subject);

		for(UserFocusListener l : listeners.getFocusListeners())
			l.showFile(subject);
		
		log(new FileNavigation(subject, ui, isNavigation));
		return null;
		
	}
	
	private int boundTime(int time) { return Math.min(Math.max(0, time), trace.getNumberOfEvents() - 1); }
	
	public void setInputTime(int newTime) { 
		
		newTime = boundTime(newTime);

		if(newTime == inputEventID) return;
		
		this.inputEventID = newTime;

		// Move the output time if necessary.
		if(inputEventID > outputEventID) setOutputTime(inputEventID);
		
		// Do this in the event thread
		for(UserTimeListener l : listeners.getTimeListeners())
			l.inputTimeChanged(inputEventID);
		
	}
	
	public void setOutputTime(int newTime) {

		newTime = boundTime(newTime);
		
		outputEventID = newTime;

		// Move the input time if necessary
		if(outputEventID < inputEventID) setInputTime(outputEventID);
		
		// Do this in the event thread
		for(UserTimeListener l : listeners.getTimeListeners()) 
			l.outputTimeChanged(outputEventID);

	}

	public Scope getCurrentScope() { return new Scope(trace, inputEventID, outputEventID); }

	public WhylineUI.Mode getMode() { return debuggingMode; }
	public boolean canAskOutputQuestions() { return debuggingMode == Mode.WHYLINE; }
	public boolean canAskCodeQuestions() { return debuggingMode == Mode.WHYLINE; }
	public boolean isWhyline() { return debuggingMode == Mode.WHYLINE; } 
	
	private QuestionMenu.MakerMenu recentMakerHovered = null;

	@SuppressWarnings("unchecked")
	private void handlePopupMenuChange(ChangeEvent e) {
	
		if(!WhylineUI.this.isActive()) return;
		
		MenuSelectionManager msm = (MenuSelectionManager)e.getSource();
	    MenuElement[] path = msm.getSelectedPath();

	    questionOver = null;
	    
	    // Enable the overlay when there's a menu visible.
	    // Disable it with the menu goes away.
    	overlay.setIntercepting(path.length > 0);
	    
	    if(path.length > 0) {

	    	MenuElement selection = path[path.length - 1];
	    	Object subject = null;
	    	
	    	// If we're hovering over the menu, fill the menu's items.
	    	if(selection instanceof JPopupMenu)
	    		if(path.length - 2 >= 0)
	    			selection = path[path.length - 2];
	    	
	    	if(selection instanceof QuestionMenu.Menu) {

	    		subject = ((QuestionMenu.Menu)selection).getSubject();

	    		// Fill the menu!
	    		((QuestionMenu.Menu)selection).addItemsIfNecessary(true);

	    		if(selection instanceof QuestionMenu.MakerMenu) {
				    // If we have previously selected a maker menu, make sure to cancel its operation.
				    if(recentMakerHovered != null) recentMakerHovered.cancel();
	    			recentMakerHovered = (QuestionMenu.MakerMenu)selection;
	    		}
	    		
	    	}
	    	else if(selection instanceof QuestionMenu.MessageItem) {
	    		
	    		// Fill the menu!
	    		if(((QuestionMenu.MessageItem)selection).getParentMenu() != null)
	    			((QuestionMenu.MessageItem)selection).getParentMenu().addItemsIfNecessary(true);
	    				    		
	    	}
	    	else if(selection instanceof QuestionMenu.QuestionItem) {
	    		
	    		subject = ((QuestionMenu.QuestionItem)selection).getSubject();
	    		questionOver = ((QuestionMenu.QuestionItem)selection).getQuestion();

	    	}	    	
	    	
			if(subject instanceof ObjectState) {
				
				objectsUI.addObject(((ObjectState)subject).getObjectID());
				
				QualifiedClassName classname = getTrace().getClassnameOfObjectID(((ObjectState)subject).getObjectID());
				Classfile classfile = getTrace().getClassfileByName(classname);
				FileInterface fileToShow = null;
				if(classfile != null) {
	    			if(classfile.getSourceFile() != null) fileToShow = classfile.getSourceFile();
	    			else fileToShow = classfile;
	    			selectFile(fileToShow, false, UI.QUESTION_HOVER_UI);
				}
				
			}
			else if(subject instanceof OutputEvent) {
				
				Instruction instruction = trace.getInstruction(((OutputEvent)subject).getEventID());
				selectInstruction(instruction, false, UI.QUESTION_HOVER_UI);

			}
			else if(subject instanceof Integer) {

				selectInstruction(trace.getInstruction((Integer)subject), false, UI.QUESTION_HOVER_UI);

			}
			else if(subject instanceof Instruction) {

				selectInstruction((Instruction)subject, false, UI.QUESTION_HOVER_UI);

			}
			else if(subject instanceof MethodInfo)
				selectMethod((MethodInfo)subject, false, UI.QUESTION_HOVER_UI);

			else if(subject instanceof Classfile) {

				selectClass((Classfile)subject, false, UI.QUESTION_HOVER_UI);
				
			}

			else if(subject instanceof Iterable)
				selectInstructions((Iterable<Instruction>)subject);
			
	    }
	    
    	timeUI.repaint();
	
	}
	
	private class LoadingListener implements TraceListener {
		
		////////////////////////////////////////////////////////////////
		//
		// Trace listener
		//
		////////////////////////////////////////////////////////////////
				
		public void loadingProgress(String message, double percentLoaded) {
			
			// In case we quit while loading...
			if(trace == null) return;

			updateTask(trace, message, percentLoaded);
			
		}
		
		public void loadingClassFiles() {}

		public void doneLoadingClassFiles() {
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {

					outlineUI.addFamiliarSource();

					// If there's just text, remove the split
					if(trace.hasTextualOutputInstructions() && !trace.hasGraphicalOutputInstructions()) {

						listeners.removeTimeListener(graphicsUI);
						
					}
					// If there's just graphics, remove the split
					else if(!trace.hasTextualOutputInstructions() && trace.hasGraphicalOutputInstructions()) {

						listeners.removeTimeListener(consoleUI);
						
					}
					// If there's both or no output, then we leave both panels so that the user can ask why.
					
					if(debuggingMode == Mode.WHYLINE) {
									
					}
					else if(debuggingMode == Mode.SLICER || debuggingMode == Mode.BREAKPOINT) {
						
						for(JavaSourceFile file : trace.getAllSourceFiles())
							filesUI.getFilesView().getWindowViewOf(file);
						
					}
					
					validate();
					repaint();

				}
			});

		}

		public void loadingMetadata() {
		
		}
		
		public void doneLoadingMetadata() {

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {

					// If this window has been closed before loading, don't do this.
					if(!WhylineUI.this.isVisible())
						return;
					
					StringBuilder builder = new StringBuilder();
							
					MethodInfo mainMethod = trace.getMain();
					if(mainMethod != null) {
						builder.append(mainMethod.getClassfile().getInternalName().getText().replace('/', '.'));
						for(String arg : getTrace().getMainArguments()) builder.append(" " + arg);
					}
					else
						builder.append("(didn't record main())");

					setTitle("Whyline for Java - " + builder.toString());
					
					selectMethod(trace.getMain(), true, UI.LOADING_UI);
					
				}
			});
			
		}
		
		public void doneLoading(final long time) {
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					
					double seconds = ((double)(time / 10)) / 100;
					
					// Have to do these in the right order to get the desired effect.
					boolean hasTextualOutput = trace.hasTextualOutputEvents();
					boolean hasGraphicalOutput = trace.hasGraphicalOutputEvents();
					
					outputTabs.setSelectedIndex(hasGraphicalOutput || !hasTextualOutput ? 0 : 1);		
					
					arrangeForAsking();

					// Once we're done reading the trace, set the % done to 100 and update the title.
					timeUI.setProgress(1.0);
					
					log(new Note("" + NumberFormat.getNumberInstance().format(trace.getNumberOfEvents()) + " events, " + seconds + " seconds"));

					setInputTime(
						getTrace().getIOHistory().getNumberOfEvents() > 0 ?
							getTrace().getIOHistory().getLastEvent().getEventID() : 0);
					setOutputTime(trace.getNumberOfEvents() - 1);

					if(debuggingMode == Mode.WHYLINE)
						timeUI.selectButtonBasedOnTrace();

					removeTask(trace);
					
					persistentState.initializeState();
					
					if(saver != null) {
						saver.scheduleAtFixedRate(new TimerTask() {
							public void run() {
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										try {
											persistentState.write();
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
								});
							}
						}, 1000, 2 * 1000);
					}

				}
			});
			
		}
				
		public void exceptionDuringLoading(Exception e) {
			
			JOptionPane.showMessageDialog(WhylineUI.this, "<html><p>There was an exception during the loading of the trace:<br><br>" + e.getMessage() + "</html>", "Trace loading error...", JOptionPane.ERROR_MESSAGE);
			saveIfNecessaryThenClose();
			
		}
		
		public void additionalSourceLoaded(JavaSourceFile source) { 

			outlineUI.addSource(source);
		
		}

		public void blockEvent(boolean loaded, int blockID, int frequency) {}
		
		public void windowParsed(final WindowState window) {
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
						graphicsUI.addWindowState(window);
				}
			});

		}

		public void ioEventsParsed(final int inputTime) {
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setInputTime(inputTime);
				}
			});
			
		}

	}

	private class UserListeners {
		
		private final Set<UserFocusListener> focusListeners = new HashSet<UserFocusListener>();
		private final Set<UserTimeListener> timeListeners = new HashSet<UserTimeListener>();
		private final Set<UserQuestionListener> questionListeners = new HashSet<UserQuestionListener>();
		
		public synchronized Iterable<UserFocusListener> getFocusListeners() { return focusListeners; }
		public synchronized Iterable<UserTimeListener> getTimeListeners() { return timeListeners; }
		public synchronized Iterable<UserQuestionListener> getQuestionListeners() { return questionListeners; }

		public synchronized void addFocusListener(UserFocusListener listener) { focusListeners.add(listener); }
		public synchronized void addTimeListener(UserTimeListener listener) { timeListeners.add(listener); }
		public synchronized void addQuestionListener(UserQuestionListener listener) { questionListeners.add(listener); }

		public synchronized void removeFocusListener(UserFocusListener listener) { focusListeners.remove(listener); }
		public synchronized void removeTimeListener(UserTimeListener listener) { timeListeners.remove(listener); }
		
	}

	private static class Task extends WhylineProgressBar  {

		private final long creationTime = System.currentTimeMillis();
		private final String description;

		private Task(WhylineUI whylineUI, String description) {

			super();
			
			this.description = description;
			
			setBorder(new EmptyBorder(UI.getBorderPadding(), UI.getBorderPadding(), UI.getBorderPadding(), UI.getBorderPadding()));
			setProgress(0.0);
			
		}

		public String getDescription() { return description; }
		
		public void setProgress(double value) { setValue(value); }
		public void setStatus(String note) { setNote(note); }
		
		public long getMillisecondsSinceCreation() { return System.currentTimeMillis() - creationTime; } 
		
		public String toString() { return description.toUpperCase(); }
		
	}

	
	public static enum Mode { 
		BREAKPOINT { public String getReadableName() { return "Breakpoints"; }}, 
		SLICER { public String getReadableName() { return "Slicer"; }},
		WHYLINE { public String getReadableName() { return "Whyline"; }};
		public abstract String getReadableName();		
	};

	private class Overlay extends WhylinePanel {
		
		private final MouseListener clicks = new MouseAdapter() {
			public void mouseClicked(MouseEvent event) { setIntercepting(false); }};

		private final MouseWheelListener scrolls = new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e)  { setIntercepting(false); }};
		
		private boolean addedListeners = false;
			
		public Overlay() {
			
			setIntercepting(false);

		}

		public void setIntercepting(boolean intercepting) {

			if(intercepting) {
				setEnabled(true);
				if(!addedListeners) {
					addMouseListener(clicks);
					addMouseWheelListener(scrolls);
					addedListeners = true;
				}
			}
			else {
				setEnabled(false);
				removeMouseListener(clicks);
				removeMouseWheelListener(scrolls);
				addedListeners = false;
			}
			WhylineUI.this.repaint();
			
		}

		public void paintComponent(Graphics g) {
			
//			if(isEnabled()) {
//				Graphics2D g2 = (Graphics2D)g.create();
//				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
//				g2.setColor(Color.black);
//				g2.fillRect(0, 0, getWidth(), getHeight());
//			}
			
		}
		
	}
	
}