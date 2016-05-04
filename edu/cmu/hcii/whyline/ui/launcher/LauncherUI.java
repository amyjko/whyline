
package edu.cmu.hcii.whyline.ui.launcher;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.trace.TraceMetaData;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.tracing.*;
import edu.cmu.hcii.whyline.tracing.AgentOptions.Option;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.*;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class LauncherUI extends WhylineWindow {
	
	private final LauncherUIActions actions = new LauncherUIActions(this);
	
	private final TextField configurationName;
	private final TextField projectPath;
	private final TextField classPaths;	
	private final TextField sourcePaths;
	private final InputField<WhylineComboBox> mainClass;
	private final TextField programArguments;
	private final TextField startMemory, maxMemory;
	
	private final WhylineCheckBox printInstrumentationEvents, printInstrumentationSummary, printBeforeAfter;
	
	private WhylineList configurationsList;
	private WhylineList savedTraces;

	private final WhylineButton clearConfigurationsButton;
	private final WhylineButton clearConfigurationButton, createConfigurationButton, editConfigurationButton;
	private final WhylineButton whylineIt, breakpointIt, sliceIt, clearCache;
	private final WhylineButton deleteTraceButton, deleteAllTracesButton;
	private final WhylineButton cancelRunningButton;
	
	private final PreferencesPanel preferencesPanel;
	private final EditConfigurationPanel fieldsPanel;
	private final WhylinePanel mainPanel;
	private final RunPanel runPanel; 

	private final WhylineTextArea selectionDetails;

	private ExecutionConfiguration configurationBeingEdited;
	
	private File WHYLINE_CONFIG_FILE;

	private final Vector<ExecutionConfiguration> configurations = new Vector<ExecutionConfiguration>();
	
	private boolean fieldsAreValid = true;
	
	protected final Vector<WhylineUI> openWindows = new Vector<WhylineUI>();

	private final Dimension preferredButtonColumnSize = new Dimension(0, 150);
	private final Dimension preferredInfoPanelSize = new Dimension(0, 150);
	
	private Runner currentExecution = null;
	
	public LauncherUI() throws InterruptedException, InvocationTargetException {
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
			
		loadConfigurations();
       		
		String javaHome = System.getProperty("java.home");
				
		configurationName = new TextField(this, "configuration name", "") {
			public String validate(WhylineTextField field) { return null; }
		};
			
		projectPath = new TextField(this, "project location", "") {
			public String validate(WhylineTextField field) {
				return ExecutionConfiguration.isValidProjectLocation(field.getText());
			}
		};

		classPaths = new TextField(this, "paths to classes", "") {
			public String validate(WhylineTextField field) {
				if(field.getText().equals("")) return null;
				return ExecutionConfiguration.isValidPathList(projectPath.getText(), field.getText());
			}
		};

		sourcePaths = new TextField(this, "paths to source", "") {
			public String validate(WhylineTextField field) {
				if(field.getText().equals("")) return null;
				return ExecutionConfiguration.isValidPathList(projectPath.getText(), field.getText());
			}
		};

		WhylineComboBox combo = new WhylineComboBox();
		combo.setEditable(true);
		
		mainClass = new InputField<WhylineComboBox>(this, "class with main()", combo) {
			public String validate(WhylineComboBox box) {
				return ExecutionConfiguration.isValidClassWithMain(projectPath.getText(), classPaths.getText(), (String)box.getSelectedItem());
			}
		};

		combo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainClass.validate(true);
			}
		});
		combo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				mainClass.validate(true);
			}
		});

		programArguments = new TextField(this, "program arguments", "") {
			public String validate(WhylineTextField field) { return null; }
		};

		startMemory = new TextField(this, "start memory (mb)", "64") {
			public String validate(WhylineTextField field) {
				return ExecutionConfiguration.isValidMemory(field.getText());
			}
		};

		maxMemory = new TextField(this, "max memory (mb)", "512") {
			public String validate(WhylineTextField field) {
				return ExecutionConfiguration.isValidMemory(field.getText());
			}
		};

		whylineIt = new WhylineButton(new RecordAction(WhylineUI.Mode.WHYLINE, null), "use the whyline to debug");
		whylineIt.setFont(UI.getLargeFont());
		breakpointIt = new WhylineButton(new RecordAction(WhylineUI.Mode.BREAKPOINT, null), "use the breakpoint debugger to debug");
		sliceIt = new WhylineButton(new RecordAction(WhylineUI.Mode.SLICER, null),"use the dynamic slider to debug");
				
		printInstrumentationEvents = new WhylineCheckBox("Print instrumentation events");
		printInstrumentationEvents.setAlignmentX(LEFT_ALIGNMENT);

		printInstrumentationSummary = new WhylineCheckBox("Print instrumentation summary");
		printInstrumentationSummary .setAlignmentX(LEFT_ALIGNMENT);

		printBeforeAfter = new WhylineCheckBox("Print methods before and after");
		printBeforeAfter.setAlignmentX(LEFT_ALIGNMENT);
				
		createConfigurationButton = new WhylineButton(actions.createConfiguration, "create a new launch configuration");		
		clearConfigurationsButton = new WhylineButton(actions.clearConfigurations, "clear the selected launch configuration");
		clearConfigurationButton = new WhylineButton(actions.removeConfiguration, "remove the selected launch configuration");
		editConfigurationButton = new WhylineButton(actions.editConfiguration, "edit the selected launch configuration");
		
		clearCache = new WhylineButton(actions.clearClassCache, "clear all of the instrumented classes in the global cache");
		clearCache.setEnabled(Whyline.getClassCacheFolder().exists());

		deleteTraceButton = new WhylineButton(actions.deleteSelectedTrace, "delete selected recording");
		deleteAllTracesButton = new WhylineButton(actions.deleteAllSavedTraces, "delete all recordings");

		deleteTraceButton.setEnabled(false);
		
		cancelRunningButton = new WhylineButton(actions.cancelRunning, "terminate this program");
		
		fieldsPanel = new EditConfigurationPanel();

		configurationsPanel = new ConfigurationsPanel();

		SavedTracesPanel tracesPanel = new SavedTracesPanel();

		runPanel = new RunPanel();

		selectionDetails = new WhylineTextArea("", 4, 15);
		selectionDetails.setBorder(null);
		selectionDetails.setEditable(false);
		selectionDetails.setOpaque(true);
		selectionDetails.setLineWrap(true);
		selectionDetails.setFont(UI.getFixedFont());
		selectionDetails.setBackground(UI.getControlBackColor());
		selectionDetails.setForeground(UI.getControlTextColor());
		
		WhylineScrollPane executionScroller = new WhylineScrollPane(selectionDetails);
		executionScroller.setBorder(new WhylineControlBorder());
		
		WhylinePanel executionsPanel = new WhylinePanel(new GridLayout(1, 2, UI.getPanelPadding(), UI.getPanelPadding()));
		executionsPanel.add(configurationsPanel);
		executionsPanel.add(tracesPanel);

		executionsAndDataPanel = new WhylinePanel(new BorderLayout(0, UI.getPanelPadding()));
		executionsAndDataPanel.add(executionsPanel, BorderLayout.CENTER);
		executionsAndDataPanel.add(executionScroller, BorderLayout.SOUTH);

		preferencesPanel = new PreferencesPanel();
		
		mainPanel = new WhylinePanel(new BorderLayout(UI.getPanelPadding(), UI.getPanelPadding() * 2));
		mainPanel.add(executionsAndDataPanel, BorderLayout.CENTER);

		WhylinePanel welcomePanel = new WhylinePanel(new BorderLayout(UI.getPanelPadding(), UI.getPanelPadding()));
		welcomePanel.add(new WhylineLabel(
				"<html>Welcome to the <b>Whyline for Java</b>! " +
				"After preparing a launch configuration or selecting a saved recording, " +
				"<b>press the button at the bottom</b> to launch or load it."), 
				BorderLayout.CENTER);
		welcomePanel.add(new WhylineButton(new AbstractAction("preferences") { public void actionPerformed(ActionEvent e) { showPreferencesPanel(); } }, "Change Whyline preferences"), BorderLayout.EAST);
		
		mainPanel.add(welcomePanel, BorderLayout.NORTH);
		
		showExecutionPanel(false);
		
		JComponent content = new WhylinePanel(new BorderLayout()) {
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D)g.create();
				double scale = ((double)getWidth() / UI.WHYLINE_IMAGE.getIconWidth());
				g2.scale(scale,scale);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
				UI.WHYLINE_IMAGE.paintIcon(this, g2, 0, 0);
			}
		};
		content.setOpaque(true);

		setContentPane(content);
		
		getContentPane().add(mainPanel);
		((JComponent)getContentPane()).setBorder(new EmptyBorder(UI.getPanelPadding() * 2, UI.getPanelPadding() * 2, UI.getPanelPadding() * 2, UI.getPanelPadding() * 2));
				
		setTitle("Whyline Launcher");
		setResizable(false);

		setPreferredSize(new Dimension(640, 640));
		pack();
		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((int)(screenSize.getWidth() - getWidth()) / 2, (int)(screenSize.getHeight() - getHeight()) / 2);
		
		setVisible(true);
		
        // Select the first one in the list.
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				if(configurations.size() > 0) configurationsList.setSelectedIndex(0);
				savedTraces.clearSelection();
			}
		});
		
		Runtime.getRuntime().addShutdownHook(new Thread() { public void run() { savePersistentStates(); }});
		
		if(configurations.size() == 0 && (!Whyline.getSavedTracesFolder().exists() || Whyline.getSavedTracesFolder().listFiles().length == 0)) {
			
			JOptionPane.showMessageDialog(this, 
					"<html>Thanks for trying the Whyline for Java! <br><br>" +
					"There are a few sample recordings you can try on the right,<br>" +
					"or you can click the <i>create config</i> to record one of your own programs.");
			
		}
		
	}

	public Color getBackground() { return UI.getPanelLightColor(); }
	
	private void savePersistentStates() {
		
		for(WhylineUI whylineUI : openWindows) {
			try {
				whylineUI.getPersistentState().write();
			} catch (IOException e) {
				System.err.println("Couldn't save persistent state of " + whylineUI.getTrace().getPath());
				e.printStackTrace();
			}
		}
		
	}
	
	public ExecutionConfiguration getConfigurationBeingEdited() { return configurationBeingEdited; }

	public Object getSelectedSavedTrace() { return savedTraces.getSelectedValue(); }

	public void addNewConfiguration(File programDirectory) {
		
		ExecutionConfiguration newConfig = new ExecutionConfiguration(programDirectory.getAbsolutePath(), null, "", "128", "256", "");
		configurations.add(0, newConfig);

		configurationBeingEdited = newConfig;

		updateAndSaveConfigurations();

		showConfigurationPanel();
		
	}
	
	public void editSelectedConfiguration() {

		showConfigurationPanel();
		
	}

	public void showMainPanel() {
		
		getContentPane().removeAll();
		getContentPane().add(mainPanel);
		getContentPane().validate();
		repaint();
		
	}

	public void showPreferencesPanel() {
		
		getContentPane().removeAll();
		getContentPane().add(preferencesPanel);
		getContentPane().validate();
		repaint();
		
	}
	
	public void showConfigurationPanel() {

		getContentPane().removeAll();
		getContentPane().add(fieldsPanel);
		getContentPane().validate();
		repaint();

		if(configurationBeingEdited != null) {
			final ExecutionConfiguration config = configurationBeingEdited;
			Thread findMains = new Thread("Find mains") {
				public void run() {
					
					config.determineClassesWithMain();
					// If we're still on the same config, update the combo box.
					if(configurationBeingEdited == config) {
						WhylineComboBox box = mainClass.getValue();
						Object selection = box.getSelectedItem();
						box.removeAllItems();
						for(String name : config.getClassfilesWithMain()) {
							box.addItem(name);
						}
						box.setSelectedItem(selection);
					}
					
				}
			};
			findMains.start();
			
		}
		
	}
	
	public void clearAllConfigurations() {
		
		configurations.clear();
		WHYLINE_CONFIG_FILE.delete();
		updateAndSaveConfigurations();

	}
	
	public void removeConfigurationBeingEdited() {
		
		configurations.remove(configurationBeingEdited);
		clearConfigurationButton.setEnabled(false);
		editConfigurationButton.setEnabled(false);
		updateAndSaveConfigurations();
		
	}
	
	public void clearClassCache() {
		
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			Util.deleteFolder(Whyline.getClassCacheFolder());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
			e.printStackTrace();
		}
		clearCache.setEnabled(false);
		setCursor(Cursor.getDefaultCursor());

	}
	
	public void deleteSelectedTrace() {

		try {
			Util.deleteFolder(new File(Whyline.getSavedTracesFolder(), "" + getSelectedSavedTrace()));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
			e.printStackTrace();
		}
		updateSavedTraceList();

	}
	
	public void deleteAllTraces() {
		
		try {
			Util.deleteFolder(Whyline.getSavedTracesFolder());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
			e.printStackTrace();
		}
		updateSavedTraceList();

	}
	
	private void handleNewSavedTraceSelection() {
		
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		Object selection = savedTraces.getSelectedValue();

		enableAnalysisButton(selection != null);

		if(selection != null) {

			deleteTraceButton.setEnabled(true);
			clearConfigurationButton.setEnabled(false);
			editConfigurationButton.setEnabled(false);
			
			configurationsList.clearSelection();
			
			whylineIt.setText("<html>Load <b>" + selection + "</b>");
			
			File traceDirectory = new File(Whyline.getSavedTracesFolder(), (String)selection);
			if(traceDirectory.isDirectory()) {

				long day = traceDirectory.lastModified() / (1000 * 60 * 60 * 24);
				long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
				long daysAgo = today - day;
				
				int size = Util.getFolderSizeInBytes(traceDirectory) / 1024 / 1024;

				TraceMetaData metadata = Trace.getMetaDataFrom(traceDirectory);

				// Compute expected retained size of trace (based on empirical data on a Mac)
				long classfilesByteSize = (metadata.getNumberOfClasses() * 80L) / 1024L;	// About 80K per classfile, on average
				long traceSize = 100; // Bare minimum if always swapping
				long uiSize = 100;
				long wiggleRoom = 100;
				long expectedRetainedSize = classfilesByteSize + traceSize + uiSize + wiggleRoom;
				
				int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024);
				boolean wontFit = expectedRetainedSize > maxMemory;
				
				// # events
				// # classfiles
				// # source files
				
				// Go back to wrapping lines, in case we're clearing program output.
				selectionDetails.setLineWrap(true);

				selectionDetails.setText(
						"" + size + " mb, created " + daysAgo + " days ago" + "\n" +
						Util.commas(metadata.getNumberOfClasses()) + " classes" + ", " +
						Util.commas(metadata.getNumberOfEvents()) + " events" + ", " +
						Util.commas((int)metadata.getNumberOfObjects())+ " objects" + "\n" +
						"~" + Util.commas(expectedRetainedSize) + " mb required, " + Util.commas(maxMemory) + " available" +
						(wontFit ? "\n(Need to restart with a higher maximum heap size)" : "")
				);
				
				selectionDetails.setForeground(wontFit ? UI.ERROR_COLOR : UI.getControlTextColor());
				
			}
			
		}
		else {
			
			selectionDetails.setText("");
			selectionDetails.setForeground(UI.getControlTextColor());

			editConfigurationButton.setEnabled(false);

		}

		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	}
	
	private void handleNewConfigurationSelection() {
		
		ExecutionConfiguration selectedConfig = (ExecutionConfiguration)configurationsList.getSelectedValue();
		
		if(selectedConfig != null) {

			clearConfigurationButton.setEnabled(true);
			deleteTraceButton.setEnabled(false);
			editConfigurationButton.setEnabled(true);
			
			configurationBeingEdited = null;

			fieldsPanel.configureFieldsWith(selectedConfig);
			
			configurationBeingEdited = selectedConfig;
			
			savedTraces.clearSelection();
			
			fieldsPanel.setVisible(true);

			// Go back to wrapping lines, in case we're clearing program output.
			selectionDetails.setLineWrap(true);

			validateSelectedConfiguration();
			
			whylineIt.setText("<html>Record <b>" + selectedConfig.getName() + "</b>");
			
		}
		else {
			
			fieldsPanel.setVisible(false);
			selectionDetails.setForeground(UI.getControlTextColor());
			editConfigurationButton.setEnabled(false);
	
		}

	}
	
	private void loadConfigurations() {
		
		configurations.clear();
		
		WHYLINE_CONFIG_FILE = new File(Whyline.getHome(), Whyline.EXECUTIONS_FILE_NAME);
		try {
			if(!WHYLINE_CONFIG_FILE.exists()) {
				WHYLINE_CONFIG_FILE.createNewFile();
			}
			else {
	            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(WHYLINE_CONFIG_FILE)));
	            String line = null;
	            
	            StringBuilder builder = new StringBuilder();
	            while ( (line = br.readLine()) != null)
	            	builder.append(line + "\n");

	            try {
		            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
					Document doc = docBuilder.parse (WHYLINE_CONFIG_FILE);
					doc.getDocumentElement ().normalize ();
					
					NodeList configs = doc.getElementsByTagName("config");

					for(int i = 0; i < configs.getLength(); i++) {

						Node config = configs.item(i);
						configurations.add(new ExecutionConfiguration(config));
						
					}
										
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				}
	            	            
			}
        } catch (IOException ioe) {
        	ioe.printStackTrace();  
        }
		        
	}
	
	public void saveConfigurations() {
		
		// Delete the configs file and rewrite it with the updated values.
		WHYLINE_CONFIG_FILE.delete();
		try {
    		FileWriter writer = new FileWriter(WHYLINE_CONFIG_FILE);
    		writer.write("<configurations>\n");
			for(ExecutionConfiguration config : configurations) 
				config.write(writer);
    		writer.write("</configurations>");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void updateSavedTraceList() {
		
		if(Whyline.getSavedTracesFolder().exists()) {
		
			File[] traceDirectories = Whyline.getSavedTracesFolder().listFiles();
			Vector<String> traceNames = new Vector<String>();
			for(File file : traceDirectories) if(file.isDirectory()) traceNames.add(file.getName());
			savedTraces.setListData(traceNames);

		}
		else savedTraces.setListData(new String[0]);
		
	}
		
	private void analyzeTrace(File traceDirectory, WhylineUI.Mode mode) {
		
		try {
			
			openWindows.add(new WhylineUI(this, traceDirectory, mode));
			setVisible(false);
			
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
		
	}

	public void cancelCurrentExecution() {
		
		if(currentExecution != null)
			currentExecution.kill();
		
	}
	
	public void close(WhylineUI whylineUI) {
		
		// Garbage collect ho!
		openWindows.remove(whylineUI);
		System.gc();
		setVisible(true);
		
		updateSavedTraceList();
		
	}
	
	private void handleTextChange() {

		validateSelectedConfiguration();
				
	}	
	
	private void validateSelectedConfiguration() {

		if(configurationBeingEdited == null) return;
		
		fieldsAreValid = startMemory.isValid() && maxMemory.isValid() && projectPath.isValid() && sourcePaths.isValid() && mainClass.isValid();
		enableAnalysisButton(fieldsAreValid);
		
		if(fieldsAreValid) {
			selectionDetails.setText(configurationBeingEdited.getJavaCommand(getOptionsFromSelectedConfiguration(), true));
			selectionDetails.setForeground(UI.getControlTextColor());			
		}
		else {
			selectionDetails.setText("Error in configuration. Press the \"" + editConfigurationButton.getText() + "\" button to fix it.");
			selectionDetails.setForeground(UI.ERROR_COLOR);
		}

	}
	
	private void enableAnalysisButton(boolean enabled) {
		
		whylineIt.setEnabled(enabled);
		breakpointIt.setEnabled(enabled);
		sliceIt.setEnabled(enabled);
		
	}
	
	private boolean updatingItems = false;

	private ConfigurationsPanel configurationsPanel;

	private final WhylinePanel executionsAndDataPanel;
		
	private void showExecutionPanel(boolean show) {

		if(show) {
			mainPanel.remove(runPanel);
			mainPanel.add(cancelRunningButton, BorderLayout.SOUTH);
			executionsAndDataPanel.setEnabled(false);
			configurationsList.clearSelection();
			savedTraces.clearSelection();
			selectionDetails.setText("");
			selectionDetails.setRows(16);
		}
		else {
			mainPanel.remove(cancelRunningButton);
			mainPanel.add(runPanel, BorderLayout.SOUTH);
			executionsAndDataPanel.setEnabled(true);
			configurationsList.clearSelection();
			savedTraces.clearSelection();
			selectionDetails.setRows(8);
		}
		selectionDetails.invalidate();
		mainPanel.validate();
		repaint();

	}
	
	private void updateAndSaveConfigurations() {
		
		configurationsList.setListData(configurations);

		saveConfigurations();
		
		configurationsList.setSelectedValue(configurationBeingEdited, true);
		
	}

	private AgentOptions getOptionsFromSelectedConfiguration() {
		
		if(configurationBeingEdited.getProjectPath() == null) return null;
		if(configurationBeingEdited.getSourcePaths() == null) return null;
		
		AgentOptions options = new AgentOptions(configurationBeingEdited.getProjectPath(), configurationBeingEdited.getClassPaths(), configurationBeingEdited.getSourcePaths());
		
		// These were mainly for me, so I've excluded them since I don't need them right now.
//		options.setOption(Option.PRINT_METHODS_BEFORE_AND_AFTER, printBeforeAfter.isSelected());
//		options.setOption(Option.PRINT_INSTRUMENTATION_SUMMARY, printInstrumentationSummary.isSelected());
//		options.setOption(Option.PRINT_INSTRUMENTATION_EVENTS, printInstrumentationEvents.isSelected());
		options.setOption(Option.SKIP, configurationBeingEdited.getClassesToSkip());
		
		return options;
		
	}
	
	private class RecordAction extends AbstractAction {
		
		private final WhylineUI.Mode mode;
		
		public RecordAction(WhylineUI.Mode mode, Icon icon) {
			
			super(mode.getReadableName());
			
			this.mode = mode;
			
		}
		
		public void actionPerformed(ActionEvent e) {

			if(currentExecution != null) return;
			
			// Load a saved trace...
			if(savedTraces.getSelectedValue() != null) {

				analyzeTrace(new File(Whyline.getSavedTracesFolder(), "" + savedTraces.getSelectedValue()), mode);
				
			}
			// Record a program's execution...
			else if(configurationBeingEdited != null) {
	
				// Move this one to the front.
				configurations.remove(configurationBeingEdited);
				configurations.insertElementAt(configurationBeingEdited, 0);
				
	    		updateAndSaveConfigurations();

	    		AgentOptions options = getOptionsFromSelectedConfiguration();

				String command = configurationBeingEdited.getJavaCommand(options, true);
			
				Whyline.debugBreak(); 
				Whyline.debug("Tracing and analyzing with command");
				Whyline.debug("" + command);
				Whyline.debugBreak();

				showExecutionPanel(true);
				
				// While executing, don't wrap lines.
				selectionDetails.setLineWrap(false);
				
				currentExecution = new Runner(Whyline.WHYLINE_JAR_PATH, configurationBeingEdited, options, 
					new ProcessListener() {
						public void processDone(String message, int exitValue) {

							if(message != null) {
								JOptionPane.showMessageDialog(
										LauncherUI.this, 
										"I tried executing the program you specified, but it failed. I got this message:\n" +
										message, 	
										"Trouble executing your program...", 
										JOptionPane.ERROR_MESSAGE);
							}
							else analyzeTrace(Whyline.getWorkingTraceFolder(), mode);
	
							currentExecution = null;

							showExecutionPanel(false);

						}
						public void outputStream(String out) {
							
							selectionDetails.append(out + "\n");
							selectionDetails.scrollRectToVisible(new Rectangle(0, selectionDetails.getHeight(), getWidth(), selectionDetails.getHeight()));
							
						}
						public void errorStream(String err) {
							
							selectionDetails.append(err + "\n");
							selectionDetails.scrollRectToVisible(new Rectangle(0, selectionDetails.getHeight(), getWidth(), selectionDetails.getHeight()));
							
						}

					}
				);

				cancelRunningButton.setText("<html>Stop running <b>" + configurationBeingEdited.getName() + "</b>");
				
				currentExecution.execute();
				
				// Now that we've instrumented some things, enable the clear cache button.
				clearCache.setEnabled(true);
			
			}

		}		
		
	}

	private static abstract class InputField<T extends JComponent> extends WhylinePanel {

		private final LauncherUI launcher;

		protected final String description;
		protected WhylineLabel label;
		protected T value;
		private boolean isValid;

		public InputField(LauncherUI launcher, String description, T value) {
			
			super(new BorderLayout(UI.getPanelPadding(), UI.getPanelPadding()));

			this.launcher = launcher;

			this.description = description;
			this.value = value;

			label = new WhylineLabel(description);
			label.setVerticalAlignment(SwingConstants.CENTER);
			label.setPreferredSize(new Dimension(200, 0));
			
			add(label, BorderLayout.WEST);
			add(value, BorderLayout.CENTER);

		}
		
		public final void validate(boolean realChange) {
			
			String message = validate(value);
			if(message == null) {
				label.setText("<html>" + description);
				label.setForeground(UI.getControlTextColor());
				isValid = true;
			}
			else {
				label.setText("<html>" + message);
				label.setForeground(UI.ERROR_COLOR);
				isValid = false;
			}
//			value.setColumns(Math.max(8, value.getText().length()));
			
			revalidate();
			
			if(realChange) launcher.handleTextChange();
			
			if(launcher.fieldsPanel != null)
				launcher.fieldsPanel.updateSelectedConfiguration();
				
			label.repaint();
			
		}

		// Should return null if valid and a String if there's an error.
		public abstract String validate(T value);

		public JLabel getLabel() { return label; }
		public T getValue() { return value; }
		
		public boolean isValid() { return isValid; }

	}
		
	private static abstract class TextField extends InputField<WhylineTextField> {
			
		public TextField(LauncherUI launcher, String description, String defaultText) {
	
			super(launcher, description, new WhylineTextField(defaultText, 20, description));
			
			value.getDocument().addDocumentListener(new DocumentListener() {
					public void changedUpdate(DocumentEvent arg) { validate(true); }
					public void insertUpdate(DocumentEvent arg) { validate(true); }
					public void removeUpdate(DocumentEvent arg) { validate(true); }
				});
			value.setFont(UI.getMediumFont());
			value.setMaximumSize(new Dimension(Short.MAX_VALUE, (int)value.getMinimumSize().getHeight()));
		
			validate(false);
			
		}
			
		public void setText(String t) { value.setText(t); }
			
		public String getText() { return value.getText(); }
				
	}
		
	private class PreferencesPanel extends WhylinePanel {
		
		private final WhylineButton changeWhylineHome;
		private final WhylineLabel whylineHomeLabel;
		private final WhylineTextField sourcePath;

		public PreferencesPanel() {
			
			setLayout(new BorderLayout());
			
			whylineHomeLabel = new WhylineLabel(Whyline.getHome().getAbsolutePath());
			changeWhylineHome = new WhylineButton(new AbstractAction("change") {
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser(Whyline.getHome().getParent()) {
						public boolean accept(File f) {
							return f.isDirectory();	
						}
					};
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int result = chooser.showDialog(LauncherUI.this, "Set the home folder for whyline data");
					switch(result) {
						case JFileChooser.CANCEL_OPTION :
						case JFileChooser.ERROR_OPTION :
							break;
						
						case JFileChooser.APPROVE_OPTION :
							File newWhylineHome = chooser.getSelectedFile();
							Whyline.setHome(newWhylineHome);
							updateSavedTraceList();
							loadConfigurations();
					        updateAndSaveConfigurations();
							whylineHomeLabel.setText(Whyline.getHome().getAbsolutePath());
					}
				}
			}, "Change the location of the whyline cache and saved traces");
			
			WhylinePanel whylineHomePanel = new WhylinePanel(new FlowLayout(FlowLayout.LEFT, UI.getPanelPadding(), UI.getPanelPadding()));
			whylineHomePanel.add(new WhylineTitleLabel("Whyline folder"));
			whylineHomePanel.add(whylineHomeLabel);
			whylineHomePanel.add(changeWhylineHome);

			WhylinePanel pathPanel = new WhylinePanel(new FlowLayout(FlowLayout.LEFT, UI.getPanelPadding(), UI.getPanelPadding()));
			pathPanel.add(new WhylineTitleLabel("Path to JDK source"));
			sourcePath = new WhylineTextField(Whyline.getJDKSourcePath(), 160, "");
			sourcePath.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent e) { validate(); }
				public void insertUpdate(DocumentEvent e) {validate(); }
				public void removeUpdate(DocumentEvent e) {validate(); }
				private void validate() {
					boolean exists = (new File(sourcePath.getText())).exists();
					sourcePath.setForeground(exists ? UI.getControlTextColor() : UI.ERROR_COLOR);
					Whyline.setJDKSourcePath(sourcePath.getText());
				}
			});
			pathPanel.add(sourcePath);

			WhylinePanel fields = new WhylinePanel();
			fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
			
			add(fields, BorderLayout.NORTH);
			
			fields.add(whylineHomePanel);
			fields.add(pathPanel);						
			add(new WhylineButton(new AbstractAction("All done!") {
				public void actionPerformed(ActionEvent e) {
					showMainPanel();
				}
			}, "save the configuration changes"), BorderLayout.SOUTH);
			
		}
		
	}
	
	private class EditConfigurationPanel extends WhylinePanel {
		
		private final InputField<WhylineScrollPane> classesToSkipField;
		private final WhylineTextArea classesToSkip;

		public EditConfigurationPanel() {
			
			super(new BorderLayout(0, UI.getPanelPadding()));
			
			WhylinePanel fields = new WhylinePanel(new GridLayout(11, 1, UI.getPanelPadding(), UI.getPanelPadding()));
			
			WhylinePanel memory = new WhylinePanel(new GridLayout(1, 2, UI.getPanelPadding(), UI.getPanelPadding()));
			memory.add(startMemory);
			memory.add(maxMemory);

			classesToSkip = new WhylineTextArea("", 3, 0);
			classesToSkip.setBorder(null);
			WhylineScrollPane skipPane = new WhylineScrollPane(classesToSkip);
			skipPane.setBorder(new WhylineControlBorder());
			
			classesToSkipField  = new InputField<WhylineScrollPane>(LauncherUI.this, "<html>Don't record classes starting with the following prefixes (<i>one per line</i>)", skipPane) {
				public String validate(WhylineScrollPane pane) {
					String list = classesToSkip.getText();
					if(!list.equals("") && !list.matches("[a-zA-Z\\$0-9\\[\\]\\.\\n]+"))
						return "Must be a list of package or class (e.g., \"java.lang.Object\") names separated by carriage returns.";
					else
						return null;
				}
			};
			
			fields.add(new WhylineLabel("<html>Prepare for launch!<br><font size=\"-2\">(all relative paths are relative to the <b>project path</b> specified below)</font>", UI.getLargeFont()));
			fields.add(configurationName);
			fields.add(projectPath);
			fields.add(classPaths);
			fields.add(sourcePaths);
			fields.add(mainClass);
			fields.add(programArguments);
			fields.add(new WhylineLabel("Advanced options", UI.getLargeFont()));
			fields.add(memory);
			fields.add(classesToSkipField);

			add(fields, BorderLayout.CENTER);
			add(new WhylineButton(new AbstractAction("All done!") {
				public void actionPerformed(ActionEvent e) {
					showMainPanel();
				}
			}, "save the configuration changes"), BorderLayout.SOUTH);
			
			setOpaque(false);
			setBackground(null);
			
		}
		
		private void updateSelectedConfiguration() {
			
			if(configurationBeingEdited != null) {
			
				configurationBeingEdited.setName(configurationName.getText());
				configurationBeingEdited.setProjectPath(projectPath.getText());
				configurationBeingEdited.setClassPaths(classPaths.getText());
				configurationBeingEdited.setSourcePaths(sourcePaths.getText());
				configurationBeingEdited.setMainClass((String)mainClass.getValue().getSelectedItem());
				configurationBeingEdited.setArguments(programArguments.getText());
				configurationBeingEdited.setStartMemory(startMemory.getText());
				configurationBeingEdited.setMaxMemory(maxMemory.getText());
				configurationBeingEdited.setClassesToSkip(classesToSkip.getText());
			
				configurationsList.repaint();

				saveConfigurations();

			}

		}

		private void configureFieldsWith(ExecutionConfiguration config) {
			
			if(config != null) {
				configurationName.setText(config.getName());
				projectPath.setText(config.getProjectPath());
				classPaths.setText(config.getClassPaths());
				sourcePaths.setText(config.getSourcePaths());
				mainClass.getValue().setSelectedItem(config.getMainClass());
				programArguments.setText(config.getArguments());
				startMemory.setText(config.getStartMemory());
				maxMemory.setText(config.getMaxMemory());
				classesToSkip.setText(config.getClassesToSkip());
			}
			else {
				
				fieldsPanel.setVisible(false);
				
			}

		}

	}
	
	private class SavedTracesPanel extends WhylinePanel {
		
		public SavedTracesPanel() {
		
			super();
			
			setLayout(new BorderLayout(UI.getPanelPadding(), UI.getPanelPadding()));

			savedTraces = new WhylineList();
			savedTraces.setBorder(null);
			updateSavedTraceList();
			savedTraces.setFont(UI.getMediumFont());
			savedTraces.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					handleNewSavedTraceSelection();
				}
	 		});

			WhylineScrollPane scroller = new WhylineScrollPane(savedTraces);
			scroller.setBorder(new WhylineControlBorder());
			
			WhylinePanel savedTraceOperationsPanel = new WhylinePanel(new GridLayout(2, 2, UI.getPanelPadding(), UI.getPanelPadding()));
			savedTraceOperationsPanel.add(deleteTraceButton);
			savedTraceOperationsPanel.add(deleteAllTracesButton);
			savedTraceOperationsPanel.add(new WhylineLabel(""));
			savedTraceOperationsPanel.add(new WhylineLabel(""));

			add(new WhylineTitleLabel("<html>or choose a saved recording..."), BorderLayout.NORTH);
			add(savedTraceOperationsPanel, BorderLayout.SOUTH);
			add(scroller, BorderLayout.CENTER);
//			add(new WhylineTitleLabel("<html><font size=\"-2\">Traces saved at " + Whyline.SAVED_TRACES_FOLDER.getAbsolutePath() + "</font>"), BorderLayout.SOUTH);

		}
		
	}
	
	private class RunPanel extends WhylinePanel {
		
		public RunPanel() {
			
			super(new BorderLayout(UI.getPanelPadding(), UI.getPanelPadding()));
			
			WhylinePanel runButtons = new WhylinePanel(new GridLayout(1, 3, UI.getRoundedness(), UI.getRoundedness()));
			
//			runButtons.add(breakpointIt);
//			runButtons.add(sliceIt);
			runButtons.add(whylineIt);
			
			add(runButtons, BorderLayout.CENTER);
			
		}
	}
		
	private class ConfigurationsPanel extends WhylinePanel {
		
		public ConfigurationsPanel() {
			
			super();
			
			setLayout(new BorderLayout(UI.getPanelPadding(), UI.getPanelPadding()));
	
			configurationsList = new WhylineList();
			configurationsList.setBorder(null);
			configurationsList.setListData(configurations);
			configurationsList.setFont(UI.getMediumFont());
			configurationsList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					handleNewConfigurationSelection();
				}
	  		});
			configurationsList.setCellRenderer(new DefaultListCellRenderer() {
			    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			    	Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			    	if(!((ExecutionConfiguration)value).isValid())
			    		c.setForeground(UI.ERROR_COLOR);
			    	return c;
			    }
			});
			
			WhylineScrollPane scroller = new WhylineScrollPane(configurationsList);
			scroller.setBorder(new WhylineControlBorder());
						
			WhylinePanel recentExecutionsOperationsPanel = new WhylinePanel(new GridLayout(2, 2, UI.getPanelPadding(), UI.getPanelPadding()));
	
			recentExecutionsOperationsPanel.add(createConfigurationButton);
			recentExecutionsOperationsPanel.add(editConfigurationButton);
			recentExecutionsOperationsPanel.add(clearConfigurationButton);
			recentExecutionsOperationsPanel.add(clearCache);
	
			add(new WhylineTitleLabel("Choose or create a launch config..."), BorderLayout.NORTH);
			add(scroller, BorderLayout.CENTER);
			add(recentExecutionsOperationsPanel, BorderLayout.SOUTH);
	
		}
		
	}

	public void createConfig() {

		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Choose the folder that contains your program.");
		
		int returnVal = chooser.showOpenDialog(this);
	    if(returnVal == JFileChooser.APPROVE_OPTION)
	    	this.addNewConfiguration(chooser.getSelectedFile());

		showConfigurationPanel();

	}
	
}